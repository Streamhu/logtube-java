package io.github.logtube.core.outputs;

import io.github.logtube.core.IEvent;
import io.github.logtube.core.IEventSerializer;
import io.github.logtube.core.serializers.EventRedisSerializer;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class EventRedisOutput extends BaseEventOutput {

    public static final int QUEUE_CAPACITY = 1024;

    private final IEventSerializer serializer = new EventRedisSerializer();

    private final String[] hosts;

    private final String key;

    @NotNull
    private final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    private Thread worker = null;

    public EventRedisOutput(String[] hosts, String key) {
        this.hosts = hosts;
        this.key = key;
    }

    @Override
    public void doStart() {
        super.doStart();
        this.worker = new Thread(new EventRedisOutputWorker(this.hosts, this.key));
        this.worker.start();
    }

    @Override
    public void doStop() {
        this.worker.interrupt();
        try {
            this.worker.join();
        } catch (InterruptedException ignored) {
        }
        this.worker = null;
        super.doStop();
    }

    @Override
    public void doAppendEvent(@NotNull IEvent e) {
        this.queue.offer(serializer.toString(e));
    }

    private class EventRedisOutputWorker implements Runnable {

        private final ArrayList<JedisPool> pools = new ArrayList<>();

        private final String key;

        private final AtomicLong cursor = new AtomicLong();

        EventRedisOutputWorker(String[] hosts, String key) {
            for (String url : hosts) {
                this.pools.add(new JedisPool(url));
            }
            this.key = key;
        }

        @NotNull
        private Jedis getJedis(int retry) {
            // if retry is too small, returns null
            if (retry < 1) {
                throw new JedisConnectionException("failed to find a reachable redis instance");
            }
            // roll the round robin
            long index = this.cursor.addAndGet(1);
            if (index < 0) {
                index = 0;
            }
            // find a pool and get a jedis
            try {
                return this.pools.get((int) (index % (long) this.pools.size())).getResource();
            } catch (Exception ignored) {
            }
            return getJedis(retry - 1);
        }

        @NotNull
        private Jedis getJedis() {
            return getJedis(this.pools.size());
        }

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                // take a message
                String message = null;
                try {
                    message = EventRedisOutput.this.queue.take();
                } catch (Exception ignored) {
                    continue;
                }
                // push a message
                try (Jedis jedis = getJedis()) { // use try-with-resource
                    jedis.rpush(this.key, message);
                } catch (Exception ignored) {
                }
            }

            // clear the pool on shutdown
            this.pools.clear();
        }

    }

}
