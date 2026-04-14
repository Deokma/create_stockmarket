package by.deokma.numismaticsstats.neoforge.market;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.*;

public class PriceHistorySavedData extends SavedData {

    public static final String NAME = "numismaticsstats_price_history";
    public static final int MAX_SNAPSHOTS = 144;

    private final Map<String, List<Integer>> history = new HashMap<>();

    public static PriceHistorySavedData getOrCreate(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
                new SavedData.Factory<>(
                        PriceHistorySavedData::new,
                        (tag, registries) -> PriceHistorySavedData.load(tag, registries)
                ),
                NAME
        );
    }

    public void addSnapshot(String itemId, int avgPrice) {
        List<Integer> list = history.computeIfAbsent(itemId, k -> new ArrayList<>());
        list.add(avgPrice);
        while (list.size() > MAX_SNAPSHOTS) {
            list.remove(0);
        }
        setDirty();
    }

    public List<Integer> getHistory(String itemId, int count) {
        List<Integer> list = history.getOrDefault(itemId, List.of());
        int from = Math.max(0, list.size() - count);
        return List.copyOf(list.subList(from, list.size()));
    }

    public List<Integer> getHistory(String itemId) {
        return List.copyOf(history.getOrDefault(itemId, List.of()));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag items = new CompoundTag();
        for (Map.Entry<String, List<Integer>> entry : history.entrySet()) {
            ListTag listTag = new ListTag();
            for (int price : entry.getValue()) {
                listTag.add(IntTag.valueOf(price));
            }
            items.put(entry.getKey(), listTag);
        }
        tag.put("items", items);
        return tag;
    }

    public static PriceHistorySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PriceHistorySavedData data = new PriceHistorySavedData();
        CompoundTag items = tag.getCompound("items");
        for (String key : items.getAllKeys()) {
            ListTag listTag = items.getList(key, Tag.TAG_INT);
            List<Integer> prices = new ArrayList<>(listTag.size());
            for (int i = 0; i < listTag.size(); i++) {
                prices.add(((IntTag) listTag.get(i)).getAsInt());
            }
            data.history.put(key, prices);
        }
        return data;
    }
}
