package by.deokma.create_stockmarket.neoforge;

import by.deokma.create_stockmarket.CommonInit;
import by.deokma.create_stockmarket.CreateStockMarket;
import by.deokma.create_stockmarket.block.MarketTerminalBlock;
import by.deokma.create_stockmarket.block.MarketTerminalBlockEntity;
import by.deokma.create_stockmarket.neoforge.command.ShopListCommand;
import by.deokma.create_stockmarket.neoforge.market.MarketEvents;
import by.deokma.create_stockmarket.neoforge.network.NetworkHandler;
import by.deokma.create_stockmarket.neoforge.network.OpenStockMarketPacket;
import by.deokma.create_stockmarket.neoforge.shop.VendorEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CreateStockMarket.MOD_ID)
public final class CreateStockMarketNeoForge {

    private static final Logger LOGGER = LogManager.getLogger(CreateStockMarket.MOD_ID);

    private final IEventBus modEventBus;

    public CreateStockMarketNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        this.modEventBus = modEventBus;
        LOGGER.info("[{}] NeoForge entrypoint initialising", CreateStockMarket.MOD_ID);

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
