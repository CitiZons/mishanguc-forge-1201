package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricBlockLootTableProvider;

import net.minecraft.world.level.block.state.BlockBehaviour;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
// TODO: Forge data gen - BlockStateModelGenerator
import pers.solid.mishang.uc.data.stubs.ModelIds;
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import pers.solid.mishang.uc.data.MishangucModels;

@ApiStatus.AvailableSince("1.1.0")
public class LightSlabBlock extends SlabBlock implements MishangucBlock {
  public final Block baseBlock;

  public LightSlabBlock(@NotNull Block baseBlock, Properties settings) {
    super(settings);
    this.baseBlock = baseBlock;
  }

  public LightSlabBlock(@NotNull Block baseBlock) {
    super(BlockBehaviour.Properties.copy(baseBlock));
    this.baseBlock = baseBlock;
  }

  @Override
  public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    final ResourceLocation bottomModelId = MishangucModels.LIGHT_SLAB.upload(this, TextureMap.all(baseBlock), blockStateModelGenerator.modelCollector);
    final ResourceLocation topModelId = MishangucModels.LIGHT_SLAB_TOP.upload(this, TextureMap.all(baseBlock), blockStateModelGenerator.modelCollector);
    blockStateModelGenerator.blockStateCollector.accept(BlockStateModelGenerator.createSlabBlockState(this, bottomModelId, topModelId, ModelIds.getBlockModelId(baseBlock)));
    blockStateModelGenerator.registerParentedItemModel(this, bottomModelId);
  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    return ((ShapedRecipeBuilder) FabricRecipeProvider.slabBuilder(RecipeCategory.BUILDING_BLOCKS, this, Ingredient.of(baseBlock)))
        .unlockedBy(FabricRecipeProvider.getHasName(baseBlock), FabricRecipeProvider.has(baseBlock));
  }

  @Override
  public boolean shouldWriteStonecuttingRecipe() {
    return true;
  }

  @Override
  public SingleItemRecipeBuilder getStonecuttingRecipe() {
    return SingleItemRecipeBuilder.stonecutting(Ingredient.of(baseBlock), RecipeCategory.DECORATIONS, this, 2)
        .unlockedBy(FabricRecipeProvider.getHasName(baseBlock), FabricRecipeProvider.has(baseBlock));
  }

  @Override
  public LootTable.Builder getLootTable(FabricBlockLootTableProvider blockLootTableGenerator) {
    return blockLootTableGenerator.createSlabItemTable(this);
  }

  @Override
  public String customRecipeCategory() {
    return "light";
  }
}
