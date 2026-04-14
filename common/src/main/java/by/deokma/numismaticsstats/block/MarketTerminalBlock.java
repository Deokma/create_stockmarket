package by.deokma.numismaticsstats.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Consumer;

public class MarketTerminalBlock extends Block {

    public static final MapCodec<MarketTerminalBlock> CODEC = simpleCodec(MarketTerminalBlock::new);

    /** Platform registers this hook during initialization. */
    private static Consumer<ServerPlayer> openScreenHandler = player -> {};

    public static void setOpenScreenHandler(Consumer<ServerPlayer> handler) {
        openScreenHandler = handler;
    }

    public MarketTerminalBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openScreenHandler.accept(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
