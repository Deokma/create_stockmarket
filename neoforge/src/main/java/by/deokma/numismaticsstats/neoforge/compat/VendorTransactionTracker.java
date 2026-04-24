package by.deokma.numismaticsstats.neoforge.compat;

import by.deokma.numismaticsstats.neoforge.market.TradeStatsSavedData;
import dev.ithundxr.createnumismatics.content.vendor.VendorBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks Numismatics Vendor transactions by monitoring NBT changes.
 *
 * Strategy: on each block update event for a VendorBlockEntity,
 * compare the current selling item count with the previously recorded count.
 * A decrease in stock (for SELL mode) means a sale occurred.
 *
 * This class must ONLY be loaded when {@link NumismaticsCompat#isPresent()} is true.
 */
public final class VendorTransactionTracker {

    private static final Logger LOGGER = LogManager.getLogger("numismaticsstats");

    /**
     * Tracks last known selling item count per block key.
     * Key: "dim|x,y,z"
     */
    private static final Map<String, Integer> lastKnownCount = new ConcurrentHashMap<>();

    private VendorTransactionTracker() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(VendorTransactionTracker::onBlockUpdate);
    }

    private static void onBlockUpdate(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var be = level.getBlockEntity(event.getPos());
        if (!(be instanceof VendorBlockEntity vendor)) return;
        checkTransaction(level, vendor);
    }

    /**
     * Called when a VendorBlockEntity is ticked or updated.
     * Compares current stock with last known stock to detect sales.
     */
    public static void checkTransaction(ServerLevel level, BlockEntity be) {
        if (!(be instanceof VendorBlockEntity vendor)) return;
        try {
            MinecraftServer server = level.getServer();
            if (server == null) return;

            // Only track SELL mode vendors
            VendorBlockEntity.Mode mode = vendor.getMode();
            if (mode == null || mode != VendorBlockEntity.Mode.SELL) return;

            var sellingItem = vendor.getSellingItem();
            if (sellingItem == null || sellingItem.isEmpty()) return;

            String key = level.dimension().location() + "|"
                    + be.getBlockPos().getX() + ","
                    + be.getBlockPos().getY() + ","
                    + be.getBlockPos().getZ();

            // Read current stock from NBT
            int currentCount = readStockCount(vendor, server);
            if (currentCount < 0) return;

            Integer lastCount = lastKnownCount.get(key);
            if (lastCount != null && currentCount < lastCount) {
                // Stock decreased — a sale happened
                int sold = lastCount - currentCount;
                LOGGER.debug("[VendorTracker] Sale detected at {} sold={}", be.getBlockPos(), sold);

                // Record the sale
                UUID ownerUuid = readOwnerUuid(vendor, server);
                if (ownerUuid != null) {
                    String ownerName = resolveOwnerName(server, ownerUuid);
                    TradeStatsSavedData stats = TradeStatsSavedData.getOrCreate(server);
                    stats.recordSale(ownerName, sold);
                }
            }

            lastKnownCount.put(key, currentCount);
        } catch (Exception e) {
            LOGGER.debug("[VendorTracker] checkTransaction failed: {}", e.getMessage());
        }
    }

    /** Clears the tracking cache (call on server stop). */
    public static void clear() {
        lastKnownCount.clear();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int readStockCount(VendorBlockEntity vendor, MinecraftServer server) {
        try {
            CompoundTag tag = vendor.saveWithoutMetadata(server.registryAccess());
            // Vendor stores its stock in the container inventory
            // Try common NBT paths
            if (tag.contains("Items")) {
                var items = tag.getList("Items", 10);
                if (!items.isEmpty()) {
                    var first = items.getCompound(0);
                    return first.contains("count") ? first.getInt("count")
                            : first.contains("Count") ? first.getByte("Count") : 1;
                }
            }
            // Fallback: use the selling item count directly
            var sellingItem = vendor.getSellingItem();
            return sellingItem != null ? sellingItem.getCount() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private static UUID readOwnerUuid(VendorBlockEntity vendor, MinecraftServer server) {
        try {
            CompoundTag tag = vendor.saveWithoutMetadata(server.registryAccess());
            if (tag.hasUUID("Owner")) return tag.getUUID("Owner");
            if (tag.hasUUID("owner")) return tag.getUUID("owner");
        } catch (Exception ignored) {}
        return null;
    }

    private static String resolveOwnerName(MinecraftServer server, UUID uuid) {
        var player = server.getPlayerList().getPlayer(uuid);
        if (player != null) return player.getName().getString();
        var cache = server.getProfileCache();
        if (cache != null) {
            var opt = cache.get(uuid);
            if (opt.isPresent() && opt.get().getName() != null) return opt.get().getName();
        }
        return uuid.toString().substring(0, 8);
    }
}
