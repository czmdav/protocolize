package dev.simplix.protocolize.api.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.querz.nbt.tag.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Resolves player names used as skull owners to complete game profiles (unique id and signed textures).
 * <p>
 * Minecraft 1.8 - 1.12 clients resolve a skull whose {@code SkullOwner} only contains a player name
 * synchronously on their render thread. This freezes the client for up to several seconds per head.
 * Protocolize therefore resolves those names on the proxy (off the network threads) and only sends
 * profiles to these clients which the client does not need to look up by itself.
 * <p>
 * Results are cached. Names which could not be resolved are cached for a shorter amount of time to
 * prevent hitting the Mojang API rate limits.
 */
@Slf4j(topic = "Protocolize")
public final class SkullProfileResolver {

    private static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z0-9_]{1,16}$");
    private static final long SUCCESS_TTL = TimeUnit.HOURS.toMillis(6);
    private static final long FAILURE_TTL = TimeUnit.MINUTES.toMillis(5);
    private static final int MAX_CACHE_SIZE = 10_000;
    private static final int TIMEOUT_MILLIS = 5_000;
    private static final String[] UUID_LOOKUP_URLS = {
        "https://api.minecraftservices.com/minecraft/profile/lookup/name/%s",
        "https://api.mojang.com/users/profiles/minecraft/%s"
    };
    private static final String PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";

    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "Protocolize Skull Resolver #" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    });

    private static volatile Function<String, Profile> localLookup = name -> null;

    private SkullProfileResolver() {
    }

    /**
     * Sets a lookup which is asked first before a name is resolved using the Mojang API. Platforms use this to
     * provide the profiles of players which are currently connected to the proxy.
     *
     * @param lookup A function returning the complete profile of the given name or null if unknown
     */
    public static void localLookup(Function<String, Profile> lookup) {
        localLookup = Objects.requireNonNull(lookup, "Lookup cannot be null");
    }

    /**
     * Returns the resolved profile of the given name without blocking. If the name has not been resolved yet,
     * the resolution is started in the background and null is returned.
     *
     * @param name The player name
     * @return The resolved profile or null if it is not available (yet)
     */
    public static Profile cached(String name) {
        CompletableFuture<Profile> future = resolve(name);
        return future.isDone() ? future.getNow(null) : null;
    }

    /**
     * Resolves the given name in the background.
     *
     * @param name The player name
     * @return A future which completes with the profile or with null if the name could not be resolved
     */
    public static CompletableFuture<Profile> resolve(String name) {
        if (name == null || !VALID_NAME.matcher(name).matches()) {
            return CompletableFuture.completedFuture(null);
        }
        String key = name.toLowerCase(Locale.ROOT);
        CacheEntry entry = CACHE.get(key);
        if (entry != null && !entry.expired()) {
            return entry.future;
        }
        Profile local = localProfile(name);
        if (local != null) {
            CacheEntry localEntry = new CacheEntry(CompletableFuture.completedFuture(local));
            CACHE.put(key, localEntry);
            return localEntry.future;
        }
        CompletableFuture<Profile> future = new CompletableFuture<>();
        CacheEntry newEntry = new CacheEntry(future);
        if (entry == null ? CACHE.putIfAbsent(key, newEntry) != null : !CACHE.replace(key, entry, newEntry)) {
            // Another thread started resolving this name in the meantime
            CacheEntry current = CACHE.get(key);
            return current != null ? current.future : CompletableFuture.completedFuture(null);
        }
        cleanupCache();
        EXECUTOR.execute(() -> {
            Profile profile = null;
            try {
                profile = lookup(name);
            } catch (Exception e) {
                log.debug("Unable to resolve skull owner {}", name, e);
            }
            future.complete(profile);
        });
        return future;
    }

    private static Profile localProfile(String name) {
        try {
            Profile profile = localLookup.apply(name);
            return profile != null && profile.textures() != null ? profile : null;
        } catch (Exception e) {
            log.debug("Local skull owner lookup failed for {}", name, e);
            return null;
        }
    }

    private static void cleanupCache() {
        if (CACHE.size() <= MAX_CACHE_SIZE) {
            return;
        }
        CACHE.values().removeIf(CacheEntry::expired);
        if (CACHE.size() > MAX_CACHE_SIZE) {
            CACHE.clear();
        }
    }

    private static Profile lookup(String name) throws IOException {
        UUID uniqueId = null;
        for (String url : UUID_LOOKUP_URLS) {
            JsonObject response = request(String.format(url, name));
            if (response != null && response.has("id")) {
                uniqueId = parseUndashedUniqueId(response.get("id").getAsString());
                break;
            }
        }
        if (uniqueId == null) {
            return null;
        }
        JsonObject profile = request(String.format(PROFILE_URL, uniqueId.toString().replace("-", "")));
        if (profile == null || !profile.has("properties")) {
            return null;
        }
        String profileName = profile.has("name") ? profile.get("name").getAsString() : name;
        JsonArray properties = profile.getAsJsonArray("properties");
        for (JsonElement element : properties) {
            JsonObject property = element.getAsJsonObject();
            if ("textures".equals(property.get("name").getAsString())) {
                String signature = property.has("signature") ? property.get("signature").getAsString() : null;
                return new Profile(uniqueId, profileName, property.get("value").getAsString(), signature);
            }
        }
        return null;
    }

    private static JsonObject request(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setRequestProperty("User-Agent", "Protocolize");
        connection.setRequestProperty("Accept", "application/json");
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status != HttpURLConnection.HTTP_NO_CONTENT && status != HttpURLConnection.HTTP_NOT_FOUND) {
                    log.debug("Skull owner lookup {} returned HTTP {}", url, status);
                }
                return null;
            }
            try (InputStream in = connection.getInputStream()) {
                JsonElement element = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8));
                return element.isJsonObject() ? element.getAsJsonObject() : null;
            }
        } finally {
            connection.disconnect();
        }
    }

    private static UUID parseUndashedUniqueId(String id) {
        if (id.length() != 32) {
            return UUID.fromString(id);
        }
        return new UUID(Long.parseUnsignedLong(id.substring(0, 16), 16), Long.parseUnsignedLong(id.substring(16), 16));
    }

    // NBT helpers for the "SkullOwner" tag used by item stacks before 1.20.5

    /**
     * Returns the player name stored in the {@code SkullOwner} tag of the given item NBT.
     *
     * @param itemNbt The item NBT
     * @return The owner name or null if there is none
     */
    public static String ownerName(CompoundTag itemNbt) {
        if (itemNbt == null) {
            return null;
        }
        Tag<?> owner = itemNbt.get("SkullOwner");
        String name = null;
        if (owner instanceof StringTag) {
            name = ((StringTag) owner).getValue();
        } else if (owner instanceof CompoundTag) {
            StringTag nameTag = ((CompoundTag) owner).getStringTag("Name");
            name = nameTag != null ? nameTag.getValue() : null;
        }
        return name == null || name.isEmpty() ? null : name;
    }

    /**
     * @param itemNbt The item NBT
     * @return true if the {@code SkullOwner} tag of the given item NBT names a player but does not contain textures
     */
    public static boolean needsResolving(CompoundTag itemNbt) {
        if (ownerName(itemNbt) == null) {
            return false;
        }
        Tag<?> owner = itemNbt.get("SkullOwner");
        return !(owner instanceof CompoundTag) || textures((CompoundTag) owner) == null;
    }

    /**
     * Creates the {@code SkullOwner} tag to send to legacy clients for an item whose owner {@link #needsResolving(CompoundTag) needs resolving}.
     * This never returns a plain name since such a name makes the client look it up on its render thread.
     *
     * @param itemNbt The item NBT
     * @return The complete owner compound if the name is resolved, otherwise a compound only containing the name
     */
    public static CompoundTag legacySkullOwner(CompoundTag itemNbt) {
        String name = ownerName(itemNbt);
        Profile profile = cached(name);
        CompoundTag owner = new CompoundTag();
        if (profile == null) {
            owner.putString("Name", name);
            return owner;
        }
        owner.putString("Id", profile.uniqueId().toString());
        owner.putString("Name", profile.name());
        CompoundTag texture = new CompoundTag();
        texture.putString("Value", profile.textures());
        if (profile.signature() != null) {
            texture.putString("Signature", profile.signature());
        }
        ListTag<CompoundTag> textures = new ListTag<>(CompoundTag.class);
        textures.add(texture);
        CompoundTag properties = new CompoundTag();
        properties.put("textures", textures);
        owner.put("Properties", properties);
        return owner;
    }

    /**
     * Reads the textures property of a {@code SkullOwner} compound.
     *
     * @param owner The {@code SkullOwner} compound
     * @return The textures property or null if there is none
     */
    public static Property textures(CompoundTag owner) {
        CompoundTag properties = owner.getCompoundTag("Properties");
        if (properties == null || !(properties.get("textures") instanceof ListTag)) {
            return null;
        }
        ListTag<?> textures = (ListTag<?>) properties.get("textures");
        if (textures.size() == 0 || !(textures.get(0) instanceof CompoundTag)) {
            return null;
        }
        CompoundTag texture = (CompoundTag) textures.get(0);
        StringTag value = texture.getStringTag("Value");
        if (value == null || value.getValue().isEmpty()) {
            return null;
        }
        StringTag signature = texture.getStringTag("Signature");
        return new Property("textures", value.getValue(), signature != null ? signature.getValue() : null);
    }

    /**
     * Reads the unique id of a {@code SkullOwner} compound. Supports both the string and the int array format.
     *
     * @param owner The {@code SkullOwner} compound
     * @return The unique id or null if there is none
     */
    public static UUID uniqueId(CompoundTag owner) {
        Tag<?> id = owner.get("Id");
        try {
            if (id instanceof StringTag) {
                return UUID.fromString(((StringTag) id).getValue());
            }
            if (id instanceof IntArrayTag && ((IntArrayTag) id).length() == 4) {
                int[] ints = ((IntArrayTag) id).getValue();
                return new UUID((long) ints[0] << 32 | ints[1] & 0xFFFFFFFFL, (long) ints[2] << 32 | ints[3] & 0xFFFFFFFFL);
            }
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    @Value
    @lombok.experimental.Accessors(fluent = true)
    public static class Profile {
        UUID uniqueId;
        String name;
        String textures;
        String signature;
    }

    private static final class CacheEntry {
        private final CompletableFuture<Profile> future;
        private final long created = System.currentTimeMillis();

        private CacheEntry(CompletableFuture<Profile> future) {
            this.future = future;
        }

        private boolean expired() {
            if (!future.isDone()) {
                return false;
            }
            long ttl = future.getNow(null) != null ? SUCCESS_TTL : FAILURE_TTL;
            return System.currentTimeMillis() - created > ttl;
        }
    }

}
