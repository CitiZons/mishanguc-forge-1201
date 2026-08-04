package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.Half;
import pers.solid.mishang.uc.data.stubs.*;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
import pers.solid.mishang.uc.data.stubs.VariantSettings;
import pers.solid.mishang.uc.data.stubs.Model;
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.data.MishangucModels;
import pers.solid.mishang.uc.data.MishangucTextureKeys;

import java.util.Map;

import static net.minecraft.world.level.material.Fluids.WATER;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.material.Fluids;
import com.mojang.math.Axis;

public class CornerLightBlock extends HorizontalDirectionalBlock
    implements SimpleWaterloggedBlock, LightConnectable, MishangucBlock {
  private static final EnumProperty<Half> BLOCK_HALF = BlockStateProperties.HALF;
  private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
  private static final Map<Direction, VoxelShape> SHAPE_PER_DIRECTION_WHEN_BOTTOM = MishangUtils.createDirectionToUnionShape(
      MishangUtils.createHorizontalDirectionToShape(4, 0, 0, 12, 1, 16),
      MishangUtils.createHorizontalDirectionToShape(4, 0, 0, 12, 16, 1));
  private static final Map<Direction, VoxelShape> SHAPE_PER_DIRECTION_WHEN_TOP = MishangUtils.createDirectionToUnionShape(
      MishangUtils.createHorizontalDirectionToShape(4, 15, 0, 12, 16, 16),
      MishangUtils.createHorizontalDirectionToShape(4, 0, 0, 12, 16, 1));
  public final String lightColor;

  public CornerLightBlock(String lightColor, Properties settings) {
    super(settings);
    this.lightColor = lightColor;
    this.registerDefaultState(defaultBlockState()
        .setValue(WATERLOGGED, false)
        .setValue(BLOCK_HALF, Half.BOTTOM));
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext ctx) {
    final BlockState placementState = super.getStateForPlacement(ctx);
    if (placementState == null) {
      return null;
    }
    final Direction side = ctx.getClickedFace();
    return placementState
        .setValue(WATERLOGGED, ctx.getLevel().getFluidState(ctx.getClickedPos()).getType() == WATER)
        .setValue(BLOCK_HALF,
            side == Direction.DOWN || ctx.getClickLocation().y - ctx.getClickedPos().getY() > 0.5
                ? Half.TOP
                : Half.BOTTOM)
        .setValue(FACING,
            Direction.Plane.HORIZONTAL.test(side) ? side : ctx.getHorizontalDirection().getOpposite());
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(FACING, BLOCK_HALF, WATERLOGGED);
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
    final Direction facing = state.getValue(FACING);
    final Direction backDirection = facing.getOpposite();
    final BlockPos backPos = pos.relative(backDirection);
    final VoxelShape centerShape = Block.box(7, 7, 7, 9, 9, 9);
    final BlockState backState = world.getBlockState(backPos);
    return !Shapes.joinIsNotEmpty(backState.getBlockSupportShape(world, backPos).getFaceShape(facing), centerShape, BooleanOp.ONLY_SECOND) || !Shapes.joinIsNotEmpty(backState.getCollisionShape(world, backPos).getFaceShape(facing), centerShape, BooleanOp.ONLY_SECOND);
  }

  @SuppressWarnings("deprecation")
  @Override
  public FluidState getFluidState(BlockState state) {
    return state.getValue(WATERLOGGED) ? WATER.getSource(false) : super.getFluidState(state);
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
    if (state.getValue(WATERLOGGED)) {
      world.scheduleTick(pos, WATER, WATER.getTickDelay(world));
    }

    return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
  }

  @SuppressWarnings("deprecation")
  @Override
  public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    return switch (state.getValue(BLOCK_HALF)) {
      case BOTTOM -> SHAPE_PER_DIRECTION_WHEN_BOTTOM.get(state.getValue(FACING));
      case TOP -> SHAPE_PER_DIRECTION_WHEN_TOP.get(state.getValue(FACING));
    };
  }

  @Override
  public boolean isConnectedIn(BlockState blockState, Direction facing, Direction direction) {
    final Direction facingProperty = blockState.getValue(FACING);
    final Half blockHalf = blockState.getValue(BLOCK_HALF);

    return switch (facing) {
      case UP -> blockHalf == Half.BOTTOM && direction.getAxis() == facingProperty.getAxis();
      case DOWN -> blockHalf == Half.TOP && direction.getAxis() == facingProperty.getAxis();
      default -> facing == facingProperty && direction.getAxis() == Direction.Axis.Y;
    };
  }

  @SuppressWarnings("deprecation")
  @Override
  public void updateIndirectNeighbourShapes(BlockState state, LevelAccessor world, BlockPos pos, int flags, int maxUpdateDepth) {
    super.updateIndirectNeighbourShapes(state, world, pos, flags, maxUpdateDepth);
    final Direction facing = state.getValue(FACING);
    final Direction facingVertical =
        state.getValue(BLOCK_HALF) == Half.TOP ? Direction.DOWN : Direction.UP;
    prepareConnection(state, world, pos, flags, maxUpdateDepth, facing);
    prepareConnection(state, world, pos, flags, maxUpdateDepth, facingVertical);
  }

  @Override
  public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    final TextureMap textures = TextureMap.of(MishangucTextureKeys.LIGHT, MishangucModels.texture(lightColor + "_light"));
    final ResourceLocation modelId = getModelType().upload(this, textures, blockStateModelGenerator.modelCollector);
    blockStateModelGenerator.blockStateCollector.accept(VariantsBlockStateSupplier.create(this, BlockStateVariant.create().put(VariantSettings.MODEL, modelId)).coordinate(BlockStateVariantMap.create(BLOCK_HALF, FACING).register((blockHalf, direction) -> {
      if (blockHalf == Half.BOTTOM) {
        return BlockStateVariant.create().put(MishangUtils.DIRECTION_Y_VARIANT, direction);
      } else {
        return BlockStateVariant.create().put(MishangUtils.DIRECTION_Y_VARIANT, direction.getOpposite()).put(VariantSettings.X, VariantSettings.Rotation.R180);
      }
    })));
    blockStateModelGenerator.registerParentedItemModel(this, modelId);
  }

  public Model getModelType() {
    final ResourceLocation identifier = BuiltInRegistries.BLOCK.getKey(this);
    String path = identifier.getPath();
    final int i = lightColor.length();
    try {
      if (path.startsWith(lightColor) && path.charAt(i) == '_') {
        path = path.substring(i + 1);
      }
    } catch (IndexOutOfBoundsException ignored) {
    }
    return MishangucModels.createBlock(path, MishangucTextureKeys.LIGHT);
  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    final ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(asItem());
    final ResourceLocation wallId = new ResourceLocation(itemId.getNamespace(), itemId.getPath().replace("_corner_", "_wall_"));
    if (wallId.equals(itemId)) {
      throw new IllegalStateException("Can't generate recipes: can't find the id of corresponding wall light block for " + this);
    }
    final @NotNull Item wall = BuiltInRegistries.ITEM.getOptional(wallId).orElseThrow(() -> new IllegalArgumentException(String.format("Can't generate recipes: can't find the corresponding wall light block with id [%s] for [%s]", wallId, itemId)));
    return ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, this, 1)
        .requires(wall)
        .requires(wall)
        .unlockedBy(FabricRecipeProvider.getHasName(wall), FabricRecipeProvider.has(wall));
  }
}
