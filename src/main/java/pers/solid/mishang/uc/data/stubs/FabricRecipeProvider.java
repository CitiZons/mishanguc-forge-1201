package pers.solid.mishang.uc.data.stubs;

import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.function.Consumer;

/**
 * Stub for Fabric's FabricRecipeProvider (net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider).
 * Extends RecipeProvider to access protected methods.
 */
public abstract class FabricRecipeProvider extends RecipeProvider {

    public FabricRecipeProvider(FabricDataOutput output) {
        super(null);
    }

    /**
     * Override to generate recipes and pass them to the exporter.
     */
    public abstract void generate(Consumer<FinishedRecipe> exporter);

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> exporter) {
        generate(exporter);
    }

    public static String getHasName(ItemLike item) {
        return RecipeProvider.getHasName(item);
    }

    public static InventoryChangeTrigger.TriggerInstance has(ItemLike item) {
        return RecipeProvider.has(item);
    }

    public static InventoryChangeTrigger.TriggerInstance has(TagKey<Item> tag) {
        return RecipeProvider.has(tag);
    }

    public static net.minecraft.data.recipes.RecipeBuilder slabBuilder(RecipeCategory category, ItemLike slab, Ingredient material) {
        return RecipeProvider.slabBuilder(category, slab, material);
    }

    public static String getConversionRecipeName(ItemLike result, ItemLike ingredient) {
        return RecipeProvider.getConversionRecipeName(result, ingredient);
    }

    public static net.minecraft.resources.ResourceLocation getConversionRecipeName(ItemLike item) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.asItem());
    }

    public static net.minecraft.data.recipes.RecipeBuilder createStairsRecipe(ItemLike output, Ingredient material) {
        return RecipeProvider.stairBuilder(output, material);
    }

    public static InventoryChangeTrigger.TriggerInstance conditionsFromItemPredicates(net.minecraft.advancements.critereon.ItemPredicate... predicates) {
        return RecipeProvider.inventoryTrigger(predicates);
    }
}
