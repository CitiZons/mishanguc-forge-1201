package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import pers.solid.mishang.uc.data.stubs.When;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.*;
import pers.solid.mishang.uc.data.stubs.*;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
import pers.solid.mishang.uc.data.stubs.BlockStateSupplier;
import pers.solid.mishang.uc.data.stubs.VariantSettings;
import pers.solid.mishang.uc.data.stubs.Model;
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.data.MishangucModels;
import pers.solid.mishang.uc.data.ModelHelper;
import pers.solid.mishang.uc.util.TextBridge;

import java.util.Map;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import com.mojang.math.Axis;

/**
 * 悬挂的告示牌上面的专用的悬挂物方块。其方块状态会与其下方的悬挂告示牌方块同步。
 */
public class HungSignBarBlock extends Block implements SimpleWaterloggedBlock, MishangucBlock {

  public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
  public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
  public static final BooleanProperty LEFT = HungSignBlock.LEFT;
  public static final BooleanProperty RIGHT = HungSignBlock.RIGHT;
  private static final Map<Direction, @Nullable VoxelShape> BAR_SHAPES =
      MishangUtils.createHorizontalDirectionToShape(7.5, 0, 11, 8.5, 16, 12);
  private static final Map<Direction, @Nullable VoxelShape> BAR_SHAPES_EDGE =
      MishangUtils.createHorizontalDirectionToShape(7.5, 0, 13, 8.5, 16, 14);
  private static final Map<Direction, @Nullable VoxelShape> BAR_SHAPES_WIDE =
      MishangUtils.createHorizontalDirectionToShape(6.5, 0, 10, 9.5, 16, 13);
  private static final Map<Direction, @Nullable VoxelShape> BAR_SHAPES_EDGE_WIDE =
      MishangUtils.createHorizontalDirectionToShape(6.5, 0, 12, 9.5, 16, 15);
  /**
   * 当 left 和 right 均为 false 时，显示在正中央，采用此轮廓。
   */
  private static final VoxelShape BAR_SHAPE_CENTRAL = box(7.5, 0, 7.5, 8.5, 16, 8.5);

  private static final VoxelShape BAR_SHAPE_CENTRAL_WIDE = box(6.5, 0, 6.5, 9.5, 16, 9.5);
  public final @Nullable Block baseBlock;
  /**
   * 告示牌杆的纹理。若为 {@code null}，则根据其 {@link #baseBlock} 的 id 来推断。
   */
  public ResourceLocation texture;

  public HungSignBarBlock(@Nullable Block baseBlock, Properties settings) {
    super(settings);
    this.baseBlock = baseBlock;
    this.registerDefaultState(defaultBlockState()
        .setValue(WATERLOGGED, false)
        .setValue(AXIS, Direction.Axis.X)
        .setValue(LEFT, true)
        .setValue(RIGHT, true));
  }

  @ApiStatus.AvailableSince("0.1.7")
  public HungSignBarBlock(@NotNull Block baseBlock) {
    this(baseBlock, BlockBehaviour.Properties.copy(baseBlock).mapColor(baseBlock.defaultMapColor()));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(AXIS, WATERLOGGED, LEFT, RIGHT);
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext ctx) {
    final BlockState placementState = super.getStateForPlacement(ctx);
    if (placementState == null) {
      return null;
    }
    final Level world = ctx.getLevel();
    final BlockPos blockPos = ctx.getClickedPos();
    final BlockPos downPos = blockPos.below();

    // 考虑放置之初，底部若为悬挂的告示牌方块，则该方块没有连接，因此在
    // updateShape 的时候，将 neighborState 设为假定连接后的 state。
    // 注意，悬挂告示牌方块的 updateShape 并不会检查其上方的告示牌杆的属性是否匹配，只要存在就行。
    return placementState.updateShape(
            Direction.DOWN,
            world.getBlockState(downPos)
                .updateShape(Direction.UP, placementState, world, downPos, blockPos),
            world,
            blockPos,
            downPos)
        .setValue(WATERLOGGED, world.getFluidState(blockPos).getType() == Fluids.WATER);
  }

  @SuppressWarnings("deprecation")
  @Override
  public FluidState getFluidState(BlockState state) {
    return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
  }

  @SuppressWarnings("deprecation")
  @Override
  public VoxelShape getShape(
      BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    final Direction.Axis axis = state.getValue(AXIS);
    final Boolean left = state.getValue(LEFT);
    final Boolean right = state.getValue(RIGHT);

    if (left && right) {
      return BAR_SHAPE_CENTRAL_WIDE;
    }
    final Map<Direction, @Nullable VoxelShape> barShapes =
        BAR_SHAPES_WIDE;
    final Map<Direction, @Nullable VoxelShape> barShapesEdge =
        BAR_SHAPES_EDGE_WIDE;
    switch (axis) {
      case X:
        if (!(left || right))
          return Shapes.or(
              barShapesEdge.get(Direction.SOUTH), barShapesEdge.get(Direction.NORTH));
        else
          return Shapes.or(
              !left ? barShapes.get(Direction.SOUTH) : Shapes.empty(),
              !right ? barShapes.get(Direction.NORTH) : Shapes.empty());
      case Z:
        if (!(left || right))
          return Shapes.or(
              barShapesEdge.get(Direction.WEST), barShapesEdge.get(Direction.EAST));
        else
          return Shapes.or(
              !left ? barShapes.get(Direction.WEST) : Shapes.empty(),
              !right ? barShapes.get(Direction.EAST) : Shapes.empty());
      default:
        return Shapes.empty();
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public VoxelShape getBlockSupportShape(BlockState state, BlockGetter world, BlockPos pos) {
    return getShape(state, world, pos, CollisionContext.empty());
  }

  @SuppressWarnings("deprecation")
  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    final Direction.Axis axis = state.getValue(AXIS);
    final Boolean left = state.getValue(LEFT);
    final Boolean right = state.getValue(RIGHT);
    if (left && right) {
      return BAR_SHAPE_CENTRAL;
    }
    final Map<Direction, @Nullable VoxelShape> barShapes =
        BAR_SHAPES;
    final Map<Direction, @Nullable VoxelShape> barShapesEdge =
        BAR_SHAPES_EDGE;
    switch (axis) {
      case X:
        if (!(left || right))
          return Shapes.or(
              barShapesEdge.get(Direction.SOUTH), barShapesEdge.get(Direction.NORTH));
        else
          return Shapes.or(
              !left ? barShapes.get(Direction.SOUTH) : Shapes.empty(),
              !right ? barShapes.get(Direction.NORTH) : Shapes.empty());
      case Z:
        if (!(left || right))
          return Shapes.or(
              barShapesEdge.get(Direction.WEST), barShapesEdge.get(Direction.EAST));
        else
          return Shapes.or(
              !left ? barShapes.get(Direction.WEST) : Shapes.empty(),
              !right ? barShapes.get(Direction.EAST) : Shapes.empty());
      default:
        return Shapes.empty();
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
    return getCollisionShape(state, world, pos, CollisionContext.empty());
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState updateShape(
      BlockState state,
      Direction direction,
      BlockState neighborState,
      LevelAccessor world,
      BlockPos pos,
      BlockPos neighborPos) {
    state =
        super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    if (state.getValue(WATERLOGGED)) {
      world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
    }
    if (direction == Direction.DOWN) {
      final Block neighborBlock = neighborState.getBlock();
      if (neighborBlock instanceof HungSignBlock || neighborBlock instanceof HungSignBarBlock) {
        state = state
            .setValue(AXIS, neighborState.getValue(AXIS))
            .setValue(LEFT, neighborState.getValue(LEFT))
            .setValue(RIGHT, neighborState.getValue(RIGHT));
      } else state = state.setValue(LEFT, true).setValue(RIGHT, true);
    }
    return state;
  }

  /**
   * 和 {@link HungSignBlock#rotate} 一致。
   */
  @SuppressWarnings("deprecation")
  @Override
  public BlockState rotate(BlockState state, Rotation rotation) {
    final Direction.Axis oldAxis = state.getValue(AXIS);
    state = super.rotate(state, rotation)
        .setValue(
            AXIS,
            rotation == Rotation.CLOCKWISE_90
                || rotation == Rotation.COUNTERCLOCKWISE_90
                ? (oldAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X)
                : oldAxis);
    if (rotation == Rotation.CLOCKWISE_180
        || (oldAxis == Direction.Axis.X && rotation == Rotation.COUNTERCLOCKWISE_90)
        || (oldAxis == Direction.Axis.Z && rotation == Rotation.CLOCKWISE_90)) {
      state = state.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    return state;
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState mirror(BlockState state, Mirror mirror) {
    state = super.mirror(state, mirror);
    final Direction.Axis axis = state.getValue(AXIS);
    if ((axis == Direction.Axis.Z && mirror == Mirror.FRONT_BACK) || (axis == Direction.Axis.X && mirror == Mirror.LEFT_RIGHT)) {
      state = state.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    return state;
  }

  @Override
  public MutableComponent getName() {
    if (baseBlock != null) {
      return TextBridge.translatable("block.mishanguc.hung_sign_bar", baseBlock.getName());
    }
    return super.getName();
  }

  @Override
  public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    final TextureMap textures = TextureMap.texture(getBaseTexture());
    final ResourceLocation modelId = MishangucModels.HUNG_SIGN_BAR.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation centralModelId = MishangucModels.HUNG_SIGN_BAR_CENTRAL.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation edgeModelId = MishangucModels.HUNG_SIGN_BAR_EDGE.upload(this, textures, blockStateModelGenerator.modelCollector);

    blockStateModelGenerator.blockStateCollector.accept(createBlockStates(modelId, centralModelId, edgeModelId));
    blockStateModelGenerator.registerParentedItemModel(this, modelId);
  }

  public @Nullable BlockStateSupplier createBlockStates(ResourceLocation modelId, ResourceLocation centralModelId, ResourceLocation edgeModelId) {
    return MultipartBlockStateSupplier.create(this)
        .setValue(When.create().set(LEFT, true).set(RIGHT, true), BlockStateVariant.create().put(VariantSettings.MODEL, centralModelId).put(VariantSettings.UVLOCK, true))
        .setValue(When.create().set(AXIS, Direction.Axis.Z).set(LEFT, false).set(RIGHT, true), BlockStateVariant.create().put(VariantSettings.MODEL, modelId).put(VariantSettings.UVLOCK, true))
        .setValue(When.create().set(AXIS, Direction.Axis.Z).set(LEFT, true).set(RIGHT, false), BlockStateVariant.create().put(VariantSettings.MODEL, modelId).put(VariantSettings.UVLOCK, true).put(MishangUtils.INT_Y_VARIANT, 180))
        .setValue(When.create().set(AXIS, Direction.Axis.X).set(LEFT, false).set(RIGHT, true), BlockStateVariant.create().put(VariantSettings.MODEL, modelId).put(VariantSettings.UVLOCK, true).put(MishangUtils.INT_Y_VARIANT, -90))
        .setValue(When.create().set(AXIS, Direction.Axis.X).set(LEFT, true).set(RIGHT, false), BlockStateVariant.create().put(VariantSettings.MODEL, modelId).put(VariantSettings.UVLOCK, true).put(MishangUtils.INT_Y_VARIANT, 90))
        .setValue(When.create().set(AXIS, Direction.Axis.Z).set(LEFT, false).set(RIGHT, false), BlockStateVariant.create().put(VariantSettings.MODEL, edgeModelId).put(VariantSettings.UVLOCK, true))
        .setValue(When.create().set(AXIS, Direction.Axis.Z).set(LEFT, false).set(RIGHT, false), BlockStateVariant.create().put(VariantSettings.MODEL, edgeModelId).put(VariantSettings.UVLOCK, true).put(MishangUtils.INT_Y_VARIANT, 180))
        .setValue(When.create().set(AXIS, Direction.Axis.X).set(LEFT, false).set(RIGHT, false), BlockStateVariant.create().put(VariantSettings.MODEL, edgeModelId).put(VariantSettings.UVLOCK, true).put(MishangUtils.INT_Y_VARIANT, 90))
        .setValue(When.create().set(AXIS, Direction.Axis.X).set(LEFT, false).set(RIGHT, false), BlockStateVariant.create().put(VariantSettings.MODEL, edgeModelId).put(VariantSettings.UVLOCK, true).put(MishangUtils.INT_Y_VARIANT, 270));
  }

  public ResourceLocation getBaseTexture() {
    if (texture != null) return texture;
    return ModelHelper.getTextureOf(baseBlock == null ? this : baseBlock);
  }

  private @Nullable String getRecipeGroup() {
    if (baseBlock instanceof ColoredBlock) return null;
    if (MishangUtils.isWood(baseBlock)) return "mishanguc:wood_hung_sign_bar";
    if (MishangUtils.isStrippedWood(baseBlock)) return "mishanguc:stripped_wood_hung_sign_bar";
    if (MishangUtils.isConcrete(baseBlock)) return "mishanguc:concrete_hung_sign_bar";
    if (MishangUtils.isTerracotta(baseBlock)) return "mishanguc:terracotta_hung_sign_bar";
    if (baseBlock == Blocks.ICE || baseBlock == Blocks.PACKED_ICE || baseBlock == Blocks.BLUE_ICE) {
      return "mishanguc:ice_hung_sign_bar";
    }
    return null;
  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    return SingleItemRecipeBuilder.stonecutting(
            Ingredient.of(baseBlock),
            RecipeCategory.DECORATIONS,
            this,
            20)
        .unlockedBy("has_base_block", FabricRecipeProvider.has(baseBlock))
        .group(getRecipeGroup());
  }

  @Override
  public String customRecipeCategory() {
    return "signs";
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {
    return false;
  }
}
