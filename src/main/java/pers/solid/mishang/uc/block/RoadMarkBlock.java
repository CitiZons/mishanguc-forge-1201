package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.*;
import pers.solid.mishang.uc.data.stubs.*;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
import pers.solid.mishang.uc.data.stubs.VariantSettings;
import pers.solid.mishang.uc.data.stubs.Model;
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.blocks.RoadMarkBlocks;
import pers.solid.mishang.uc.data.MishangucModels;
import pers.solid.mishang.uc.data.stubs.ConventionalItemTags;
import pers.solid.mishang.uc.util.EightHorizontalDirection;
import pers.solid.mishang.uc.util.FourHorizontalAxis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

@ApiStatus.AvailableSince("1.0.4")
public class RoadMarkBlock extends Block implements SimpleWaterloggedBlock, MishangucBlock {
  public static final VoxelShape SHAPE = box(0, 0, 0, 16, 1, 16);
  public static final VoxelShape SHAPE_X = box(0, 0, 2, 16, 1, 14);
  public static final VoxelShape SHAPE_Z = box(2, 0, 0, 14, 1, 16);
  public static final VoxelShape SHAPE_ON_SLAB = box(0, -8, 0, 16, -7, 16);
  public static final VoxelShape SHAPE_ON_SLAB_X = box(0, -8, 2, 16, -7, 14);
  public static final VoxelShape SHAPE_ON_SLAB_Z = box(2, -8, 0, 14, -7, 16);
  public static final BooleanProperty ON_SLAB = BooleanProperty.create("on_slab");
  protected final ResourceLocation texture;
  private static final VoxelShape SHAPE_TOP_MASK = box(0, 15.5, 0, 16, 16, 16);
  private static final VoxelShape SHAPE_SLAB_TOP_MASK = box(0, 7.5, 0, 16, 8, 16);

  public RoadMarkBlock(@NotNull ResourceLocation texture, Properties settings) {
    super(settings);
    this.texture = texture;
    registerDefaultState(defaultBlockState()
        .setValue(BlockStateProperties.WATERLOGGED, false)
        .setValue(ON_SLAB, false));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(BlockStateProperties.WATERLOGGED, ON_SLAB);
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
    final BlockPos downPos = pos.below();
    final BlockState downState = world.getBlockState(downPos);
    final VoxelShape downShape = downState.getBlockSupportShape(world, downPos);
    return !Shapes.joinIsNotEmpty(downShape, SHAPE_TOP_MASK, BooleanOp.ONLY_SECOND) || !Shapes.joinIsNotEmpty(downShape, SHAPE_SLAB_TOP_MASK, BooleanOp.ONLY_SECOND);
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext ctx) {
    BlockState state = super.getStateForPlacement(ctx);
    if (state != null) {
      final BlockPos blockPos = ctx.getClickedPos();
      final Level world = ctx.getLevel();
      state = state.setValue(BlockStateProperties.WATERLOGGED, world.getFluidState(blockPos).getType() == Fluids.WATER);
      final BlockPos downPos = blockPos.below();
      final BlockState downState = world.getBlockState(downPos);
      final VoxelShape downShape = downState.getBlockSupportShape(world, downPos);
      if (Shapes.joinIsNotEmpty(downShape, SHAPE_TOP_MASK, BooleanOp.ONLY_SECOND) && !Shapes.joinIsNotEmpty(downShape, SHAPE_SLAB_TOP_MASK, BooleanOp.ONLY_SECOND)) {
        state = state.setValue(ON_SLAB, true);
      }
    }
    return state;
  }

  @SuppressWarnings("deprecation")
  @Override
  public FluidState getFluidState(BlockState state) {
    return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
    if (state.getValue(BlockStateProperties.WATERLOGGED)) {
      world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
    }
    if (direction == Direction.DOWN) {
      if (!this.canSurvive(state, world, pos)) {
        return Blocks.AIR.defaultBlockState();
      } else {
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos)
            .setValue(ON_SLAB, Shapes.joinIsNotEmpty(world.getBlockState(neighborPos).getShape(world, neighborPos), SHAPE_TOP_MASK, BooleanOp.ONLY_SECOND));
      }
    }
    return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
  }

  @SuppressWarnings("deprecation")
  @Override
  public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    return state.getValue(ON_SLAB) ? SHAPE_ON_SLAB : SHAPE;
  }

  public static RoadMarkBlock createAxisFacing(ResourceLocation texture, Properties settings) {
    return new AxisFacing(texture, settings);
  }

  public static RoadMarkBlock createDirectionalFacing(ResourceLocation texture, Properties settings) {
    return new DirectionalFacing(texture, settings);
  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    return SingleItemRecipeBuilder.stonecutting(Ingredient.of(ConventionalItemTags.WHITE_DYES), RecipeCategory.DECORATIONS, this)
        .unlockedBy("has_white_dye", FabricRecipeProvider.has(ConventionalItemTags.WHITE_DYES));
  }

  @Override
  public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    final TextureMap textures = TextureMap.all(texture);
    final ResourceLocation modelId = MishangucModels.ROAD_MARK.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation onSlabModelId = MishangucModels.ROAD_MARK_ON_SLAB.upload(this, textures, blockStateModelGenerator.modelCollector);
    blockStateModelGenerator.blockStateCollector.accept(VariantsBlockStateSupplier.create(this)
        .coordinate(BlockStateVariantMap.create(ON_SLAB)
            .register(false, new BlockStateVariant().put(VariantSettings.MODEL, modelId))
            .register(true, new BlockStateVariant().put(VariantSettings.MODEL, onSlabModelId))));
    Models.HANDHELD.upload(ModelIds.getItemModelId(asItem()), TextureMap.layer0(texture), blockStateModelGenerator.modelCollector);
  }

  @Override
  public String customRecipeCategory() {
    return "road_marks";
  }

  protected static class AxisFacing extends RoadMarkBlock {
    public static final EnumProperty<FourHorizontalAxis> AXIS = EnumProperty.create("axis", FourHorizontalAxis.class);

    protected AxisFacing(ResourceLocation texture, Properties settings) {
      super(texture, settings);
      registerDefaultState(defaultBlockState().setValue(AXIS, FourHorizontalAxis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(AXIS);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
      final BlockState state = super.getStateForPlacement(ctx);
      if (state != null) {
        return state.setValue(AXIS, EightHorizontalDirection.fromRotation(ctx.getRotation()).axis);
      }
      return null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return switch (state.getValue(AXIS)) {
        case X -> state.getValue(ON_SLAB) ? SHAPE_ON_SLAB_X : SHAPE_X;
        case Z -> state.getValue(ON_SLAB) ? SHAPE_ON_SLAB_Z : SHAPE_Z;
        default -> super.getShape(state, world, pos, context);
      };
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
      return super.rotate(state, rotation).setValue(AXIS, state.getValue(AXIS).rotate(rotation));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
      BlockState mirror1 = super.mirror(state, mirror);
      if (RoadMarkBlocks.LEFT_TO_RIGHT.containsKey(this)) {
        mirror1 = MishangUtils.getStateWithProperties(RoadMarkBlocks.LEFT_TO_RIGHT.get(this), mirror1);
      } else if (RoadMarkBlocks.LEFT_TO_RIGHT.inverse().containsKey(this)) {
        mirror1 = MishangUtils.getStateWithProperties(RoadMarkBlocks.LEFT_TO_RIGHT.inverse().get(this), mirror1);
      }
      return mirror1.setValue(AXIS, state.getValue(AXIS).mirror());
    }

    @Override
    public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
      final TextureMap textures = TextureMap.all(texture);
      final ResourceLocation modelId = MishangucModels.ROAD_MARK.upload(this, textures, blockStateModelGenerator.modelCollector);
      final ResourceLocation rotatedModelId = MishangucModels.ROAD_MARK_ROTATED.upload(this, textures, blockStateModelGenerator.modelCollector);
      final ResourceLocation onSlabModelId = MishangucModels.ROAD_MARK_ON_SLAB.upload(this, textures, blockStateModelGenerator.modelCollector);
      final ResourceLocation onSlabRotatedModelId = MishangucModels.ROAD_MARK_ON_SLAB_ROTATED.upload(this, textures, blockStateModelGenerator.modelCollector);
      final BlockStateVariantMap.DoubleProperty<Boolean, FourHorizontalAxis> map = BlockStateVariantMap.create(ON_SLAB, AXIS)
          .register(false, FourHorizontalAxis.X, BlockStateVariant.create().put(VariantSettings.MODEL, modelId).put(MishangUtils.INT_Y_VARIANT, 90))
          .register(false, FourHorizontalAxis.NW_SE, BlockStateVariant.create().put(VariantSettings.MODEL, rotatedModelId).put(MishangUtils.INT_Y_VARIANT, 90))
          .register(false, FourHorizontalAxis.Z, BlockStateVariant.create().put(VariantSettings.MODEL, modelId).put(MishangUtils.INT_Y_VARIANT, 0))
          .register(false, FourHorizontalAxis.NE_SW, BlockStateVariant.create().put(VariantSettings.MODEL, rotatedModelId).put(MishangUtils.INT_Y_VARIANT, 0))
          .register(true, FourHorizontalAxis.X, BlockStateVariant.create().put(VariantSettings.MODEL, onSlabModelId).put(MishangUtils.INT_Y_VARIANT, 90))
          .register(true, FourHorizontalAxis.NW_SE, BlockStateVariant.create().put(VariantSettings.MODEL, onSlabRotatedModelId).put(MishangUtils.INT_Y_VARIANT, 90))
          .register(true, FourHorizontalAxis.Z, BlockStateVariant.create().put(VariantSettings.MODEL, onSlabModelId).put(MishangUtils.INT_Y_VARIANT, 0))
          .register(true, FourHorizontalAxis.NE_SW, BlockStateVariant.create().put(VariantSettings.MODEL, onSlabRotatedModelId).put(MishangUtils.INT_Y_VARIANT, 0));
      blockStateModelGenerator.blockStateCollector.accept(VariantsBlockStateSupplier.create(this).coordinate(map));
      Models.HANDHELD.upload(ModelIds.getItemModelId(asItem()), TextureMap.layer0(texture), blockStateModelGenerator.modelCollector);
    }
  }

  protected static class DirectionalFacing extends RoadMarkBlock {
    public static final EnumProperty<EightHorizontalDirection> FACING = EnumProperty.create("facing", EightHorizontalDirection.class);

    public DirectionalFacing(ResourceLocation texture, Properties settings) {
      super(texture, settings);
      registerDefaultState(defaultBlockState().setValue(FACING, EightHorizontalDirection.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
      final BlockState state = super.getStateForPlacement(ctx);
      if (state != null) {
        return state.setValue(FACING, EightHorizontalDirection.fromRotation(ctx.getRotation()));
      }
      return null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return switch (state.getValue(FACING).axis) {
        case X -> state.getValue(ON_SLAB) ? SHAPE_ON_SLAB_X : SHAPE_X;
        case Z -> state.getValue(ON_SLAB) ? SHAPE_ON_SLAB_Z : SHAPE_Z;
        default -> super.getShape(state, world, pos, context);
      };
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
      return super.rotate(state, rotation).setValue(FACING, state.getValue(FACING).rotate(rotation));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
      BlockState mirror1 = super.mirror(state, mirror);
      if (RoadMarkBlocks.LEFT_TO_RIGHT.containsKey(this)) {
        mirror1 = MishangUtils.getStateWithProperties(RoadMarkBlocks.LEFT_TO_RIGHT.get(this), mirror1);
      } else if (RoadMarkBlocks.LEFT_TO_RIGHT.inverse().containsKey(this)) {
        mirror1 = MishangUtils.getStateWithProperties(RoadMarkBlocks.LEFT_TO_RIGHT.inverse().get(this), mirror1);
      }
      return mirror1.setValue(FACING, state.getValue(FACING).mirror(mirror));
    }

    @Override
    public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
      final TextureMap textures = TextureMap.all(texture);
      final ResourceLocation modelId = MishangucModels.ROAD_MARK.upload(this, textures, blockStateModelGenerator.modelCollector);
      final ResourceLocation rotatedModelId = MishangucModels.ROAD_MARK_ROTATED.upload(this, textures, blockStateModelGenerator.modelCollector);
      final ResourceLocation onSlabModelId = MishangucModels.ROAD_MARK_ON_SLAB.upload(this, textures, blockStateModelGenerator.modelCollector);
      final ResourceLocation onSlabRotatedModelId = MishangucModels.ROAD_MARK_ON_SLAB_ROTATED.upload(this, textures, blockStateModelGenerator.modelCollector);
      final BlockStateVariantMap.DoubleProperty<Boolean, EightHorizontalDirection> map = BlockStateVariantMap.create(ON_SLAB, FACING);
      for (EightHorizontalDirection direction : EightHorizontalDirection.VALUES) {
        int rotation = (int) direction.asRotation();
        boolean rotated = direction.right().isPresent();
        if (rotated) {
          rotation -= 45;
        }
        map.register(false, direction, BlockStateVariant.create().put(VariantSettings.MODEL, rotated ? rotatedModelId : modelId).put(MishangUtils.INT_Y_VARIANT, rotation));
        map.register(true, direction, BlockStateVariant.create().put(VariantSettings.MODEL, rotated ? onSlabRotatedModelId : onSlabModelId).put(MishangUtils.INT_Y_VARIANT, rotation));
      }
      blockStateModelGenerator.blockStateCollector.accept(VariantsBlockStateSupplier.create(this).coordinate(map));
      Models.HANDHELD.upload(ModelIds.getItemModelId(asItem()), TextureMap.layer0(texture), blockStateModelGenerator.modelCollector);
    }
  }
}
