package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricBlockLootTableProvider;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.Block;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
// TODO: Forge data gen - BlockStateModelGenerator
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import pers.solid.mishang.uc.data.stubs.TextureKey;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public interface MishangucBlock {
  default LootTable.Builder getLootTable(FabricBlockLootTableProvider blockLootTableGenerator) {
    return blockLootTableGenerator.drops((ItemLike) this);
  }

  default RecipeBuilder getCraftingRecipe() {
    return null;
  }

  default SingleItemRecipeBuilder getStonecuttingRecipe() {
    return null;
  }

  default ResourceLocation getStonecuttingRecipeId() {
    return FabricRecipeProvider.getConversionRecipeName((ItemLike) this).withSuffix("_from_stonecutting");
  }

  default boolean shouldWriteStonecuttingRecipe() {
    return false;
  }

  default void writeRecipes(Consumer<FinishedRecipe> exporter) {
    final RecipeBuilder craftingRecipe = getCraftingRecipe();
    if (craftingRecipe != null) {
      craftingRecipe.save(exporter);
    }
    if (shouldWriteStonecuttingRecipe()) {
      final SingleItemRecipeBuilder stonecuttingRecipe = getStonecuttingRecipe();
      if (stonecuttingRecipe != null) {
        stonecuttingRecipe.save(exporter, getStonecuttingRecipeId());
      }
    }
  }

  void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator);

  default ResourceLocation getTexture(TextureKey key) {
    return TextureMap.getId(((Block) this));
  }

  default String customRecipeCategory() {
    return null;
  }
}
