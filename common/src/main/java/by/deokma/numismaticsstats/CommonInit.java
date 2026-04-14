package by.deokma.numismaticsstats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader-agnostic initialisation logic.
 *
 * Each platform entrypoint (NeoForge / Fabric) calls {@link #init()} at
 * the appropriate point in its own lifecycle.
 */
public final class CommonInit {

    private static final Logger LOGGER = LoggerFactory.getLogger(NumismaticsStats.MOD_ID);

    private CommonInit() {}

    public static void init() {
        LOGGER.info("[{}] Common init running", NumismaticsStats.MOD_ID);
    }
}
