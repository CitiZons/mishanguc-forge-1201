package pers.solid.mishang.uc.block;

import com.google.common.collect.Maps;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import pers.solid.mishang.uc.data.stubs.BlockStateSupplier;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.MishangucProperties;
import pers.solid.mishang.uc.data.ModelHelper;
import pers.solid.mishang.uc.util.HorizontalCornerDirection;

import java.util.Map;

/**
 * 栏杆外部角落的方块。当两个栏杆方块靠边直角围起来的时候，可以设置一个这样的外部角落方块，以填补两个栏杆之间的空隙。<br>
 * 同 {@link HandrailCornerBlock} 一样，本方块也是水平角落朝向的，默认朝向为西南方。
 */
public abstract class HandrailOuterBlock<T extends HandrailBlock> extends Block implements SimpleWaterloggedBlock, MishangucBlock, Handrails {
  public static final EnumProperty<HorizontalCornerDirection> FACING = MishangucProperties.HORIZONTAL_CORNER_FACING;
  public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
  public static final Map<HorizontalCornerDirection, VoxelShape> SHAPES = Util.make(() -> {

    final Map<Direction, @Nullable VoxelShape> map1 = MishangUtils.createHorizontalDirectionToShape(0.5, 0, 13.5, 2.5, 16, 15.5);
    final Map<Direction, @Nullable VoxelShape> map2 = MishangUtils.createHorizontalDirectionToShape(0.5, 0, 15.5, 2.5, 16, 16);
    return Direction.Plane.HORIZONTAL.stream().collect(Maps.toImmutableEnumMap(direction -> HorizontalCornerDirection.fromDirections(direction, direction.getClockWise()), direction -> Shapes.or(map1.get(direction), map2.get(direction))));
  });
  public final @NotNull T baseHandrail;

  public HandrailOuterBlock(@NotNull T baseHandrail, Properties settings) {
    super(settings);
    this.baseHandrail = baseHandrail;
    registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(FACING, HorizontalCornerDirection.SOUTH_WEST));
  }

  public HandrailOuterBlock(@NotNull T baseHandrail) {
    this(baseHandrail, BlockBehaviour.Properties.copy(baseHandrail));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(WATERLOGGED, FACING);
  }

  @Override
  public @Nullable Block baseBlock() {
    return baseHandrail.baseBlock();
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState rotate(BlockState state, Rotation rotation) {
    return super.rotate(state, rotation).setValue(FACING, state.getValue(FACING).rotate(rotation));
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState mirror(BlockState state, Mirror mirror) {
    return super.mirror(state, mirror).setValue(FACING, state.getValue(FACING).mirror(mirror));
  }

  @SuppressWarnings("deprecation")
  @Override
  public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    return SHAPES.get(state.getValue(FACING));
  }

  @SuppressWarnings("deprecation")
  @Override
  public FluidState getFluidState(BlockState state) {
    return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext ctx) {
    final BlockState placementState = super.getStateForPlacement(ctx);
    if (placementState == null) return null;
    return placementState.setValue(FACING, HorizontalCornerDirection.fromRotation(ctx.getRotation())).setValue(WATERLOGGED, ctx.getLevel().getFluidState(ctx.getClickedPos()).getType() == Fluids.WATER);
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
    if (state.getValue(WATERLOGGED)) {
      world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
    }
    return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
    final Block block = stateFrom.getBlock();
    if (direction.getAxis().isHorizontal() && block instanceof final Handrails handrails) {
      return block.asItem() == asItem()
          && handrails.connectsIn(stateFrom, direction.getOpposite(), state.getValue(FACING).getDirectionInAxis(direction.getClockWise().getAxis()).getOpposite());
    }
    return super.skipRendering(state, stateFrom, direction);
  }

  @Override
  public boolean connectsIn(@NotNull BlockState blockState, @NotNull Direction direction, @Nullable Direction offsetFacing) {
    final HorizontalCornerDirection facing = blockState.getValue(FACING);
    return offsetFacing != null && facing.hasDirection(direction) && facing.hasDirection(offsetFacing.getOpposite());
  }

  @Override
  public Item asItem() {
    return baseHandrail.asItem();
  }

  public @NotNull BlockStateSupplier createBlockStates(ResourceLocation modeId) {
    return ModelHelper.stateForHorizontalCornerFacingBlock(this, modeId, true);
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {
    return false;
  }
}
