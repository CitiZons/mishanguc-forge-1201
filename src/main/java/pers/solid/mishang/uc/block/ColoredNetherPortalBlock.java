package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricBlockLootTableProvider;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.*;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
import pers.solid.mishang.uc.data.stubs.VariantSettings;
import pers.solid.mishang.uc.data.stubs.Model;
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.blockentity.SimpleColoredBlockEntity;

import java.util.List;
import com.mojang.math.Axis;

@ApiStatus.AvailableSince("1.0.2")
public class ColoredNetherPortalBlock extends NetherPortalBlock implements ColoredBlock {
  public ColoredNetherPortalBlock(Properties settings) {
    super(settings);
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext ctx) {
    final BlockState state = super.getStateForPlacement(ctx);
    return state == null ? null : state.setValue(AXIS, ctx.getHorizontalDirection().getClockWise().getAxis());
  }

  @Override
  public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
    return getColoredPickStack(world, pos, state, (blockView, pos1, state1) -> new ItemStack(this));
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
    final ResourceLocation ewId = ModelIds.getBlockSubModelId(this, "_ew");
    final ResourceLocation nsId = ModelIds.getBlockSubModelId(this, "_ns");
    blockStateModelGenerator.blockStateCollector.accept(VariantsBlockStateSupplier.create(this)
        .coordinate(
            BlockStateVariantMap.create(BlockStateProperties.HORIZONTAL_AXIS)
                .register(Direction.Axis.X, BlockStateVariant.create().put(VariantSettings.MODEL, nsId))
                .register(Direction.Axis.Z, BlockStateVariant.create().put(VariantSettings.MODEL, ewId))));
    blockStateModelGenerator.registerParentedItemModel(this, nsId);
  }

  @Override
  public LootTable.Builder getLootTable(FabricBlockLootTableProvider blockLootTableGenerator) {
    return LootTable.lootTable();
  }
}
