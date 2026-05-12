package by.deokma.create_stockmarket.neoforge.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Optional integration with Create: Tradeworks (shelves and inverted table cloths).
 * Shop blocks keep using Create's {@code TableClothBlockEntity}; we detect Tradeworks via registry namespace.
 */
public final class TradeworksCompat {

    private static final Logger LOGGER = LogManager.getLogger("create_stockmarket");
    public static final String MOD_ID = "tradeworks";

    private static Boolean present = null;

    private TradeworksCompat() {}

    /** True if Create: Tradeworks is loaded. Cached after first call. */
    public static boolean isPresent() {
        if (present == null) {
            present = ModList.get().isLoaded(MOD_ID);
            LOGGER.info("[TradeworksCompat] Tradeworks present = {}", present);
        }
        return present;
    }

    /** True for Tradeworks blocks (shelves, inverted cloths). Safe without the mod installed. */
    public static boolean isTradeworksBlock(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(MOD_ID);
    }
}
