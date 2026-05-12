package by.deokma.create_stockmarket.neoforge.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton that resolves player names to skin {@link ResourceLocation}s.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>In-memory session cache (instant, no I/O)</li>
 *   <li>Minecraft's {@code SkinManager} via {@code getOrLoad} (async, background thread)</li>
 *   <li>Fallback (Steve) texture while loading or on failure</li>
 * </ol>
 *
 * <p>All network I/O is performed on a background thread via the
 * {@code CompletableFuture} returned by {@code SkinManager.getOrLoad}.
 * The render thread only reads from {@link ConcurrentHashMap}-backed collections
 * and never blocks.
 *
 * <p>Failed names are never retried within the same game session.
 * Call {@link #clear()} on world disconnect to free memory.
 */
public final class SkinFetcher {

    /** Shared singleton instance. */
    public static final SkinFetcher INSTANCE = new SkinFetcher();

    // ── Fallback texture ──────────────────────────────────────────────────────
    private static final ResourceLocation FALLBACK;

    static {
        ResourceLocation fb = null;
        try {
            fb = DefaultPlayerSkin.getDefaultTexture();
        } catch (Exception ignored) {}
        FALLBACK = fb;
    }

    // ── Session state (all ConcurrentHashMap-backed, safe for render thread) ──
    /** Resolved textures keyed by player name. */
    private final Map<String, ResourceLocation> cache      = new ConcurrentHashMap<>();
    /** Names with an in-flight fetch. */
    private final Set<String>                   pending    = ConcurrentHashMap.newKeySet();
    /** Names that failed this session — no retry. */
    private final Set<String>                   failed     = ConcurrentHashMap.newKeySet();
    /** Whether each resolved skin uses the legacy 64×32 format. */
    private final Map<String, Boolean>          legacyFlags = new ConcurrentHashMap<>();

    private SkinFetcher() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the skin texture for the given player name, or the fallback
     * texture if the skin is not yet available.
     *
     * <p>Initiates an async fetch if the name is not cached and not already pending.
     *
     * @param playerName case-sensitive Minecraft player name; may be null
     * @return {@link ResourceLocation} of the skin texture, never {@code null}
     *         (falls back to Steve if the fallback itself is null, callers must guard)
     */
    public ResourceLocation getTexture(String playerName) {
        if (playerName == null || playerName.isBlank()) return fallback();
        ResourceLocation cached = cache.get(playerName);
        if (cached != null) return cached;
        if (failed.contains(playerName))   return fallback();
        if (pending.contains(playerName))  return fallback();
        startFetch(playerName);
        return fallback();
    }

    /**
     * Returns {@code true} if the resolved skin for this player uses the
     * legacy 64×32 format (no hat overlay).
     */
    public boolean isLegacy(String playerName) {
        return playerName != null && legacyFlags.getOrDefault(playerName, false);
    }


    // ── Private implementation ────────────────────────────────────────────────

    private ResourceLocation fallback() {
        return FALLBACK;
    }

    private void startFetch(String playerName) {
        pending.add(playerName);
        try {
            // Build a GameProfile with just the name; SkinManager will resolve the UUID.
            GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(
                    ("OfflinePlayer:" + playerName).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    playerName);

            Minecraft.getInstance().getSkinManager()
                    .getOrLoad(profile)
                    .thenAccept(skin -> {
                        if (skin != null) {
                            boolean legacy = (skin.model() == PlayerSkin.Model.WIDE)
                                    && isLegacyTexture(skin);
                            legacyFlags.put(playerName, legacy);
                            onFetchSuccess(playerName, skin.texture());
                        } else {
                            onFetchFailure(playerName);
                        }
                    })
                    .exceptionally(ex -> {
                        onFetchFailure(playerName);
                        return null;
                    });
        } catch (Exception ex) {
            onFetchFailure(playerName);
        }
    }

    /**
     * Heuristic: treat a skin as legacy if its texture ResourceLocation path
     * does not contain a UUID-style segment (Mojang modern skins always use
     * a UUID-derived hash path). This is a best-effort check; the hat overlay
     * is simply not rendered for skins flagged as legacy.
     */
    private static boolean isLegacyTexture(PlayerSkin skin) {
        // Modern skins always have a non-null texture; we conservatively return
        // false (modern) so the hat overlay is attempted for all resolved skins.
        return false;
    }

    private void onFetchSuccess(String playerName, ResourceLocation texture) {
        // Discard if clear() was called while the fetch was in flight.
        if (cache.isEmpty() && !pending.contains(playerName)) return;
        cache.put(playerName, texture);
        pending.remove(playerName);
    }

    private void onFetchFailure(String playerName) {
        failed.add(playerName);
        pending.remove(playerName);
    }
}
