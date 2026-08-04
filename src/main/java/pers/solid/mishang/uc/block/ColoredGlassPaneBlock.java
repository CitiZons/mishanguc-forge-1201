package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricBlockLootTableProvider;

import pers.solid.mishang.uc.data.stubs.When;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.*;
import pers.solid.mishang.uc.data.stubs.TextureKey;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
import pers.solid.mishang.uc.data.stubs.BlockStateSupplier;
import pers.solid.mishang.uc.data.stubs.VariantSettings;
import pers.solid.mishang.uc.data.stubs.Model;
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.blockentity.SimpleColoredBlockEntity;
import pers.solid.mishang.uc.data.MishangucModels;

import java.util.List;

public class ColoredGlassPaneBlock extends IronBarsBlock implements ColoredBlock {
  private final ResourceLocation paneTexture;
  private final ResourceLocation edgeTexture;

  public ColoredGlassPaneBlock(ResourceLocation paneTexture, ResourceLocation edgeTexture, Properties settings) {
    super(settings);
    this.paneTexture = paneTexture;
    this.edgeTexture = edgeTexture;
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
    TextureMap textures = TextureMap.of(TextureKey.PANE, paneTexture).put(TextureKey.EDGE, edgeTexture);
    final ResourceLocation postId = MishangucModels.TEMPLATE_COLORED_GLASS_PANE_POST.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation sideId = MishangucModels.TEMPLATE_COLORED_GLASS_PANE_SIDE.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation SideAltId = MishangucModels.TEMPLATE_COLORED_GLASS_PANE_SIDE_ALT.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation nosideId = MishangucModels.TEMPLATE_COLORED_GLASS_PANE_NOSIDE.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation nosideAltId = MishangucModels.TEMPLATE_COLORED_GLASS_PANE_NOSIDE_ALT.upload(this, textures, blockStateModelGenerator.modelCollector);

    blockStateModelGenerator.blockStateCollector.accept(createBlockStates(postId, sideId, SideAltId, nosideId, nosideAltId));
    Models.GENERATED.upload(ModelIds.getItemModelId(asItem()), TextureMap.layer0(paneTexture), blockStateModelGenerator.modelCollector);
  }

  public @NotNull BlockStateSupplier createBlockStates(ResourceLocation postId, ResourceLocation sideId, ResourceLocation sideAltId, ResourceLocation nosideId, ResourceLocation nosideAltId) {
    return MultipartBlockStateSupplier.create(this)
        .setValue(BlockStateVariant.create()
            .put(VariantSettings.MODEL, postId))
        .setValue(When.create().set(BlockStateProperties.NORTH, true),
            BlockStateVariant.create()
                .put(VariantSettings.MODEL, sideId))
        .setValue(When.create().set(BlockStateProperties.EAST, true),
            BlockStateVariant.create()
                .put(VariantSettings.MODEL, sideId)
                .put(VariantSettings.Y, VariantSettings.Rotation.R90))
        .setValue(When.create().set(BlockStateProperties.SOUTH, true),
            BlockStateVariant.create()
                .put(VariantSettings.MODEL, sideAltId))
        .setValue(When.create().set(BlockStateProperties.WEST, true),
            BlockStateVariant.create()
                .put(VariantSettings.MODEL, sideAltId)
                .put(VariantSettings.Y, VariantSettings.Rotation.R90))
        .setValue(When.create().set(BlockStateProperties.NORTH, false),
            BlockStateVariant.create()
                .put(VariantSettings.MODEL, nosideId))
        .setValue(When.create().set(BlockStateProperties.EAST, false),
            BlockStateVariant.create()
                .put(VariantSettings.MODEL, nosideAltId))
        .setValue(When.create().set(BlockStateProperties.SOUTH, false),
            BlockStateVariant.create()
                .put(VariantSettings.MODEL, nosideAltId)
                .put(VariantSettings.Y, VariantSettings.Rotation.R90))
        .setValue(When.create().set(BlockStateProperties.WEST, false),
            BlockStateVariant.create()
                .put(VariantSettings.MODEL, nosideId)
                .put(VariantSettings.Y, VariantSettings.Rotation.R270));
  }

  @Override
  public LootTable.@NotNull Builder getLootTable(FabricBlockLootTableProvider blockLootTableGenerator) {
    return FabricBlockLootTableProvider.createSilkTouchOnlyTable(this).apply(COPY_COLOR_LOOT_FUNCTION);
  }
}
