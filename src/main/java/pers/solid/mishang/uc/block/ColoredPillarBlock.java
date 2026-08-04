package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricBlockLootTableProvider;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
// TODO: Forge data gen - BlockStateModelGenerator
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.blockentity.SimpleColoredBlockEntity;
import pers.solid.mishang.uc.data.MishangucModels;

import java.util.List;

public class ColoredPillarBlock extends RotatedPillarBlock implements ColoredBlock {
  private final TextureMap textures;

  public ColoredPillarBlock(Properties settings, TextureMap textures) {
    super(settings);
    this.textures = textures;
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
  public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    final ResourceLocation modelId = MishangucModels.COLORED_CUBE_COLUMN.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation horizontalModelId = MishangucModels.COLORED_CUBE_COLUMN_HORITONZAL.upload(this, textures, blockStateModelGenerator.modelCollector);
    blockStateModelGenerator.blockStateCollector.accept(BlockStateModelGenerator.createAxisRotatedBlockState(this, modelId, horizontalModelId));
    blockStateModelGenerator.registerParentedItemModel(this, modelId);
  }

  @Override
  public LootTable.Builder getLootTable(FabricBlockLootTableProvider blockLootTableGenerator) {
    return blockLootTableGenerator.drops(this).apply(COPY_COLOR_LOOT_FUNCTION);
  }
}
