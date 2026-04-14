package by.deokma.numismaticsstats.neoforge;

import by.deokma.numismaticsstats.market.MarketData;
import by.deokma.numismaticsstats.neoforge.client.StockMarketScreen;
import by.deokma.numismaticsstats.neoforge.network.NetworkHandler;
import by.deokma.numismaticsstats.neoforge.network.RequestMarketPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public final class ClientSetup {

    public static final KeyMapping OPEN_STOCK_MARKET = new KeyMapping(
            "key.numismaticsstats.open_stock_market",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.numismaticsstats"
    );

    private ClientSetup() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::onRegisterKeys);
        NeoForge.EVENT_BUS.addListener(ClientSetup::onKeyInput);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STOCK_MARKET);
    }

    private static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (OPEN_STOCK_MARKET.consumeClick()) {
            MarketData.setLoading(true);
            NetworkHandler.sendToServer(new RequestMarketPacket());
            mc.setScreen(new StockMarketScreen());
        }
    }
}
