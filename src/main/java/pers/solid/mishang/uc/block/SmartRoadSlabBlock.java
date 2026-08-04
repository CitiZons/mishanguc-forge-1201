package pers.solid.mishang.uc.block;

import net.minecraft.Util;

import pers.solid.mishang.uc.MishangUtils;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.BlockStateSupplier;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.util.*;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.data.ModelHelper;
import pers.solid.mishang.uc.util.LineColor;
import pers.solid.mishang.uc.util.LineType;
import pers.solid.mishang.uc.util.RoadConnectionState;

import java.util.List;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;

/**
 * 根据其基础方块来生成台阶方块。
 *
 * @param <T> 基础方块类型。
 */
public class SmartRoadSlabBlock<T extends AbstractRoadBlock> extends AbstractRoadSlabBlock {
  private static Block cachedBaseBlock;
  public final T baseBlock;

  public SmartRoadSlabBlock(T baseBlock) {
    super(baseBlock, Util.make(() -> {
      cachedBaseBlock = baseBlock;
      return BlockBehaviour.Properties.copy(baseBlock);
    }));
    this.baseBlock = baseBlock;
  }

  @Override
  public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    // 由于该方法是在构造方法中执行的，所以可能存在 null 的情况。
    (baseBlock == null ? cachedBaseBlock : baseBlock)
        .getStateDefinition()
        .getProperties()
        .forEach(builder::add);
  }

  @Override
  public LineColor getLineColor(BlockState blockState, Direction direction) {
    return baseBlock.getLineColor(blockState, direction);
  }

  @Override
  public LineType getLineType(BlockState blockState, Direction direction) {
    return baseBlock.getLineType(blockState, direction);
  }

  @Override
  public void appendDescriptionTooltip(List<Component> tooltip, TooltipFlag options) {
    baseBlock.appendDescriptionTooltip(tooltip, options);
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext ctx) {
    BlockPos blockPos = ctx.getClickedPos();
    BlockState blockState = ctx.getLevel().getBlockState(blockPos);
    if (blockState.is(this)) {
      return super.getStateForPlacement(ctx);
    } else {
      return baseBlock.withPlacementState(super.getStateForPlacement(ctx), ctx);
    }
  }

  @Override
  public BlockState rotate(BlockState state, Rotation rotation) {
    return baseBlock.rotate(state, rotation);
  }

  @Override
  public BlockState mirror(BlockState state, Mirror mirror) {
    return baseBlock.mirror(state, mirror);
  }

  @Override
  public InteractionResult use(
      BlockState state,
      Level world,
      BlockPos pos,
      Player player,
      InteractionHand hand,
      BlockHitResult hit) {
    final InteractionResult result = super.use(state, world, pos, player, hand, hit);
    if (result == InteractionResult.FAIL) {
      return result;
    } else {
      return onUseRoad(state, world, pos, player, hand, hit);
    }
  }

  @Override
  public void neighborChanged(
      BlockState state, Level world, BlockPos pos, Block block, BlockPos sourcePos, boolean notify) {
    baseBlock.neighborChanged(state, world, pos, block, sourcePos, notify);
  }

  @Override
  public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
    return MishangUtils.getStateWithProperties(this, MishangUtils.getStateWithProperties(baseBlock, state).updateShape(direction, neighborState, world, pos, neighborPos))
        .setValue(TYPE, state.getValue(TYPE))
        .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
  }

  @Override
  public void appendRoadTooltip(
      ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag options) {
    baseBlock.appendRoadTooltip(stack, world, tooltip, options);
  }

  @Override
  public RoadConnectionState getConnectionStateOf(BlockState state, Direction direction) {
    return baseBlock.getConnectionStateOf(state, direction);
  }

  @Override
  public boolean shouldWriteStonecuttingRecipe() {
    return true;
  }

  @Override
  public SingleItemRecipeBuilder getStonecuttingRecipe() {
    return SingleItemRecipeBuilder.stonecutting(Ingredient.of(baseBlock), RecipeCategory.BUILDING_BLOCKS, this, 2)
        .unlockedBy(FabricRecipeProvider.getHasName(baseBlock), FabricRecipeProvider.has(baseBlock));
  }

  @Override
  public RecipeBuilder getPaintingRecipe(Block base, Block self) {
    return baseBlock.getPaintingRecipe(base, this);
  }

  @Override
  public BlockStateSupplier composeState(@NotNull BlockStateSupplier stateForFull) {
    return ModelHelper.composeStateForSlab(stateForFull);
  }
}
