package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricBlockLootTableProvider;

import net.minecraft.world.level.block.state.BlockBehaviour;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
// TODO: Forge data gen - BlockStateModelGenerator
import pers.solid.mishang.uc.data.stubs.ModelIds;
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.blockentity.SimpleColoredBlockEntity;
import pers.solid.mishang.uc.data.MishangucModels;

import java.util.List;

public class ColoredSlabBlock extends SlabBlock implements ColoredBlock {
  public final Block baseBlock;

  public ColoredSlabBlock(@Nullable Block baseBlock, Properties settings) {
    super(settings);
    this.baseBlock = baseBlock;
  }

  public ColoredSlabBlock(@NotNull Block baseBlock) {
    super(BlockBehaviour.Properties.copy(baseBlock));
    this.baseBlock = baseBlock;
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
    final TextureMap textures = baseBlock instanceof ColoredCubeBlock coloredCubeBlock ? coloredCubeBlock.textures : TextureMap.all(this);
    final ResourceLocation bottomModelId = MishangucModels.COLORED_SLAB.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation topModelId = MishangucModels.COLORED_SLAB_TOP.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation fullModelId;
    if (baseBlock == null) {
      fullModelId = MishangucModels.COLORED_CUBE_BOTTOM_UP.upload(this, textures, blockStateModelGenerator.modelCollector);
    } else {
      fullModelId = ModelIds.getBlockModelId(baseBlock);
    }

    blockStateModelGenerator.blockStateCollector.accept(BlockStateModelGenerator.createSlabBlockState(this, bottomModelId, topModelId, fullModelId));
    blockStateModelGenerator.registerParentedItemModel(this, bottomModelId);
  }

  @Override
  public LootTable.Builder getLootTable(FabricBlockLootTableProvider blockLootTableGenerator) {
    return blockLootTableGenerator.createSlabItemTable(this).apply(COPY_COLOR_LOOT_FUNCTION);
  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    return ((ShapedRecipeBuilder) FabricRecipeProvider.slabBuilder(RecipeCategory.BUILDING_BLOCKS, this, Ingredient.of(baseBlock)))
        .unlockedBy(FabricRecipeProvider.getHasName(this.baseBlock), FabricRecipeProvider.has(this.baseBlock));
  }
}
