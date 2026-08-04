package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
// TODO: Forge data gen - BlockStateModelGenerator
import pers.solid.mishang.uc.data.stubs.Models;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.ItemTags;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import pers.solid.mishang.uc.util.LineColor;
import pers.solid.mishang.uc.util.LineType;
import pers.solid.mishang.uc.util.RoadConnectionState;

import java.util.List;

public class RoadBlock extends AbstractRoadBlock {
  private final ResourceLocation texture;

  public RoadBlock(Properties settings, ResourceLocation texture, LineColor lineColor) {
    super(settings, lineColor, LineType.NORMAL);
    this.texture = texture;
  }

  @Override
  public RoadConnectionState getConnectionStateOf(BlockState state, Direction direction) {
    return RoadConnectionState.empty();
  }

  @Override
  protected <B extends Block & Road> void registerBaseOrSlabModels(B road, BlockStateModelGenerator blockStateModelGenerator) {
    final TextureMap textures = TextureMap.all(texture);
    final ResourceLocation modelId;
    if (road instanceof SlabBlock) {
      modelId = Models.SLAB.upload(road, textures, blockStateModelGenerator.modelCollector);
      Models.SLAB_TOP.upload(road, textures, blockStateModelGenerator.modelCollector);
    } else {
      modelId = Models.CUBE_ALL.upload(road, textures, blockStateModelGenerator.modelCollector);
    }
    blockStateModelGenerator.blockStateCollector.accept(road.composeState(BlockStateModelGenerator.createBlockStateWithRandomHorizontalRotations(road, modelId)));
  }

  @Override
  public void appendDescriptionTooltip(List<Component> tooltip, TooltipFlag options) {

  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    if (lineColor != LineColor.NONE) return null;
    return ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, this, 9)
        .pattern("***")
        .pattern("|X|")
        .pattern("***")
        .define('*', ItemTags.COALS)
        .define('|', Items.FLINT)
        .define('X', Ingredient.of(Items.WHITE_CONCRETE, Items.GRAY_CONCRETE, Items.LIGHT_GRAY_CONCRETE, Items.BLACK_CONCRETE))
        .unlockedBy("has_coal", FabricRecipeProvider.has(ItemTags.COALS))
        .unlockedBy(FabricRecipeProvider.getHasName(Items.FLINT), FabricRecipeProvider.has(Items.FLINT))
        .unlockedBy("has_proper_concrete", FabricRecipeProvider.conditionsFromItemPredicates(ItemPredicate.Builder.item().of(Items.WHITE_CONCRETE, Items.GRAY_CONCRETE, Items.LIGHT_GRAY_CONCRETE, Items.BLACK_CONCRETE).build()));
  }

  @Override
  public RecipeBuilder getPaintingRecipe(Block base, Block self) {
    if (lineColor == LineColor.NONE) return null;
    return ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, self)
        .pattern("***")
        .pattern(" X ")
        .define('*', lineColor.getIngredient())
        .define('X', base)
        .unlockedBy("has_paint", FabricRecipeProvider.has(lineColor.getIngredient()))
        .unlockedBy(FabricRecipeProvider.getHasName(base), FabricRecipeProvider.has(base));
  }
}
