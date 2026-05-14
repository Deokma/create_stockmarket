package by.deokma.stockmarket.block;

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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public class MarketTerminalBlock extends BaseEntityBlock {

    public static final MapCodec<MarketTerminalBlock> CODEC = simpleCodec(MarketTerminalBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** Platform registers this hook during initialization. */
    private static Consumer<ServerPlayer> openScreenHandler = player -> {};

    public static void setOpenScreenHandler(Consumer<ServerPlayer> handler) {
        openScreenHandler = handler;
    }

    // ── Hitbox (matches new model geometry, gears excluded) ──────────────────
    // Model coords (0–16). For FACING=NORTH (default orientation):
    //   keyboard side = low Z (north), screen side = high Z (south)
    private static final VoxelShape SHAPE_NORTH = buildNorthShape();

    private static VoxelShape buildNorthShape() {
        VoxelShape base   = Shapes.box(0,         0,        0,        1,         4.0/16,  1        ); // [0,0,0]→[16,4,16]
        VoxelShape back   = Shapes.box(0,         4.0/16,   9.0/16,   1,         8.0/16,  1        ); // [0,4,9]→[16,8,16]
        VoxelShape screen = Shapes.box(1.0/16,    8.0/16,   10.0/16,  15.0/16,   20.0/16, 15.0/16  ); // [1,8,10]→[15,20,15]
        VoxelShape keys   = Shapes.box(1.25/16,   4.0/16,   1.0/16,   14.75/16,  7.0/16,  7.0/16   ); // keyboard area
        return Shapes.or(base, back, screen, keys);
    }

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        SHAPES.put(Direction.NORTH, SHAPE_NORTH);
        SHAPES.put(Direction.SOUTH, rotateShapeY(SHAPE_NORTH, 180));
        SHAPES.put(Direction.EAST,  rotateShapeY(SHAPE_NORTH, 90));
        SHAPES.put(Direction.WEST,  rotateShapeY(SHAPE_NORTH, 270));
    }

    /** Rotates a VoxelShape around the Y axis by the given degrees (90/180/270). */
    private static VoxelShape rotateShapeY(VoxelShape shape, int deg) {
        VoxelShape[] result = { Shapes.empty() };
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            double nx1, nz1, nx2, nz2;
            switch (deg) {
                case 90  -> { nx1 = 1-z2; nz1 = x1;  nx2 = 1-z1; nz2 = x2;  }
                case 180 -> { nx1 = 1-x2; nz1 = 1-z2; nx2 = 1-x1; nz2 = 1-z1; }
                case 270 -> { nx1 = z1;   nz1 = 1-x2; nx2 = z2;   nz2 = 1-x1; }
                default  -> { nx1 = x1;   nz1 = z1;   nx2 = x2;   nz2 = z2;   }
            }
            result[0] = Shapes.or(result[0], Shapes.box(nx1, y1, nz1, nx2, y2, nz2));
        });
        return result[0];
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

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES.getOrDefault(state.getValue(FACING), SHAPE_NORTH);
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
