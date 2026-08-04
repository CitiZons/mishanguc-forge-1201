package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricBlockLootTableProvider;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
// TODO: Forge data gen - BlockStateModelGenerator
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import pers.solid.mishang.uc.data.stubs.Models;
import pers.solid.mishang.uc.data.stubs.TextureMap;
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

import java.util.List;
import java.util.function.BiFunction;

@ApiStatus.AvailableSince("0.2.4")
public class ColoredLeavesBlock extends LeavesBlock implements ColoredBlock {
  private final BiFunction<Block, FabricBlockLootTableProvider, LootTable.Builder> lootBuilder;
  private final ResourceLocation texture;

  public ColoredLeavesBlock(Properties settings, @Nullable BiFunction<Block, FabricBlockLootTableProvider, LootTable.Builder> lootBuilder, ResourceLocation texture) {
    super(settings);
    this.lootBuilder = lootBuilder;
    this.texture = texture;
  }

  public ColoredLeavesBlock(Properties settings, @Nullable BiFunction<Block, FabricBlockLootTableProvider, LootTable.Builder> lootBuilder, String texture) {
    this(settings, lootBuilder, new ResourceLocation(texture));
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
    final ResourceLocation modelId = Models.LEAVES.upload(this, TextureMap.all(texture), blockStateModelGenerator.modelCollector);
    blockStateModelGenerator.blockStateCollector.accept(BlockStateModelGenerator.createSingletonBlockState(this, modelId));
    blockStateModelGenerator.registerParentedItemModel(this, modelId);
  }

  @Override
  public LootTable.Builder getLootTable(FabricBlockLootTableProvider blockLootTableGenerator) {
    if (lootBuilder == null) return null;
    return (lootBuilder.apply(this, blockLootTableGenerator).apply(COPY_COLOR_LOOT_FUNCTION));
  }
}
