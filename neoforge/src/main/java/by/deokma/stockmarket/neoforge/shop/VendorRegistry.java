package by.deokma.stockmarket.neoforge.shop;

import by.deokma.stockmarket.neoforge.compat.NumismaticsCompat;
import by.deokma.stockmarket.neoforge.compat.TradeworksCompat;
import by.deokma.stockmarket.neoforge.compat.VendorIndexer;
import by.deokma.stockmarket.shop.ShopEntry;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
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
 * - Tradeworks shelves / inverted cloths: same as TableCloth BE, tagged TRADEWORKS when block namespace is {@code tradeworks}.
 */
public final class VendorRegistry {

    private static final Logger LOGGER = LogManager.getLogger("stockmarket");

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

        // Spawn chunks (and any other always-loaded chunks) fire ChunkEvent.Load
        // before ServerStartedEvent, so savedData is null at that point and they
        // are silently skipped. Re-scan every loaded chunk now that savedData is ready.
        refreshLoaded(server);
        LOGGER.info("[VendorRegistry] Post-start refresh: {} shops indexed", savedData.getAll().size());
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
        savedData.removeByBaseKey(makeKey(level, pos));
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
        pruneStaleEntries(server);
    }

    /**
     * Removes persisted entries whose chunk is currently loaded but the block
     * entity at that position is no longer a valid shop (broken block, removed mod, etc.).
     * Entries in unloaded chunks are left alone — they will be verified once that chunk loads.
     */
    private static void pruneStaleEntries(MinecraftServer server) {
        if (savedData == null) return;

        Set<String> toRemove = new HashSet<>();

        for (ShopEntry entry : savedData.getAll()) {
            ResourceLocation dimId = ResourceLocation.tryParse(entry.dimensionId());
            if (dimId == null) {
                toRemove.add(entryBaseKey(entry));
                continue;
            }

            ServerLevel level = null;
            for (ServerLevel l : server.getAllLevels()) {
                if (l.dimension().location().equals(dimId)) { level = l; break; }
            }
            if (level == null) continue; // dimension not loaded — can't verify

            if (!level.isLoaded(entry.pos())) continue; // chunk not loaded — skip

            // Chunk is loaded: block must exist and be a recognised shop entity
            if (!isValidShopEntity(level.getBlockEntity(entry.pos()))) {
                toRemove.add(entryBaseKey(entry));
            }
        }

        for (String baseKey : toRemove) {
            savedData.removeByBaseKey(baseKey);
            LOGGER.info("[VendorRegistry] Pruned stale shop entry: {}", baseKey);
        }
    }

    private static boolean isValidShopEntity(BlockEntity be) {
        if (be == null) return false;
        if (be instanceof TableClothBlockEntity) return true;
        return NumismaticsCompat.isPresent() && VendorIndexer.isVendorEntity(be);
    }

    private static String entryBaseKey(ShopEntry entry) {
        BlockPos p = entry.pos();
        return entry.dimensionId() + "|" + p.getX() + "," + p.getY() + "," + p.getZ();
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

            net.minecraft.nbt.ListTag entryList;
            try {
                var requestData   = tag.getCompound("RequestData");
                var encodedReq    = requestData.getCompound("encoded_request");
                var orderedStacks = encodedReq.getCompound("ordered_stacks");
                entryList = orderedStacks.getList("entries", 10);
            } catch (Exception ignored) {
                return;
            }
            if (entryList.isEmpty()) return;

            ItemStack paymentTemplate = parseTableClothPaymentItem(tag, server);

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

            String baseKey = makeKey(level, cloth.getBlockPos());
            savedData.removeByBaseKey(baseKey);

            String shopType = TradeworksCompat.isPresent()
                    && TradeworksCompat.isTradeworksBlock(cloth.getBlockState().getBlock())
                    ? "TRADEWORKS"
                    : "TABLECLOTH";

            int indexedRows = 0;
            for (int i = 0; i < entryList.size(); i++) {
                var offer = entryList.getCompound(i);
                var itemStackTag = offer.getCompound("item_stack");
                ItemStack sellingItem = ItemStack.parseOptional(server.registryAccess(), itemStackTag);
                if (!sellingItem.isEmpty() && offer.contains("count")) {
                    sellingItem.setCount(offer.getInt("count"));
                }
                if (sellingItem.isEmpty()) continue;

                ItemStack paymentItem = paymentTemplate.isEmpty()
                        ? ItemStack.EMPTY
                        : paymentTemplate.copy();

                String rowKey = baseKey + "#" + i;
                savedData.put(rowKey, new ShopEntry(
                        cloth.getBlockPos(),
                        level.dimension().location().toString(),
                        sellingItem,
                        0,
                        paymentItem,
                        ownerUuid,
                        ownerName,
                        "SELL",
                        shopType,
                        i
                ));
                indexedRows++;
            }
            if (indexedRows > 0) {
                LOGGER.debug("[VendorRegistry] TableCloth at {} indexed {} non-empty offer row(s)",
                        cloth.getBlockPos(), indexedRows);
            }
        } catch (Exception e) {
            LOGGER.debug("[VendorRegistry] indexTableCloth failed: {}", e.getMessage());
        }
    }

    /**
     * Payment item type comes from the Filter slot; required amount is stored separately on the BE.
     * Do not use the filter stack's serialized Count — it often mirrors a full slot (64) while the real price is 1.
     */
    private static ItemStack parseTableClothPaymentItem(CompoundTag tag, MinecraftServer server) {
        if (!tag.contains("Filter")) return ItemStack.EMPTY;
        CompoundTag filterTag = tag.getCompound("Filter");
        ItemStack paymentItem = ItemStack.parseOptional(server.registryAccess(), filterTag).copy();
        if (paymentItem.isEmpty()) return ItemStack.EMPTY;

        int amount = 1;
        if (tag.contains("FilterAmount")) {
            amount = Math.max(1, tag.getInt("FilterAmount"));
        } else if (tag.contains("filter_amount")) {
            amount = Math.max(1, tag.getInt("filter_amount"));
        }
        // Create's table cloth price UI is 1–100; it is not limited by the payment item's maxStackSize (often 64).
        paymentItem.setCount(Mth.clamp(amount, 1, 100));
        return paymentItem;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String makeKey(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
