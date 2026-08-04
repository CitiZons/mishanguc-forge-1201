package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import pers.solid.mishang.uc.data.stubs.*;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
import pers.solid.mishang.uc.data.stubs.VariantSettings;
import pers.solid.mishang.uc.data.stubs.Model;
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.NotNull;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.data.MishangucModels;
import pers.solid.mishang.uc.data.MishangucTextureKeys;
import com.mojang.math.Axis;

/**
 * 柱形灯方块，且没有底座，因此没有朝向，而是直接根据的坐标轴。
 */
public class ColumnLightBlock extends Block implements SimpleWaterloggedBlock, MishangucBlock {
  public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
  public final String lightColor;
  private final int sizeType;

  public ColumnLightBlock(String lightColor, Properties settings, int sizeType) {
    super(settings);
    this.lightColor = lightColor;
    this.sizeType = sizeType;
    registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(AXIS, BlockStateProperties.WATERLOGGED);
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
    if (state.getValue(BlockStateProperties.WATERLOGGED)) {
      world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
    }

    return super.updateShape(
        state, direction, neighborState, world, pos, neighborPos);
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState rotate(BlockState state, Rotation rotation) {
    return super.rotate(state, rotation).setValue(AXIS, MishangUtils.rotateAxis(rotation, state.getValue(AXIS)));
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext ctx) {
    Direction direction = ctx.getClickedFace();
    final Level world = ctx.getLevel();
    final BlockPos blockPos = ctx.getClickedPos();
    BlockState blockState = world.getBlockState(blockPos.relative(direction.getOpposite()));
    if (blockState.getBlockSupportShape(world, blockPos).getFaceShape(direction).isEmpty() && blockState.getShape(world, blockPos).getFaceShape(direction).isEmpty()) {
      return null;
    }
    return this.defaultBlockState()
        .setValue(AXIS, direction.getAxis())
        .setValue(BlockStateProperties.WATERLOGGED, world.getBlockState(blockPos).getFluidState().getType() == Fluids.WATER);
  }

  @SuppressWarnings("deprecation")
  @Override
  public FluidState getFluidState(BlockState state) {
    return state.getValue(BlockStateProperties.WATERLOGGED)
        ? Fluids.WATER.getSource(false)
        : super.getFluidState(state);
  }

  @SuppressWarnings("deprecation")
  @Override
  public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    return (sizeType >= 2 ? ColumnWallLightBlock.SHAPES4 : sizeType == 1 ? ColumnWallLightBlock.SHAPES5 : ColumnWallLightBlock.SHAPES6).get(state.getValue(AXIS));
  }

  @SuppressWarnings("deprecation")
  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    return (sizeType >= 2 ? ColumnWallLightBlock.SHAPES5 : sizeType == 1 ? ColumnWallLightBlock.SHAPES6 : ColumnWallLightBlock.SHAPES7).get(state.getValue(AXIS));
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
    return stateFrom.is(this) && state.getValue(AXIS).test(direction) && stateFrom.getValue(AXIS).test(direction) || super.skipRendering(state, stateFrom, direction);
  }

  @Override
  public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    final TextureMap textures = TextureMap.of(MishangucTextureKeys.LIGHT, MishangucModels.texture(lightColor + "_light"));
    final ResourceLocation modelId = getModelType().upload(this, textures, blockStateModelGenerator.modelCollector);
    blockStateModelGenerator.blockStateCollector.accept(VariantsBlockStateSupplier.create(this, BlockStateVariant.create()
            .put(VariantSettings.MODEL, modelId))
        .coordinate(BlockStateVariantMap.create(AXIS)
            .register(Direction.Axis.Y, BlockStateVariant.create())
            .register(Direction.Axis.X, BlockStateVariant.create()
                .put(VariantSettings.X, VariantSettings.Rotation.R270)
                .put(VariantSettings.Y, VariantSettings.Rotation.R90))
            .register(Direction.Axis.Z, BlockStateVariant.create()
                .put(VariantSettings.X, VariantSettings.Rotation.R270))));
    blockStateModelGenerator.registerParentedItemModel(this, modelId);
  }

  public Model getModelType() {
    final ResourceLocation identifier = BuiltInRegistries.BLOCK.getKey(this);
    String path = identifier.getPath();
    final int i = lightColor.length();
    if (path.startsWith(lightColor) && path.charAt(i) == '_') {
      path = path.substring(i + 1);
    } else {
      throw new AssertionError();
    }
    return MishangucModels.createBlock(path, MishangucTextureKeys.LIGHT);
  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    final ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(asItem());
    final String itemPath = itemId.getPath();
    if (itemPath.endsWith("_tube")) {
      final @NotNull Item fullLight = WallLightBlock.getBaseLight(itemId.getNamespace(), lightColor, this);
      final int outputCount;
      if (itemPath.contains("_thin_")) {
        outputCount = 32;
      } else if (itemPath.contains("_medium_")) {
        outputCount = 16;
      } else if (itemPath.contains("thick")) {
        outputCount = 8;
      } else {
        throw new IllegalStateException(String.format("Can't generate recipes: Cannot determine the type of %s according to its id", this));
      }
      return SingleItemRecipeBuilder.stonecutting(Ingredient.of(fullLight), RecipeCategory.DECORATIONS, this, outputCount)
          .unlockedBy(FabricRecipeProvider.getHasName(fullLight), FabricRecipeProvider.has(fullLight));
    } else {
      final ResourceLocation tubeId = itemId.withSuffix("_tube");
      final @NotNull Item tube = BuiltInRegistries.ITEM.getOptional(tubeId).orElseThrow(() -> new IllegalArgumentException(String.format("Can't generate recipes: %s does not have a corresponding tube block (with id [%s])", this, tubeId)));
      return ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, this, 1)
          .requires(tube)
          .requires(Items.GRAY_CONCRETE)
          .unlockedBy(FabricRecipeProvider.getHasName(tube), FabricRecipeProvider.has(tube));
    }
  }

  @Override
  public String customRecipeCategory() {
    return "light";
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {
    return false;
  }
}
