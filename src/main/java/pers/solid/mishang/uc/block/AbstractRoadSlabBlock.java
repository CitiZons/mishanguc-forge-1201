package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricBlockLootTableProvider;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.*;
import pers.solid.mishang.uc.data.stubs.TextureKey;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
import pers.solid.mishang.uc.data.stubs.Model;
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.data.recipes.RecipeCategory;
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
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.blocks.RoadBlocks;
import pers.solid.mishang.uc.data.MishangucModels;

import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractRoadSlabBlock extends SlabBlock implements Road {
  private final Block baseBlock;

  public AbstractRoadSlabBlock(Block baseBlock, Properties settings) {
    super(settings);
    this.baseBlock = baseBlock;
  }

  @Override
  public LootTable.Builder getLootTable(FabricBlockLootTableProvider blockLootTableGenerator) {
    return blockLootTableGenerator.createSlabItemTable(this);
  }

  @Override
  public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    appendRoadProperties(builder);
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext ctx) {
    BlockPos blockPos = ctx.getClickedPos();
    BlockState blockState = ctx.getLevel().getBlockState(blockPos);
    if (blockState.is(this)) {
      return super.getStateForPlacement(ctx);
    } else {
      return withPlacementState(super.getStateForPlacement(ctx), ctx);
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState rotate(BlockState state, Rotation rotation) {
    return rotateRoad(super.rotate(state, rotation), rotation);
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState mirror(BlockState state, Mirror mirror) {
    return mirrorRoad(super.mirror(state, mirror), mirror);
  }

  @SuppressWarnings("deprecation")
  @Override
  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    InteractionResult result = super.use(state, world, pos, player, hand, hit);
    if (result == InteractionResult.FAIL) {
      return result;
    } else {
      return onUseRoad(state, world, pos, player, hand, hit);
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public void neighborChanged(
      BlockState state, Level world, BlockPos pos, Block block, BlockPos sourcePos, boolean notify) {
    super.neighborChanged(state, world, pos, block, sourcePos, notify);
    neighborRoadUpdate(state, world, pos, block, sourcePos, notify);
  }

  @Override
  public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
    return withStateForNeighborUpdate(super.updateShape(state, direction, neighborState, world, pos, neighborPos), direction, neighborState, world, pos, neighborPos);
  }

  @OnlyIn(Dist.CLIENT)
  @Override
  public void appendHoverText(
      ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag options) {
    super.appendHoverText(stack, world, tooltip, options);
    appendDescriptionTooltip(tooltip, options);
    appendRoadTooltip(stack, world, tooltip, options);
  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    return FabricRecipeProvider.slabBuilder(RecipeCategory.BUILDING_BLOCKS, this, Ingredient.of(baseBlock))
        .unlockedBy(FabricRecipeProvider.getHasName(baseBlock), FabricRecipeProvider.has(baseBlock));
  }

  @Override
  public void writeRecipes(Consumer<FinishedRecipe> exporter) {
    Road.super.writeRecipes(exporter);
    final RecipeBuilder paintingRecipe = getPaintingRecipe(RoadBlocks.ROAD_BLOCK.getRoadSlab(), this);
    if (paintingRecipe != null) {
      paintingRecipe.group(getRecipeGroup()).save(exporter, getPaintingRecipeId());
    }
  }

  @Override
  public String getModelName(String suffix) {
    return "road_slab" + suffix;
  }

  @Override
  public final void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    ((AbstractRoadBlock) baseBlock).registerBaseOrSlabModels(this, blockStateModelGenerator);
    blockStateModelGenerator.registerParentedItemModel(this, ModelIds.getBlockModelId(this));
  }

  @Override
  public ResourceLocation uploadModel(String suffix, TextureMap textureMap, BlockStateModelGenerator blockStateModelGenerator, TextureKey... textureKeys) {
    final Model slabModel = MishangucModels.createBlock(getModelName(suffix), textureKeys);
    final Model slabTopModel = MishangucModels.createBlock(getModelName(suffix + "_top"), "_top", textureKeys);
    final ResourceLocation slabModelId = slabModel.upload(this, textureMap, blockStateModelGenerator.modelCollector);
    slabTopModel.upload(this, textureMap, blockStateModelGenerator.modelCollector);
    return slabModelId;
  }

  @Override
  public ResourceLocation uploadModel(String suffix, String variant, TextureMap textureMap, BlockStateModelGenerator blockStateModelGenerator, TextureKey... textureKeys) {
    final Model slabModel = MishangucModels.createBlock(getModelName(suffix), variant, textureKeys);
    final Model slabTopModel = MishangucModels.createBlock(getModelName(suffix + "_top"), variant + "_top", textureKeys);
    final ResourceLocation slabModelId = slabModel.upload(this, textureMap, blockStateModelGenerator.modelCollector);
    slabTopModel.upload(this, textureMap, blockStateModelGenerator.modelCollector);
    return slabModelId;
  }
}
