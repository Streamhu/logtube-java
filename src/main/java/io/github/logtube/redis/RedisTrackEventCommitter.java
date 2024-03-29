package io.github.logtube.redis;

import io.github.logtube.Logtube;
import io.github.logtube.core.IMutableEvent;
import io.github.logtube.utils.Flatten;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Protocol.Command;

import java.util.StringJoiner;

public class RedisTrackEventCommitter {

    private IMutableEvent event = Logtube.getLogger(RedisTrackEventCommitter.class).topic("x-redis-track");

    private long startTime = System.currentTimeMillis();

    public void commit(Object res) {
        event.extra("duration", System.currentTimeMillis() - this.startTime);
        event.extra("result_size", Flatten.objectToByteArray(res).length);
        this.event.commit();
    }

    public void setCmdAndArgs(@NotNull Command cmd, byte[][] args) {
        event.extra("cmd", cmd.name());

        StringJoiner paramValueJoiner = new StringJoiner(",");
        long paramValueSize = 0;

        // args的第1个为key。当args长度大于零时，才需要设置key及param等参数
        if (args != null && args.length > 0) {
            event.extra("key", new String(args[0]));

            // 从参数的size相同及记录
            for (int i = 1; i < args.length; i++) {
                paramValueSize += args[i].length;
                paramValueJoiner.add(new String(args[i]));
            }

            // 仅当参数存在时设置paramValue、paramValueSize
            if (paramValueSize > 0) {
                event.extra("param_value_size", paramValueSize);

                // set命令不记录参数值内容
                if (!cmd.equals(Command.SET)) {
                    event.extra("param_value", paramValueJoiner.toString());
                }
            }

        }

    }
}
