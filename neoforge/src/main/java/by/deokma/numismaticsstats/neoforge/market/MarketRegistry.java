package by.deokma.numismaticsstats.neoforge.market;

import by.deokma.numismaticsstats.market.IMarketRegistry;
import by.deokma.numismaticsstats.market.MarketEntry;
import by.deokma.numismaticsstats.market.PriceTrend;
import by.deokma.numismaticsstats.neoforge.shop.VendorRegistry;
import by.deokma.numismaticsstats.shop.ShopEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public final class MarketRegistry implements IMarketRegistry {

    private static final Logger LOGGER = LogManager.getLogger("numismaticsstats");

    @Override
    public List<MarketEntry> buildEntries() {
        return List.of();
    }

    public static List<MarketEntry> buildEntries(MinecraftServer server) {
        try {
            List<ShopEntry> shops = VendorRegistry.getAll();
            PriceHistorySavedData histData = PriceHistorySavedData.getOrCreate(server);

            // Group by item ResourceLocation (ignore NBT)
            Map<ResourceLocation, List<ShopEntry>> grouped = new LinkedHashMap<>();
            for (ShopEntry e : shops) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(e.sellingItem().getItem());
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
            }

            List<MarketEntry> result = new ArrayList<>();
            for (Map.Entry<ResourceLocation, List<ShopEntry>> group : grouped.entrySet()) {
                ResourceLocation itemId = group.getKey();
                List<ShopEntry> entries = group.getValue();

                // Prices only from VENDOR SELL shops
                List<Integer> vendorSellPrices = entries.stream()
                        .filter(e -> "VENDOR".equals(e.shopType()) && "SELL".equals(e.mode())
                                && e.totalPriceInSpurs() > 0)
                        .map(ShopEntry::totalPriceInSpurs)
                        .collect(Collectors.toList());

                int minPrice  = vendorSellPrices.isEmpty() ? 0
                        : vendorSellPrices.stream().mapToInt(i -> i).min().orElse(0);
                int avgPrice  = computeAvg(vendorSellPrices);
                int sellCount = (int) entries.stream().filter(e -> "SELL".equals(e.mode())).count();
                int buyCount  = (int) entries.stream().filter(e -> "BUY".equals(e.mode())).count();

                List<Integer> history = histData.getHistory(itemId.toString(), 10);
                PriceTrend trend = computeTrend(histData.getHistory(itemId.toString()));

                ItemStack display = entries.get(0).sellingItem().copyWithCount(1);

                // Collect barter payment item from TableCloth SELL shops (first non-empty)
                ItemStack barterItem = entries.stream()
                        .filter(e -> "TABLECLOTH".equals(e.shopType()) && "SELL".equals(e.mode())
                                && !e.priceItem().isEmpty())
                        .map(e -> e.priceItem().copyWithCount(1))
                        .findFirst()
                        .orElse(ItemStack.EMPTY);

                result.add(new MarketEntry(itemId, display, minPrice, avgPrice,
                        sellCount, buyCount, trend, history, barterItem));
            }
            return result;
        } catch (Exception e) {
            LOGGER.debug("[MarketRegistry] buildEntries failed: {}", e.getMessage());
            return List.of();
        }
    }

    public static void takeSnapshot(MinecraftServer server) {
        try {
            List<MarketEntry> entries = buildEntries(server);
            PriceHistorySavedData histData = PriceHistorySavedData.getOrCreate(server);
            for (MarketEntry entry : entries) {
                if (entry.avgPrice() > 0) {
                    histData.addSnapshot(entry.itemId().toString(), entry.avgPrice());
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[MarketRegistry] takeSnapshot failed: {}", e.getMessage());
        }
    }

    static PriceTrend computeTrend(List<Integer> history) {
        if (history.size() < 2) return PriceTrend.STABLE;
        int prev = history.get(history.size() - 2);
        int last = history.get(history.size() - 1);
        if (last - prev > 1) return PriceTrend.RISING;
        if (prev - last > 1) return PriceTrend.FALLING;
        return PriceTrend.STABLE;
    }

    static int computeAvg(List<Integer> prices) {
        if (prices.isEmpty()) return 0;
        return (int) prices.stream().mapToInt(i -> i).average().orElse(0);
    }
}
