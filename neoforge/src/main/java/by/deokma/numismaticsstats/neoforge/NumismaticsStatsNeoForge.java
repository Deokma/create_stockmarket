package by.deokma.numismaticsstats.neoforge;

import by.deokma.numismaticsstats.CommonInit;
import by.deokma.numismaticsstats.NumismaticsStats;
import by.deokma.numismaticsstats.block.MarketTerminalBlock;
import by.deokma.numismaticsstats.block.MarketTerminalBlockEntity;
import by.deokma.numismaticsstats.neoforge.command.ShopListCommand;
import by.deokma.numismaticsstats.neoforge.market.MarketEvents;
import by.deokma.numismaticsstats.neoforge.network.NetworkHandler;
import by.deokma.numismaticsstats.neoforge.network.OpenStockMarketPacket;
import by.deokma.numismaticsstats.neoforge.shop.VendorEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(NumismaticsStats.MOD_ID)
public final class NumismaticsStatsNeoForge {

    private static final Logger LOGGER = LogManager.getLogger(NumismaticsStats.MOD_ID);

    private final IEventBus modEventBus;

    public NumismaticsStatsNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        this.modEventBus = modEventBus;
        LOGGER.info("[{}] NeoForge entrypoint initialising", NumismaticsStats.MOD_ID);

        CommonInit.init();
        NetworkHandler.register(modEventBus);
        VendorEvents.register();
        MarketEvents.register();

        // Register block/item/block-entity deferred registers
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlocks.BLOCK_ENTITIES.register(modEventBus);

        // Wire the BlockEntityType supplier so MarketTerminalBlockEntity can use it
        MarketTerminalBlockEntity.setTypeSupplier(ModBlocks.MARKET_TERMINAL_BE);

        // Hook: when player right-clicks MarketTerminalBlock, send OpenStockMarketPacket
        MarketTerminalBlock.setOpenScreenHandler(player ->
                NetworkHandler.sendToPlayer(player, new OpenStockMarketPacket())
        );

        modEventBus.addListener(this::clientSetup);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        ClientSetup.init(modEventBus);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ShopListCommand.register(event.getDispatcher());
    }
}
