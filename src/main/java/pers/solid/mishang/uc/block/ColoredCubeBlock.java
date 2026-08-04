package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricBlockLootTableProvider;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.*;
import pers.solid.mishang.uc.data.stubs.TextureKey;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
import pers.solid.mishang.uc.data.stubs.Model;
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.blockentity.SimpleColoredBlockEntity;
import pers.solid.mishang.uc.blocks.ColoredBlocks;
import pers.solid.mishang.uc.data.MishangucModels;

import java.util.List;

public class ColoredCubeBlock extends Block implements ColoredBlock {
  protected final Model model;
  protected final TextureMap textures;
  public static final Model COLORED_CUBE_ALL = MishangucModels.createBlock("colored_cube_all", TextureKey.ALL);
  public static final Model COLORED_CUBE_BOTTOM_TOP = MishangucModels.createBlock("colored_cube_bottom_top", TextureKey.TOP, TextureKey.BOTTOM, TextureKey.SIDE);
  public static final Model COLORED_CUBE_MIRRORED_ALL = MishangucModels.createBlock("colored_cube_mirrored_all", "_mirrored", TextureKey.ALL);
  public static final Model COLORED_CUBE_ALL_WITHOUT_SHADE = MishangucModels.createBlock("colored_cube_all_without_shade", TextureKey.ALL);

  @ApiStatus.Internal
  public ColoredCubeBlock(Properties settings, Model model, TextureMap textures) {
    super(settings);
    this.model = model;
    this.textures = textures;
  }

  public static ColoredCubeBlock cubeAll(Properties settings, String allTexture) {
    return new ColoredCubeBlock(settings, COLORED_CUBE_ALL, TextureMap.all(new ResourceLocation(allTexture)));
  }

  public static ColoredCubeBlock cubeBottomTop(Properties settings, String topTexture, String sideTexture, String bottomTexture) {
    return new ColoredCubeBlock(settings, COLORED_CUBE_BOTTOM_TOP, TextureMap.of(TextureKey.TOP, new ResourceLocation(topTexture)).put(TextureKey.SIDE, new ResourceLocation(sideTexture)).put(TextureKey.BOTTOM, new ResourceLocation(bottomTexture)));
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

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new SimpleColoredBlockEntity(pos, state);
  }

  @Override
  public LootTable.Builder getLootTable(FabricBlockLootTableProvider blockLootTableGenerator) {
    if (this == ColoredBlocks.COLORED_PACKED_ICE) {
      return FabricBlockLootTableProvider.createSilkTouchOnlyTable(this).apply(COPY_COLOR_LOOT_FUNCTION);
    } else if (this == ColoredBlocks.COLORED_STONE) {
      return blockLootTableGenerator.drops(this, ColoredBlocks.COLORED_COBBLESTONE).apply(COPY_COLOR_LOOT_FUNCTION);
    }
    return blockLootTableGenerator.drops(this).apply(COPY_COLOR_LOOT_FUNCTION);
  }

  @Override
  public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    if (this == ColoredBlocks.COLORED_STONE) {
      final ResourceLocation modelId = ColoredCubeBlock.COLORED_CUBE_ALL.upload(this, textures, blockStateModelGenerator.modelCollector);
      final ResourceLocation mirroredModelId = ColoredCubeBlock.COLORED_CUBE_MIRRORED_ALL.upload(this, textures, blockStateModelGenerator.modelCollector);

      blockStateModelGenerator.blockStateCollector.accept(BlockStateModelGenerator.createBlockStateWithTwoModelAndRandomInversion(this, modelId, mirroredModelId));
      return;
    }
    final ResourceLocation modelId = model.upload(this, textures, blockStateModelGenerator.modelCollector);
    blockStateModelGenerator.blockStateCollector.accept(BlockStateModelGenerator.createSingletonBlockState(this, modelId));
    blockStateModelGenerator.registerParentedItemModel(this, modelId);
  }

  @Override
  public ResourceLocation getTexture(TextureKey key) {
    return textures.getTexture(key);
  }
}
