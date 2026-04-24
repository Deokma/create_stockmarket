package by.deokma.numismaticsstats.neoforge.market;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Persists trade statistics: how many times each shop owner's shops were purchased from.
 * Stored as "numismaticsstats_trade_stats.dat" in the overworld data directory.
 */
public class TradeStatsSavedData extends SavedData {

    public static final String NAME = "numismaticsstats_trade_stats";
    private static final Logger LOGGER = LogManager.getLogger("numismaticsstats");

    /** owner name → total sales count */
    private final Map<String, Long> salesByOwner = new LinkedHashMap<>();

    // ── Factory ───────────────────────────────────────────────────────────────

    public static TradeStatsSavedData getOrCreate(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
                new SavedData.Factory<>(
                        TradeStatsSavedData::new,
                        (tag, reg) -> TradeStatsSavedData.load(tag, reg)
                ),
                NAME
        );
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Record a sale from the given owner's shop. */
    public void recordSale(String ownerName, int quantity) {
        salesByOwner.merge(ownerName, (long) quantity, Long::sum);
        setDirty();
    }

    /** Returns top N owners sorted by sales count descending. */
    public List<Map.Entry<String, Long>> getTopSellers(int limit) {
        return salesByOwner.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    /** Returns all sales data as an unmodifiable map. */
    public Map<String, Long> getAllSales() {
        return Collections.unmodifiableMap(salesByOwner);
    }

    public long getSales(String ownerName) {
        return salesByOwner.getOrDefault(ownerName, 0L);
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag sales = new CompoundTag();
        for (Map.Entry<String, Long> e : salesByOwner.entrySet()) {
            sales.putLong(e.getKey(), e.getValue());
        }
        tag.put("sales", sales);
        return tag;
    }

    public static TradeStatsSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TradeStatsSavedData data = new TradeStatsSavedData();
        CompoundTag sales = tag.getCompound("sales");
        for (String key : sales.getAllKeys()) {
            data.salesByOwner.put(key, sales.getLong(key));
        }
        LOGGER.info("[TradeStatsSavedData] Loaded sales stats for {} owners", data.salesByOwner.size());
        return data;
    }
}
