package by.deokma.numismaticsstats.neoforge.shop;

import by.deokma.numismaticsstats.neoforge.compat.NumismaticsCompat;
import by.deokma.numismaticsstats.neoforge.compat.VendorIndexer;
import by.deokma.numismaticsstats.shop.ShopEntry;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side shop registry backed by {@link ShopSavedData}.
 *
 * All shops (from loaded chunks) are written to SavedData so they persist
 * across server restarts. When a player requests the shop list they receive
 * ALL known shops, not just those in currently loaded chunks.
 *
 * - Numismatics Vendor shops: indexed only when the mod is present.
 * - Create TableCloth shops: always indexed.
 */
public final class VendorRegistry {

    private static final Logger LOGGER = LogManager.getLogger("numismaticsstats");

    /** In-memory name cache — rebuilt from SavedData entries on load. */
    private static final Map<UUID, String> nameCache = new ConcurrentHashMap<>();

    /** Reference to the current world's SavedData. Set on server start, cleared on stop. */
    private static ShopSavedData savedData = null;

    private VendorRegistry() {}

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Call once when the server starts (or a world loads).
     * Loads persisted shops from disk into memory.
     */
    public static void onServerStart(MinecraftServer server) {
        savedData = ShopSavedData.getOrCreate(server);
        // Rebuild name cache from persisted entries
        for (ShopEntry e : savedData.getAll()) {
            nameCache.put(e.ownerUuid(), e.ownerName());
        }
        LOGGER.info("[VendorRegistry] Loaded {} shops from disk", savedData.getAll().size());
    }

    /** Call when the server stops / world unloads. */
    public static void onServerStop() {
        savedData = null;
        nameCache.clear();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns ALL known shops (loaded + persisted from previous sessions). */
    public static List<ShopEntry> getAll() {
        if (savedData == null) return List.of();
        return savedData.getAll();
    }

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            tryIndex(level, be);
        }
    }

    public static void onBlockPlace(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) tryIndex(level, be);
    }

    public static void onBlockBreak(ServerLevel level, BlockPos pos) {
        if (savedData == null) return;
        savedData.remove(makeKey(level, pos));
    }

    public static void refreshLoaded(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            try {
                var method = net.minecraft.server.level.ChunkMap.class.getDeclaredMethod("getChunks");
                method.setAccessible(true);
                Object result = method.invoke(level.getChunkSource().chunkMap);
                for (Object obj : (Iterable<?>) result) {
                    net.minecraft.server.level.ChunkHolder holder =
                            (net.minecraft.server.level.ChunkHolder) obj;
                    LevelChunk chunk = holder.getTickingChunk();
                    if (chunk == null) continue;
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        tryIndex(level, be);
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("[VendorRegistry] refreshLoaded: {}", e.getMessage());
            }
        }
    }

    /** Clears only the in-memory name cache; persisted shops are kept on disk. */
    public static void clear() {
        nameCache.clear();
    }

    // ── Indexing ──────────────────────────────────────────────────────────────

    private static void tryIndex(ServerLevel level, BlockEntity be) {
        if (savedData == null) return;

        // Numismatics Vendor — only if mod is present
        if (NumismaticsCompat.isPresent() && VendorIndexer.isVendorEntity(be)) {
            VendorIndexer.indexVendor(level, be, savedData, nameCache);
            return;
        }

        // Create TableCloth — always available
        if (be instanceof TableClothBlockEntity cloth) {
            indexTableCloth(level, cloth);
        }
    }

    private static void indexTableCloth(ServerLevel level, TableClothBlockEntity cloth) {
        try {
            MinecraftServer server = level.getServer();
            if (server == null) return;

            CompoundTag tag = cloth.saveWithoutMetadata(server.registryAccess());
            LOGGER.debug("[VendorRegistry] TableCloth NBT keys at {}: {}", cloth.getBlockPos(), tag.getAllKeys());

            if (!tag.hasUUID("OwnerUUID")) return;
            UUID ownerUuid = tag.getUUID("OwnerUUID");

            ItemStack sellingItem = ItemStack.EMPTY;
            try {
                var requestData   = tag.getCompound("RequestData");
                var encodedReq    = requestData.getCompound("encoded_request");
                var orderedStacks = encodedReq.getCompound("ordered_stacks");
                var entryList     = orderedStacks.getList("entries", 10);
                if (!entryList.isEmpty()) {
                    var first        = entryList.getCompound(0);
                    var itemStackTag = first.getCompound("item_stack");
                    sellingItem = ItemStack.parseOptional(server.registryAccess(), itemStackTag);
                    if (!sellingItem.isEmpty() && first.contains("count")) {
                        sellingItem.setCount(first.getInt("count"));
                    }
                }
            } catch (Exception ignored) {}
            if (sellingItem.isEmpty()) return;

            ItemStack paymentItem = ItemStack.EMPTY;
            if (tag.contains("Filter")) {
                CompoundTag filterTag = tag.getCompound("Filter");
                paymentItem = ItemStack.parseOptional(server.registryAccess(), filterTag);

                // Try to read the required count from FilterAmount or similar keys
                // TableCloth stores the required payment count separately
                if (!paymentItem.isEmpty()) {
                    int count = 1;
                    // Check common NBT keys for payment count
                    if (tag.contains("FilterAmount")) {
                        count = tag.getInt("FilterAmount");
                    } else if (filterTag.contains("count")) {
                        count = filterTag.getInt("count");
                    } else if (filterTag.contains("Count")) {
                        count = filterTag.getByte("Count");
                    }
                    if (count > 1) paymentItem.setCount(count);
                    LOGGER.debug("[VendorRegistry] TableCloth at {} paymentItem={} count={}",
                            cloth.getBlockPos(), paymentItem.getHoverName().getString(), paymentItem.getCount());
                }
            }

            String ownerName = nameCache.computeIfAbsent(ownerUuid, id -> {
                var player = server.getPlayerList().getPlayer(id);
                if (player != null) return player.getName().getString();
                var cache = server.getProfileCache();
                if (cache != null) {
                    var opt = cache.get(id);
                    if (opt.isPresent() && opt.get().getName() != null) return opt.get().getName();
                }
                return id.toString().substring(0, 8);
            });

            String key = makeKey(level, cloth.getBlockPos());
            savedData.put(key, new ShopEntry(
                    cloth.getBlockPos(),
                    level.dimension().location().toString(),
                    sellingItem,
                    0,
                    paymentItem,
                    ownerUuid,
                    ownerName,
                    "SELL",
                    "TABLECLOTH"
            ));
        } catch (Exception e) {
            LOGGER.debug("[VendorRegistry] indexTableCloth failed: {}", e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String makeKey(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
