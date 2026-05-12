package by.deokma.stockmarket.fabric;

import by.deokma.stockmarket.CommonInit;
import by.deokma.stockmarket.CreateStockMarket;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CreateStockMarketFabric implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateStockMarket.MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[{}] Fabric entrypoint initialising", CreateStockMarket.MOD_ID);

        CommonInit.init();

        LOGGER.info("[{}] Fabric init complete", CreateStockMarket.MOD_ID);
    }
}
