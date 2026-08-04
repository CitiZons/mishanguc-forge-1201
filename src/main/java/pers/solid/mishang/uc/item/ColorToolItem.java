package pers.solid.mishang.uc.item;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.block.ColoredBlock;
import pers.solid.mishang.uc.blockentity.ColoredBlockEntity;
import pers.solid.mishang.uc.util.TextBridge;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class ColorToolItem extends BlockToolItem implements MishangucItem {
  public ColorToolItem(Properties settings, @Nullable Boolean includesFluid) {
    super(settings, includesFluid);
  }

  @Override
  public Component getName(ItemStack stack) {
    final CompoundTag nbt = stack.getTag();
    if (nbt != null && nbt.contains("color")) {
      final int color = MishangUtils.readColorFromNbtElement(nbt.get("color"));
      return TextBridge.translatable("block.mishanguc.colored_block.color", super.getName(stack), MishangUtils.describeColor(color));
    } else {
      return super.getName(stack);
    }
  }

  @Override
  public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    final CompoundTag nbt = stack.getTag();
    tooltip.add(TextBridge.translatable("item.mishanguc.color_tool.tooltip.1", TextBridge.keybind("key.attack").withStyle(style -> style.withColor(0xdddddd))).withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.color_tool.tooltip.2", TextBridge.keybind("key.use").withStyle(style -> style.withColor(0xdddddd))).withStyle(ChatFormatting.GRAY));
    if (nbt != null && nbt.contains("color")) {
      // 此时该对象已经定义了颜色。
      final int color = MishangUtils.readColorFromNbtElement(nbt.get("color"));
      Color colorObject = new Color(color);
      tooltip.add(TextBridge.translatable("block.mishanguc.colored_block.tooltip.color",
          MishangUtils.describeColor(color)
      ).withStyle(ChatFormatting.GRAY));
      tooltip.add(TextBridge.translatable("block.mishanguc.colored_block.tooltip.color_components", colorObject.getRed(), colorObject.getGreen(), colorObject.getBlue(), colorObject.getAlpha()).withStyle(ChatFormatting.GRAY));
    }
  }

  @Override
  public InteractionResult useOnBlock(ItemStack stack, Player player, Level world, BlockHitResult blockHitResult, InteractionHand hand, boolean fluidIncluded) {
    final BlockPos blockPos = blockHitResult.getBlockPos();
    BlockEntity blockEntity = world.getBlockEntity(blockPos);
    final CompoundTag nbt = stack.getTag();
    if (nbt == null || !nbt.contains("color")) {
      if (!world.isClientSide) {
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.color_tool.message.no_data").withStyle(ChatFormatting.RED), true);
        return InteractionResult.FAIL;
      }
      return InteractionResult.PASS;
    }
    if (!(blockEntity instanceof ColoredBlockEntity)) {
      final BlockState blockState = world.getBlockState(blockPos);
      final Block block = blockState.getBlock();
      final Block coloredBlock;
      if (ColoredBlock.BASE_TO_COLORED.containsKey(block)) {
        coloredBlock = ColoredBlock.BASE_TO_COLORED.get(block);
      } else {
        coloredBlock = ColoredBlock.BASE_TAG_TO_COLORED.entrySet().stream()
            .filter(entry -> blockState.is(entry.getKey()))
            .findAny()
            .map(Map.Entry::getValue)
            .orElse(null);
      }

      if (coloredBlock != null) {
        final BlockState coloredState = MishangUtils.getStateWithProperties(coloredBlock, blockState);
        world.setBlockAndUpdate(blockPos, coloredState);
        final BlockEntity oldBlockEntity = blockEntity;
        blockEntity = world.getBlockEntity(blockPos);
        if (oldBlockEntity != null && blockEntity != null) {
          blockEntity.load(oldBlockEntity.saveWithFullMetadata());
        }
      }
    }
    if (blockEntity instanceof ColoredBlockEntity coloredBlockEntity) {
      final int color = MishangUtils.readColorFromNbtElement(nbt.get("color"));
      coloredBlockEntity.setColor(color);
      blockEntity.setChanged();
      world.sendBlockUpdated(blockPos, blockEntity.getBlockState(), blockEntity.getBlockState(), Block.UPDATE_CLIENTS);
      world.playSound(null, blockPos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
      if (!world.isClientSide) {
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.color_tool.message.success_set", MishangUtils.describeColor(color)), true);
      }
      stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
      return InteractionResult.sidedSuccess(world.isClientSide);
    } else {
      if (!world.isClientSide) {
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.color_tool.message.not_colored").withStyle(ChatFormatting.RED), true);
        return InteractionResult.FAIL;
      }
      return InteractionResult.PASS;
    }
  }

  @Override
  public InteractionResult beginAttackBlock(ItemStack stack, Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction, boolean fluidIncluded) {
    final BlockState blockState = world.getBlockState(pos);
    final BlockEntity blockEntity = world.getBlockEntity(pos);
    final int color;
    if (blockEntity instanceof ColoredBlockEntity coloredBlockEntity) {
      stack.getOrCreateTag().putInt("color", color = coloredBlockEntity.getColor());
    } else {
      stack.getOrCreateTag().putInt("color", color = blockState.getMapColor(world, pos).col);
    }
    if (!world.isClientSide) {
      player.displayClientMessage(TextBridge.translatable("item.mishanguc.color_tool.message.success_copied", MishangUtils.describeColor(color)), true);
    }
    return null;
  }
}
