package by.deokma.numismaticsstats.fabric;

import by.deokma.numismaticsstats.CommonInit;
import by.deokma.numismaticsstats.NumismaticsStats;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NumismaticsStatsFabric implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NumismaticsStats.MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[{}] Fabric entrypoint initialising", NumismaticsStats.MOD_ID);

        CommonInit.init();

        LOGGER.info("[{}] Fabric init complete", NumismaticsStats.MOD_ID);
    }
}
