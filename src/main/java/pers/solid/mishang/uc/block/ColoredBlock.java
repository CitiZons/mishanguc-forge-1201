package pers.solid.mishang.uc.block;

import com.mojang.datafixers.util.Function3;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.blockentity.ColoredBlockEntity;
import pers.solid.mishang.uc.util.TextBridge;

import java.awt.*;
import java.util.List;

/**
 * <p>所有带有颜色的方块应有的接口。其对应的方块实体应该实现 {@link pers.solid.mishang.uc.blockentity.ColoredBlockEntity}。
 * <p>在 {@link pers.solid.mishang.uc.MishangucClient} 中，本模组中所有实现该接口的方块都会为其自身以及方块物品注册颜色提供器。
 */
public interface ColoredBlock extends EntityBlock, MishangucBlock {

  LootItemFunction.Builder COPY_COLOR_LOOT_FUNCTION = CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY).copy("color", "BlockEntityTag.color");

  /**
   * 给方块添加关于颜色的提示。
   *
   * @see Block#appendHoverText(ItemStack, BlockGetter, List, TooltipFlag)
   */
  static void appendColorTooltip(ItemStack stack, List<Component> tooltip) {
    final CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
    if (blockEntityTag != null && blockEntityTag.contains("color")) {
      // 此时该对象已经定义了颜色。
      final int color = MishangUtils.readColorFromNbtElement(blockEntityTag.get("color"));
      Color colorObject = new Color(color);
      tooltip.add(TextBridge.translatable("block.mishanguc.colored_block.tooltip.color", MishangUtils.describeColor(color)).withStyle(ChatFormatting.GRAY));
      tooltip.add(TextBridge.translatable("block.mishanguc.colored_block.tooltip.color_components", colorObject.getRed(), colorObject.getGreen(), colorObject.getBlue(), colorObject.getAlpha()).withStyle(ChatFormatting.GRAY));
    } else {
      // 没有定义颜色的情况。
      tooltip.add(TextBridge.translatable("block.mishanguc.colored_block.tooltip.auto_color").withStyle(ChatFormatting.GRAY));
      tooltip.add(TextBridge.translatable("block.mishanguc.colored_block.tooltip.auto_color2").withStyle(ChatFormatting.GRAY));
    }
  }

  /**
   * 子类在覆盖 {@link net.minecraft.block.Block#getCloneItemStack(BlockGetter, BlockPos, BlockState)} 时，可以这么写（下列代码使用yarn映射）：
   * <pre>{@code
   *     return getColoredPickStack(world, pos, state, super::getCloneItemStack);}</pre>
   */
  default ItemStack getColoredPickStack(BlockGetter world, BlockPos pos, BlockState state, Function3<BlockGetter, BlockPos, BlockState, ItemStack> superGetPickStack) {
    final ItemStack stack = superGetPickStack.apply(world, pos, state);
    final BlockEntity blockEntity = world.getBlockEntity(pos);
    if (blockEntity instanceof ColoredBlockEntity coloredBlockEntity) {
      stack.getOrCreateTagElement("BlockEntityTag").putInt("color", coloredBlockEntity.getColor());
    }
    return stack;
  }

  Object2ObjectMap<Block, Block> BASE_TO_COLORED = new Object2ObjectOpenHashMap<>();
  Object2ObjectMap<TagKey<Block>, Block> BASE_TAG_TO_COLORED = new Object2ObjectOpenHashMap<>();

  @Override
  default String customRecipeCategory() {
    return "colored_blocks";
  }
}
