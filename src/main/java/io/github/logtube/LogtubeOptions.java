package io.github.logtube;

import io.github.logtube.utils.Strings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.*;

public class LogtubeOptions {

    public static @NotNull String getHostname() {
        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
        }
        if (hostname == null) {
            hostname = "localhost";
        }
        return hostname;
    }

    private static @NotNull Set<String> quickStringSet(@NotNull String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    public static @NotNull LogtubeOptions fromClasspath() {
        // load logtube.properties
        Properties properties = new Properties();
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("logtube.properties")) {
            if (stream != null) {
                properties.load(stream);
            } else {
                System.err.println("logtube.properties not found in class path, using default options");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("failed to load logtube.properties, using default options.");
        }
        // load custom file
        String filename = Strings.evaluateEnvironmentVariables(properties.getProperty("logtube.config-file"));
        if (filename != null) {
            properties = new Properties();
            try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename)) {
                if (stream != null) {
                    properties.load(stream);
                } else {
                    System.err.println(filename + " not found in class path, using default options");
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
                System.err.println("failed to load " + filename + ", using default options.");
            }
        }
        return new LogtubeOptions(properties);
    }

    @NotNull
    private final Properties properties;

    public LogtubeOptions(@NotNull Properties properties) {
        this.properties = properties;
    }

    @Nullable
    private String getProperty(@NotNull String field) {
        return Strings.evaluateEnvironmentVariables(this.properties.getProperty(field));
    }

    @Nullable
    @Contract("_, !null -> !null")
    private String safeStringValue(@NotNull String field, @Nullable String defaultValue) {
        return Strings.sanitize(getProperty(field), defaultValue);
    }

    @Nullable
    @Contract("_, !null -> !null")
    private String stringValue(@NotNull String field, @Nullable String defaultValue) {
        String ret = getProperty(field);
        return ret == null ? defaultValue : ret;
    }

    private boolean booleanValue(String field, boolean defaultValue) {
        String value = getProperty(field);
        if (value == null) {
            return defaultValue;
        }
        value = value.trim();
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("on") || value.equalsIgnoreCase("yes")) {
            return true;
        }
        if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("off") || value.equalsIgnoreCase("no")) {
            return false;
        }
        return defaultValue;
    }

    @Contract("_, !null -> !null")
    private @Nullable Set<String> setValue(String field, @Nullable Set<String> defaultValue) {
        String value = getProperty(field);
        if (value == null) {
            return defaultValue;
        }
        String[] components = value.split(",");
        if (components.length == 0) {
            return defaultValue;
        }
        HashSet<String> result = new HashSet<>();
        for (String component : components) {
            component = component.trim();
            if (component.length() == 0) {
                continue;
            }
            result.add(component);
        }
        if (result.isEmpty()) {
            return defaultValue;
        }
        return result;
    }

    @Contract("_, !null -> !null")
    private @Nullable Map<String, String> mapValue(String field, @Nullable Map<String, String> defaultValue) {
        String value = getProperty(field);
        if (value == null) {
            return defaultValue;
        }
        String[] components = value.split(",");
        if (components.length == 0) {
            return defaultValue;
        }
        HashMap<String, String> result = new HashMap<>();
        for (String component : components) {
            component = component.trim();
            if (component.length() == 0) {
                continue;
            }
            String[] kvs = component.split("=");
            if (kvs.length != 2) {
                continue;
            }
            String k = Strings.normalize(kvs[0]);
            String v = Strings.normalize(kvs[1]);
            if (k == null || v == null) {
                continue;
            }
            result.put(k, v);
        }
        if (result.isEmpty()) {
            return defaultValue;
        }
        return result;
    }

    @NotNull
    public String getProject() {
        return safeStringValue("logtube.project", "unknown-project");
    }

    @NotNull
    public String getEnv() {
        return safeStringValue("logtube.env", "unknown-env");
    }

    @NotNull
    public Set<String> getTopics() {
        return setValue("logtube.topics", quickStringSet("*", "-trace", "-debug"));
    }

    @NotNull
    public Map<String, String> getTopicMappings() {
        return mapValue("logtube.topic-mappings", new HashMap<>());
    }

    public boolean getConsoleEnabled() {
        return booleanValue("logtube.console.enabled", true);
    }

    @Nullable
    public Set<String> getConsoleTopics() {
        return setValue("logtube.console.topics", null);
    }

    public boolean getFilePlainEnabled() {
        return booleanValue("logtube.file-plain.enabled", true);
    }

    @Nullable
    public Set<String> getFilePlainTopics() {
        return setValue("logtube.file-plain.topics", quickStringSet("trace", "debug", "info", "warn", "error"));
    }

    @NotNull
    public String getFilePlainDir() {
        return stringValue("logtube.file-plain.dir", "logs");
    }

    @NotNull
    public String getFilePlainSignal() {
        return stringValue("logtube.file-plain.signal", "/tmp/logtube.reopen.txt");
    }

    public boolean getFileJSONEnabled() {
        return booleanValue("logtube.file-json.enabled", false);
    }

    @Nullable
    public Set<String> getFileJSONTopics() {
        return setValue("logtube.file-json.topics", quickStringSet("*", "-trace", "-debug", "-info", "-warn", "-error"));
    }

    @NotNull
    public String getFileJSONDir() {
        return stringValue("logtube.file-json.dir", "logs");
    }

    @NotNull
    public String getFileJSONSignal() {
        return stringValue("logtube.file-json.signal", "/tmp/logtube.reopen.txt");
    }

    public boolean getRemoteEnabled() {
        return booleanValue("logtube.remote.enabled", false);
    }

    @Nullable
    public Set<String> getRemoteTopics() {
        return setValue("logtube.remote.topics", quickStringSet("*", "-trace", "-debug"));
    }

    @NotNull
    public String[] getRemoteHosts() {
        Set<String> set = setValue("logtube.remote.hosts", null);
        if (set == null) {
            return new String[]{"127.0.0.1:9921"};
        }
        return (String[]) set.toArray();
    }

    public boolean getRedisEnabled() {
        return booleanValue("logtube.redis.enabled", false);
    }

    @NotNull
    public Set<String> getRedisTopics() {
        return setValue("logtube.redis.topics", quickStringSet("*", "-trace", "-debug"));
    }


    @NotNull
    public String[] getRedisHosts() {
        Set<String> set = setValue("logtube.redis.hosts", null);
        if (set == null) {
            return new String[]{"127.0.0.1:6379"};
        }
        return (String[]) set.toArray();
    }

    @NotNull
    public String getRedisKey() {
        return stringValue("logtube.redis.key", "logtube");
    }

}
