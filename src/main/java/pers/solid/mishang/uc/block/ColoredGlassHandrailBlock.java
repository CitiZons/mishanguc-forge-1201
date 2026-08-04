package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricBlockLootTableProvider;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.blockentity.SimpleColoredBlockEntity;

import java.util.List;
import net.minecraft.world.level.block.StairBlock;

public class ColoredGlassHandrailBlock extends GlassHandrailBlock implements ColoredBlock {
  public ColoredGlassHandrailBlock(Block baseBlock, Properties settings, String frameTexture, String decorationTexture) {
    super(baseBlock, settings, frameTexture, decorationTexture, ColoredCentral::new, ColoredCorner::new, ColoredStair::new, ColoredOuter::new);
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
    return blockLootTableGenerator.drops(this).apply(COPY_COLOR_LOOT_FUNCTION);
  }

  @Override
  public String customRecipeCategory() {
    return "handrails";
  }

  public static class ColoredCentral extends CentralBlock implements ColoredBlock {

    protected ColoredCentral(@NotNull GlassHandrailBlock baseRail) {
      super(baseRail);
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
      return blockLootTableGenerator.drops(this).apply(COPY_COLOR_LOOT_FUNCTION);
    }

    @Override
    public String customRecipeCategory() {
      return "handrails";
    }
  }

  public static class ColoredCorner extends CornerBlock implements ColoredBlock {

    protected ColoredCorner(@NotNull GlassHandrailBlock baseRail) {
      super(baseRail);
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
      return blockLootTableGenerator.drops(this, ConstantValue.exactly(2)).apply(COPY_COLOR_LOOT_FUNCTION);
    }

    @Override
    public String customRecipeCategory() {
      return "handrails";
    }
  }

  public static class ColoredOuter extends OuterBlock implements ColoredBlock {

    protected ColoredOuter(@NotNull GlassHandrailBlock baseRail) {
      super(baseRail);
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
      return blockLootTableGenerator.drops(this).apply(COPY_COLOR_LOOT_FUNCTION);
    }

    @Override
    public String customRecipeCategory() {
      return "handrails";
    }
  }

  public static class ColoredStair extends StairBlock implements ColoredBlock {

    protected ColoredStair(@NotNull GlassHandrailBlock baseRail) {
      super(baseRail);
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
      return blockLootTableGenerator.drops(this).apply(COPY_COLOR_LOOT_FUNCTION);
    }

    @Override
    public String customRecipeCategory() {
      return "handrails";
    }
  }
}
