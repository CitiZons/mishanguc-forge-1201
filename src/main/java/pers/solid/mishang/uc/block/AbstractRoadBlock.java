package pers.solid.mishang.uc.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.*;
import pers.solid.mishang.uc.data.stubs.TextureKey;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
import pers.solid.mishang.uc.data.stubs.BlockStateSupplier;
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.blocks.RoadBlocks;
import pers.solid.mishang.uc.blocks.RoadSlabBlocks;
import pers.solid.mishang.uc.data.MishangucModels;
import pers.solid.mishang.uc.util.LineColor;
import pers.solid.mishang.uc.util.LineType;

import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractRoadBlock extends Block implements Road {
  protected final LineColor lineColor;
  protected final LineType lineType;

  public AbstractRoadBlock(Properties settings, LineColor lineColor, LineType lineType) {
    super(settings);
    this.lineColor = lineColor;
    this.lineType = lineType;
  }

  @Override
  public LineType getLineType(BlockState state, Direction direction) {
    return lineType;
  }

  @Override
  public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    appendRoadProperties(builder);
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext ctx) {
    return withPlacementState(super.getStateForPlacement(ctx), ctx);
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState mirror(BlockState state, Mirror mirror) {
    return mirrorRoad(super.mirror(state, mirror), mirror);
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState rotate(BlockState state, Rotation rotation) {
    return rotateRoad(super.rotate(state, rotation), rotation);
  }

  @SuppressWarnings("deprecation")
  @Override
  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    InteractionResult result = super.use(state, world, pos, player, hand, hit);
    if (result == InteractionResult.FAIL) {
      return result;
    }
    return onUseRoad(state, world, pos, player, hand, hit);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void neighborChanged(
      BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
    super.neighborChanged(state, world, pos, sourceBlock, sourcePos, notify);
    neighborRoadUpdate(state, world, pos, sourceBlock, sourcePos, notify);
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
    return withStateForNeighborUpdate(super.updateShape(state, direction, neighborState, world, pos, neighborPos), direction, neighborState, world, pos, neighborPos);
  }

  @Override
  public LineColor getLineColor(BlockState state, Direction direction) {
    return lineColor;
  }

  @Override
  public void appendHoverText(
      ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag options) {
    super.appendHoverText(stack, world, tooltip, options);
    appendDescriptionTooltip(tooltip, options);
    appendRoadTooltip(stack, world, tooltip, options);
  }

  @ApiStatus.AvailableSince("1.1.0")
  @Contract(pure = true)
  public final AbstractRoadSlabBlock getRoadSlab() {
    return RoadSlabBlocks.BLOCK_TO_SLABS.get(this);
  }

  @Override
  public void writeRecipes(Consumer<FinishedRecipe> exporter) {
    Road.super.writeRecipes(exporter);
    final RecipeBuilder paintingRecipe = getPaintingRecipe(RoadBlocks.ROAD_BLOCK, this);
    if (paintingRecipe != null) {
      paintingRecipe.group(getRecipeGroup()).save(exporter, getPaintingRecipeId());
    }
  }

  @Override
  public final void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    registerBaseOrSlabModels(this, blockStateModelGenerator);
    blockStateModelGenerator.registerParentedItemModel(this, ModelIds.getBlockModelId(this));
  }

  /**
   * 注册基础方块或台阶方块的模型。此方块应该由基础方块调用，即 {@code this} 应该是基础方块，然而 {@code road} 可能是基础方块，也可能是台阶。
   */
  protected abstract <B extends Block & Road> void registerBaseOrSlabModels(B road, BlockStateModelGenerator blockStateModelGenerator);

  @Override
  public String getModelName(String suffix) {
    return "road" + suffix;
  }

  @Override
  public ResourceLocation uploadModel(String suffix, TextureMap textureMap, BlockStateModelGenerator blockStateModelGenerator, TextureKey... textureKeys) {
    return MishangucModels.createBlock(getModelName(suffix), textureKeys).upload(this, textureMap, blockStateModelGenerator.modelCollector);
  }

  @Override
  public ResourceLocation uploadModel(String suffix, String variant, TextureMap textureMap, BlockStateModelGenerator blockStateModelGenerator, TextureKey... textureKeys) {
    return MishangucModels.createBlock(getModelName(suffix), variant, textureKeys).upload(this, textureMap, blockStateModelGenerator.modelCollector);
  }

  @Override
  public BlockStateSupplier composeState(@NotNull BlockStateSupplier stateForFull) {
    return stateForFull;
  }
}
