package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricBlockLootTableProvider;

import net.minecraft.world.level.block.state.BlockBehaviour;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
// TODO: Forge data gen - BlockStateModelGenerator
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.blockentity.SimpleColoredBlockEntity;
import pers.solid.mishang.uc.data.MishangucModels;

import java.util.List;

public class ColoredStairsBlock extends StairBlock implements ColoredBlock {
  public final @NotNull Block baseBlock;

  public ColoredStairsBlock(@NotNull Block baseBlock, Properties settings) {
    super(baseBlock.defaultBlockState(), settings);
    this.baseBlock = baseBlock;
  }

  public ColoredStairsBlock(@NotNull Block baseBlock) {
    this(baseBlock, BlockBehaviour.Properties.copy(baseBlock));
  }

  @Override
  public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
    return getColoredPickStack(world, pos, state, super::getCloneItemStack);
  }

  @Override
  public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag options) {
    super.appendHoverText(stack, world, tooltip, options);
    ColoredBlock.appendColorTooltip(stack, tooltip);
  }

  @NotNull
  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new SimpleColoredBlockEntity(pos, state);
  }

  @Override
  public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    final TextureMap textureMap = baseBlock instanceof ColoredCubeBlock coloredCubeBlock ? coloredCubeBlock.textures : TextureMap.all(this);
    final ResourceLocation regularModelId = MishangucModels.COLORED_STAIRS.upload(this, textureMap, blockStateModelGenerator.modelCollector);
    final ResourceLocation innerModelId = MishangucModels.COLORED_INNER_STAIRS.upload(this, textureMap, blockStateModelGenerator.modelCollector);
    final ResourceLocation outerModelId = MishangucModels.COLORED_OUTER_STAIRS.upload(this, textureMap, blockStateModelGenerator.modelCollector);
    blockStateModelGenerator.blockStateCollector.accept(BlockStateModelGenerator.createStairsBlockState(this, innerModelId, regularModelId, outerModelId));
    blockStateModelGenerator.registerParentedItemModel(this, regularModelId);
  }

  @Override
  public LootTable.Builder getLootTable(FabricBlockLootTableProvider blockLootTableGenerator) {
    return blockLootTableGenerator.drops(this).apply(COPY_COLOR_LOOT_FUNCTION);
  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    return ((ShapedRecipeBuilder) FabricRecipeProvider.createStairsRecipe(this, Ingredient.of(baseBlock)))
        .unlockedBy(FabricRecipeProvider.getHasName(baseBlock), FabricRecipeProvider.has(baseBlock));
  }
}
