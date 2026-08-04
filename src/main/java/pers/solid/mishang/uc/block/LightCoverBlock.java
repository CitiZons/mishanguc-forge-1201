package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.NotNull;
import pers.solid.mishang.uc.MishangUtils;

import java.util.Map;

public class LightCoverBlock extends WallLightBlock {
  private static final Map<Direction, VoxelShape> SHAPE_PER_DIRECTION = MishangUtils.createDirectionToShape(0, 0, 0, 16, 1, 16);

  public LightCoverBlock(String lightColor, Properties settings) {
    super(lightColor, settings, true);
    registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH));
  }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    return SHAPE_PER_DIRECTION.get(state.getValue(FACING));
  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    final ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(asItem());
    final @NotNull Item fullLight = getBaseLight(itemId.getNamespace(), lightColor, this);
    return SingleItemRecipeBuilder.stonecutting(Ingredient.of(fullLight), RecipeCategory.DECORATIONS, this, 8)
        .unlockedBy(FabricRecipeProvider.getHasName(fullLight), FabricRecipeProvider.has(fullLight));
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {
    return type == PathComputationType.WATER && state.getFluidState().is(FluidTags.WATER);
  }
}
