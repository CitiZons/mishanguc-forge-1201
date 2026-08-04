package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.Block;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
// TODO: Forge data gen - BlockStateModelGenerator
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.ResourceLocation;
import pers.solid.mishang.uc.data.MishangucModels;

public class FullLightBlock extends Block implements MishangucBlock {
  private final Item dyeIngredient;
  private final Item concreteIngredient;

  public FullLightBlock(Properties settings, Item dyeIngredient, Item concreteIngredient) {
    super(settings);
    this.dyeIngredient = dyeIngredient;
    this.concreteIngredient = concreteIngredient;
  }

  @Override
  public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    final ResourceLocation modelId = MishangucModels.LIGHT.upload(this, TextureMap.all(this), blockStateModelGenerator.modelCollector);
    blockStateModelGenerator.blockStateCollector.accept(BlockStateModelGenerator.createSingletonBlockState(this, modelId));
    blockStateModelGenerator.registerParentedItemModel(this, modelId);
  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, this, 8)
        .pattern("*#*")
        .pattern("#C#")
        .pattern("*#*")
        .define('*', dyeIngredient)
        .define('#', Items.GLOWSTONE)
        .define('C', concreteIngredient)
        .unlockedBy(FabricRecipeProvider.getHasName(dyeIngredient), FabricRecipeProvider.has(dyeIngredient))
        .unlockedBy(FabricRecipeProvider.getHasName(Items.GLOWSTONE), FabricRecipeProvider.has(Items.GLOWSTONE))
        .unlockedBy(FabricRecipeProvider.getHasName(concreteIngredient), FabricRecipeProvider.has(concreteIngredient));
  }

  @Override
  public String customRecipeCategory() {
    return "light";
  }
}
