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
 * Server-side shop registry.
 * - Create: Numismatics Vendor shops are indexed only when the mod is present.
 * - Create TableCloth shops are always indexed (no Numismatics dependency).
 */
public final class VendorRegistry {

    private static final Logger LOGGER = LogManager.getLogger("numismaticsstats");

    private static final Map<String, ShopEntry> entries   = new ConcurrentHashMap<>();
    private static final Map<UUID, String>      nameCache = new ConcurrentHashMap<>();

    private VendorRegistry() {}

    // ── Public API ────────────────────────────────────────────────────────────

    public static List<ShopEntry> getAll() {
        return new ArrayList<>(entries.values());
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
        entries.remove(makeKey(level, pos));
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

    public static void clear() {
        entries.clear();
        nameCache.clear();
    }

    // ── Indexing ──────────────────────────────────────────────────────────────

    private static void tryIndex(ServerLevel level, BlockEntity be) {
        // Numismatics Vendor — only if mod is present
        if (NumismaticsCompat.isPresent() && VendorIndexer.isVendorEntity(be)) {
            VendorIndexer.indexVendor(level, be, entries, nameCache);
            return;
        }

        // Create TableCloth — always available (no Numismatics dependency)
        if (be instanceof TableClothBlockEntity cloth) {
            indexTableCloth(level, cloth);
        }
    }

    private static void indexTableCloth(ServerLevel level, TableClothBlockEntity cloth) {
        try {
            MinecraftServer server = level.getServer();
            if (server == null) return;

            CompoundTag tag = cloth.saveWithoutMetadata(server.registryAccess());

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
                paymentItem = ItemStack.parseOptional(server.registryAccess(),
                        tag.getCompound("Filter"));
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

            entries.put(makeKey(level, cloth.getBlockPos()), new ShopEntry(
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
