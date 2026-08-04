package pers.solid.mishang.uc.item;

import net.minecraft.data.recipes.RecipeBuilder;

public interface MishangucItem {
  default RecipeBuilder getCraftingRecipe() {
    return null;
  }

  default String customRecipeCategory() {
    return null;
  }
}
