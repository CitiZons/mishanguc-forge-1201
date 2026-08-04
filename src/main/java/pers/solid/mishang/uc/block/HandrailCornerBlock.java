package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricBlockLootTableProvider;

import com.google.common.collect.Maps;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import pers.solid.mishang.uc.data.stubs.BlockStateSupplier;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
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

public abstract class HandrailCornerBlock<T extends HandrailBlock> extends Block implements SimpleWaterloggedBlock, MishangucBlock, Handrails {
  /**
   * 该方块的基础的栏杆方块。
   */
  public static final EnumProperty<HorizontalCornerDirection> FACING = MishangucProperties.HORIZONTAL_CORNER_FACING;
  public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
  public final @NotNull T baseHandrail;
  public static final Map<HorizontalCornerDirection, VoxelShape> SHAPES = Util.make(() -> {
    final Map<Direction, @Nullable VoxelShape> shapes1 = MishangUtils.createHorizontalDirectionToShape(0, 0, 0.5, 15, 16, 2.5);
    final Map<Direction, @Nullable VoxelShape> shapes2 = MishangUtils.createHorizontalDirectionToShape(0.5, 0, 1, 16, 16, 2.5);
    return Direction.Plane.HORIZONTAL.stream().collect(Maps.toImmutableEnumMap(direction -> HorizontalCornerDirection.fromDirections(direction, direction.getClockWise()), direction -> Shapes.or(shapes1.get(direction), shapes2.get(direction.getClockWise()))));
  });

  public HandrailCornerBlock(@NotNull T baseHandrail, Properties settings) {
    super(settings);
    registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(FACING, HorizontalCornerDirection.SOUTH_WEST));
    this.baseHandrail = baseHandrail;
  }

  public HandrailCornerBlock(@NotNull T baseHandrail) {
    this(baseHandrail, BlockBehaviour.Properties.copy(baseHandrail));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(WATERLOGGED, FACING);
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
  public FluidState getFluidState(BlockState state) {
    return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
    if (state.getValue(WATERLOGGED)) {
      world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
    }
    return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
  }

  @Override
  public Item asItem() {
    return baseHandrail.asItem();
  }

  public @NotNull BlockStateSupplier createBlockStates(ResourceLocation modelId) {
    return ModelHelper.stateForHorizontalCornerFacingBlock(this, modelId, true);
  }

  @SuppressWarnings("deprecation")
  @Override
  public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    return SHAPES.get(state.getValue(FACING));
  }

  @Override
  public LootTable.Builder getLootTable(FabricBlockLootTableProvider blockLootTableGenerator) {
    return blockLootTableGenerator.drops(this, ConstantValue.exactly(2));
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState rotate(BlockState state, Rotation rotation) {
    return super.rotate(state, rotation)
        .setValue(FACING, state.getValue(FACING).rotate(rotation));
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState mirror(BlockState state, Mirror mirror) {
    return super.mirror(state, mirror)
        .setValue(FACING, state.getValue(FACING).mirror(mirror));
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
    final Block block = stateFrom.getBlock();
    if (direction.getAxis().isHorizontal() && block instanceof final Handrails handrails) {
      return block.asItem() == asItem()
          && handrails.connectsIn(stateFrom, direction.getOpposite(), state.getValue(FACING).getDirectionInAxis(direction.getClockWise().getAxis()));
    }
    return super.skipRendering(state, stateFrom, direction);
  }

  @Override
  public @Nullable Block baseBlock() {
    return baseHandrail.baseBlock();
  }

  @Override
  public boolean connectsIn(@NotNull BlockState blockState, @NotNull Direction direction, @Nullable Direction offsetFacing) {
    final HorizontalCornerDirection facing = blockState.getValue(FACING);
    return offsetFacing != null && facing.hasDirection(direction) && facing.hasDirection(offsetFacing);
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {
    return false;
  }
}
