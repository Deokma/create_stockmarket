package by.deokma.numismaticsstats.neoforge.shop;

import by.deokma.numismaticsstats.shop.ShopEntry;
import dev.ithundxr.createnumismatics.content.backend.behaviours.SliderStylePriceBehaviour;
import dev.ithundxr.createnumismatics.content.vendor.VendorBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopScanner {

    private static final Logger LOGGER = LogManager.getLogger("numismaticsstats");
    private static final Map<UUID, String> nameCache = new ConcurrentHashMap<>();

    // Cache the reflected method
    private static Method getChunksMethod = null;

    private ShopScanner() {}

    public static List<ShopEntry> scan(MinecraftServer server) {
        List<ShopEntry> result = new ArrayList<>();
        int totalVendors = 0;

        for (ServerLevel level : server.getAllLevels()) {
            String dimId = level.dimension().location().toString();
            List<LevelChunk> chunks = getLoadedChunks(level);
            LOGGER.info("[ShopScanner] dim={} loaded chunks={}", dimId, chunks.size());

            for (LevelChunk chunk : chunks) {
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof VendorBlockEntity vendor)) continue;
                    totalVendors++;

                    UUID ownerUuid = readOwnerFromNbt(vendor, server);
                    LOGGER.info("[ShopScanner] Found vendor at {} owner={} sellingItem={}",
                            be.getBlockPos(),
                            ownerUuid,
                            vendor.getSellingItem());

                    if (ownerUuid == null) continue;

                    String ownerName = resolveName(server, ownerUuid);
                    ItemStack sellingItem = vendor.getSellingItem();
                    if (sellingItem == null || sellingItem.isEmpty()) continue;

                    SliderStylePriceBehaviour priceBehaviour =
                            vendor.getBehaviour(SliderStylePriceBehaviour.TYPE);
                    int totalSpurs = priceBehaviour != null ? priceBehaviour.getTotalPrice() : 0;

                    VendorBlockEntity.Mode mode = vendor.getMode();
                    String modeStr = mode != null ? mode.name() : "SELL";

                    result.add(new ShopEntry(
                            be.getBlockPos(),
                            dimId,
                            sellingItem.copy(),
                            totalSpurs,
                            net.minecraft.world.item.ItemStack.EMPTY,
                            ownerUuid,
                            ownerName,
                            modeStr,
                            "VENDOR"
                    ));
                }
            }
        }

        LOGGER.info("[ShopScanner] Total vendors found: {}, added to list: {}", totalVendors, result.size());
        return result;
    }

    private static List<LevelChunk> getLoadedChunks(ServerLevel level) {
        List<LevelChunk> chunks = new ArrayList<>();
        try {
            if (getChunksMethod == null) {
                getChunksMethod = net.minecraft.server.level.ChunkMap.class.getDeclaredMethod("getChunks");
                getChunksMethod.setAccessible(true);
            }
            Object result = getChunksMethod.invoke(level.getChunkSource().chunkMap);
            // NeoForge 21.1 returns Iterable<ChunkHolder> (Guava UnmodifiableIterable)
            for (Object obj : (Iterable<?>) result) {
                net.minecraft.server.level.ChunkHolder holder = (net.minecraft.server.level.ChunkHolder) obj;
                LevelChunk chunk = holder.getTickingChunk();
                if (chunk != null) chunks.add(chunk);
            }
        } catch (Exception e) {
            LOGGER.error("[ShopScanner] Failed to get loaded chunks via reflection: {}", e.getMessage());
        }
        return chunks;
    }

    private static UUID readOwnerFromNbt(VendorBlockEntity vendor, MinecraftServer server) {
        try {
            CompoundTag tag = vendor.saveWithoutMetadata(server.registryAccess());
            LOGGER.debug("[ShopScanner] NBT keys: {}", tag.getAllKeys());
            if (tag.hasUUID("Owner")) return tag.getUUID("Owner");
            // Some versions may use lowercase
            if (tag.hasUUID("owner")) return tag.getUUID("owner");
        } catch (Exception e) {
            LOGGER.error("[ShopScanner] Failed to read owner NBT: {}", e.getMessage());
        }
        return null;
    }

    private static String resolveName(MinecraftServer server, UUID uuid) {
        return nameCache.computeIfAbsent(uuid, id -> {
            var player = server.getPlayerList().getPlayer(id);
            if (player != null) return player.getName().getString();
            var cache = server.getProfileCache();
            if (cache != null) {
                var opt = cache.get(id);
                if (opt.isPresent() && opt.get().getName() != null)
                    return opt.get().getName();
            }
            return id.toString().substring(0, 8);
        });
    }
}
