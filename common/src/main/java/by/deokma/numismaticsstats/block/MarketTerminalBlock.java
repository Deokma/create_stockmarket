package by.deokma.numismaticsstats.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Consumer;

public class MarketTerminalBlock extends BaseEntityBlock {

    public static final MapCodec<MarketTerminalBlock> CODEC = simpleCodec(MarketTerminalBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** Platform registers this hook during initialization. */
    private static Consumer<ServerPlayer> openScreenHandler = player -> {};

    public static void setOpenScreenHandler(Consumer<ServerPlayer> handler) {
        openScreenHandler = handler;
    }

    public MarketTerminalBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }
    private static final VoxelShape SHAPE = Shapes.or(
            Shapes.box(0.0,    0.0,   0.0,   1.0,    0.5,   1.0  ),  // нижний блок (0–8)
            Shapes.box(0.125,  0.5,   0.125, 0.875,  0.875, 0.875)   // верхняя рамка (2–14, 8–14)
    );

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MarketTerminalBlockEntity(pos, state);
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
