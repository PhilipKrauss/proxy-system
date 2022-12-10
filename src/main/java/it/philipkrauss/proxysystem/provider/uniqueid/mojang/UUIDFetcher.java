package it.philipkrauss.proxysystem.provider.uniqueid.mojang;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.philipkrauss.proxysystem.ProxySystem;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * UUIDFetcher on <a href="https://gist.github.com/Jofkos/efaaff2afb645898adc3">Github</a> and
 * UUIDTypeAdapter on <a href="https://github.com/Techcable/Authlib/blob/master/src/main/java/com/mojang/util/UUIDTypeAdapter.java">Github</a>
 *
 */

public class UUIDFetcher {

    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();

    private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/%s?at=%d";
    private static final String NAME_URL = "https://api.mojang.com/user/profiles/%s/names";

    private static final Map<String, UUID> UUID_CACHE = Maps.newHashMap();
    private static final Map<UUID, String> NAME_CACHE = Maps.newHashMap();

    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

    private String name;
    private UUID id;

    /**
     * Fetches the uuid synchronously and returns it
     *
     * @param name The name
     * @return The uuid
     */
    public static UUID getUUID(String name) {
        return getUUIDAt(name, System.currentTimeMillis());
    }

    public static UUID getUUIDAt(String name, long timestamp) {
        name = name.toLowerCase();
        if (UUID_CACHE.containsKey(name)) return UUID_CACHE.get(name);
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(String.format(UUID_URL, name, timestamp / 1000)).openConnection();
            connection.setReadTimeout(5000);
            UUIDFetcher data = GSON.fromJson(new BufferedReader(new InputStreamReader(connection.getInputStream())), UUIDFetcher.class);
            UUID_CACHE.put(name, data.id);
            NAME_CACHE.put(data.id, data.name);
            ProxySystem.getInstance().getLogger().info(String.format("[UUIDProvider] Fetched profile %s...", data.id));
            return data.id;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Fetches the name asynchronously and passes it to the consumer
     *
     * @param uuid   The uuid
     * @param action Do what you want to do with the name her
     */
    public static void getName(final UUID uuid, Consumer<String> action) {
        THREAD_POOL.execute(new Acceptor<>(action) {
            @Override
            public String getValue() {
                return getName(uuid);
            }
        });
    }

    /**
     * Fetches the name synchronously and returns it
     *
     * @param uuid The uuid
     * @return The name
     */
    public static String getName(UUID uuid) {
        if (NAME_CACHE.containsKey(uuid)) return NAME_CACHE.get(uuid);
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(String.format(NAME_URL, UUIDTypeAdapter.fromUUID(uuid))).openConnection();
            connection.setReadTimeout(5000);
            UUIDFetcher[] nameHistory = GSON.fromJson(new BufferedReader(new InputStreamReader(connection.getInputStream())), UUIDFetcher[].class);
            UUIDFetcher currentNameData = nameHistory[nameHistory.length - 1];
            UUID_CACHE.put(currentNameData.name.toLowerCase(), uuid);
            NAME_CACHE.put(uuid, currentNameData.name);
            ProxySystem.getInstance().getLogger().info(String.format("[UUIDProvider] Fetched profile %s...", currentNameData.id));
            return currentNameData.name;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public interface Consumer<T> {
        void accept(T t);
    }

    public static abstract class Acceptor<T> implements Runnable {

        private final Consumer<T> consumer;

        public Acceptor(Consumer<T> consumer) {
            this.consumer = consumer;
        }

        public abstract T getValue();

        @Override
        public void run() {
            consumer.accept(getValue());
        }

    }
}
