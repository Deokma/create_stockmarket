package by.deokma.create_stockmarket.neoforge;

import by.deokma.create_stockmarket.neoforge.client.MarketTerminalRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class ClientSetup {
    private ClientSetup() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::onRegisterRenderers);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.MARKET_TERMINAL_BE.get(),
                MarketTerminalRenderer::new);
    }

}
