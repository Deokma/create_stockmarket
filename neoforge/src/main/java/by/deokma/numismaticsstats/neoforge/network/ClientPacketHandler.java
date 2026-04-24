package by.deokma.numismaticsstats.neoforge.network;

import by.deokma.numismaticsstats.market.MarketData;
import by.deokma.numismaticsstats.market.MarketEntry;
import by.deokma.numismaticsstats.market.TradeStatsData;
import by.deokma.numismaticsstats.neoforge.client.ShopListScreen;
import by.deokma.numismaticsstats.neoforge.client.StockMarketScreen;
import by.deokma.numismaticsstats.shop.ShopEntry;
import by.deokma.numismaticsstats.shop.ShopListData;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * All client-side packet handling logic lives here.
 * This class is only loaded on the CLIENT dist — never on the dedicated server.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    public static void handleShopList(List<ShopEntry> entries) {
        ShopListData.set(entries);
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof StockMarketScreen sms) {
            sms.refreshShopEntries();
        } else if (mc.screen instanceof ShopListScreen screen) {
            screen.refreshEntries();
        }
    }

    public static void handleOpenShopList() {
        Minecraft.getInstance().setScreen(new ShopListScreen());
    }

    public static void handleMarketData(List<MarketEntry> entries) {
        MarketData.set(entries);
        MarketData.setLoading(false);
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof StockMarketScreen screen) {
            screen.refreshEntries();
        }
    }

    public static void handleOpenStockMarket() {
        Minecraft.getInstance().setScreen(new StockMarketScreen());
    }

    public static void handleTradeStats(java.util.List<java.util.Map.Entry<String, Long>> topSellers) {
        TradeStatsData.set(topSellers);
        // Refresh market screen if open
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof StockMarketScreen screen) {
            screen.refreshEntries();
        }
    }
}
