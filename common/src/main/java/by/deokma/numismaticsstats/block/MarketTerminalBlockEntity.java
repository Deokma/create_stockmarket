package by.deokma.numismaticsstats.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * BlockEntity for the Market Terminal.
 * Exists solely to support the client-side eye renderer.
 * No server-side data is stored here.
 *
 * TYPE is provided by the platform via setTypeSupplier() before any world loads.
 */
public class MarketTerminalBlockEntity extends BlockEntity {

    // Set by the platform (NeoForge) after DeferredRegister fires
    private static Supplier<BlockEntityType<MarketTerminalBlockEntity>> typeSupplier;

    public static void setTypeSupplier(Supplier<BlockEntityType<MarketTerminalBlockEntity>> supplier) {
        typeSupplier = supplier;
    }

    public MarketTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(typeSupplier.get(), pos, state);
    }
}
