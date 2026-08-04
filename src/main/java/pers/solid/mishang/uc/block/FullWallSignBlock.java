package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import com.google.common.collect.ImmutableMap;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.AttachFace;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
// TODO: Forge data gen - BlockStateModelGenerator
import pers.solid.mishang.uc.data.stubs.ModelIds;
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.blockentity.FullWallSignBlockEntity;
import pers.solid.mishang.uc.blocks.WallSignBlocks;
import pers.solid.mishang.uc.data.MishangucModels;
import pers.solid.mishang.uc.data.ModelHelper;
import pers.solid.mishang.uc.util.TextBridge;

import java.util.Map;

public class FullWallSignBlock extends WallSignBlock {
  public static final Map<Direction, VoxelShape> SHAPES_WHEN_WALL =
      MishangUtils.createHorizontalDirectionToShape(0, 0, 0, 16, 16, 1);
  public static final Map<Direction, VoxelShape> SHAPES_WHEN_FLOOR =
      MishangUtils.createHorizontalDirectionToShape(0, 0, 0, 16, 1, 16);
  public static final Map<Direction, VoxelShape> SHAPES_WHEN_CEILING =
      MishangUtils.createHorizontalDirectionToShape(0, 15, 0, 16, 16, 16);

  @Unmodifiable
  public static final Map<AttachFace, Map<Direction, VoxelShape>>
      SHAPE_PER_WALL_MOUNT_LOCATION =
      ImmutableMap.of(
          AttachFace.CEILING,
          SHAPES_WHEN_CEILING,
          AttachFace.FLOOR,
          SHAPES_WHEN_FLOOR,
          AttachFace.WALL,
          SHAPES_WHEN_WALL);

  public FullWallSignBlock(@Nullable Block baseBlock, Properties settings) {
    super(baseBlock, settings);
  }

  @ApiStatus.AvailableSince("0.1.7")
  public FullWallSignBlock(@NotNull Block baseBlock) {
    this(baseBlock, BlockBehaviour.Properties.copy(baseBlock));
  }

  @Override
  public MutableComponent getName() {
    return baseBlock == null
        ? super.getName()
        : TextBridge.translatable("block.mishanguc.full_wall_sign", baseBlock.getName());
  }

  @Override
  public VoxelShape getShape(
      BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    return SHAPE_PER_WALL_MOUNT_LOCATION.get(state.getValue(FACE)).get(state.getValue(FACING));
  }

  @Override
  public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new FullWallSignBlockEntity(pos, state);
  }

  private @Nullable String getRecipeGroup() {
    if (baseBlock instanceof ColoredBlock) return null;
    if (MishangUtils.isConcrete(baseBlock)) return "mishanguc:full_concrete_wall_sign";
    if (MishangUtils.isTerracotta(baseBlock)) return "mishanguc:full_terracotta_wall_sign";
    return null;
  }

  @Override
  public @Nullable RecipeBuilder getCraftingRecipe() {
    if (baseBlock == null) return null;
    return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, this, 4)
        .pattern("-#-")
        .pattern("###")
        .pattern("-#-")
        .define('#', baseBlock).define('-', WallSignBlocks.INVISIBLE_WALL_SIGN)
        .unlockedBy("has_base_block", FabricRecipeProvider.has(baseBlock))
        .unlockedBy("has_sign", FabricRecipeProvider.has(WallSignBlocks.INVISIBLE_WALL_SIGN))
        .group(getRecipeGroup());
  }

  @Override
  public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    if (this == WallSignBlocks.INVISIBLE_WALL_SIGN || this == WallSignBlocks.INVISIBLE_GLOWING_WALL_SIGN) {
      blockStateModelGenerator.blockStateCollector.accept(createBlockStates(ModelIds.getBlockModelId(this)));
      return;
    }
    final TextureMap textures = TextureMap.texture(ModelHelper.getTextureOf(baseBlock));
    final ResourceLocation modelId = MishangucModels.FULL_WALL_SIGN.upload(this, textures, blockStateModelGenerator.modelCollector);
    blockStateModelGenerator.blockStateCollector.accept(createBlockStates(modelId));
  }

  @Override
  public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
    if (direction.getAxis().isHorizontal() && state.getBlock() instanceof FullWallSignBlock && stateFrom.getBlock() instanceof FullWallSignBlock wallSignBlockFrom && state.getValue(FACING) == stateFrom.getValue(FACING) && direction.getAxis() != state.getValue(FACING).getAxis()) {
      if (wallSignBlockFrom.baseBlock instanceof HalfTransparentBlock) {
        if (baseBlock instanceof HalfTransparentBlock) {
          // 自身和相邻方块都为透明方块，则双方均为同一方块时隐藏。
          return baseBlock == wallSignBlockFrom.baseBlock;
        } else {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }
}
