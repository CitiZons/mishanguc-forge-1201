package pers.solid.mishang.uc.item;

import com.google.common.collect.Collections2;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.blockentity.StandingSignBlockEntity;
import pers.solid.mishang.uc.text.TextContext;
import pers.solid.mishang.uc.util.TextBridge;

import java.util.List;
import java.util.stream.Stream;

public class StandingSignBlockItem extends NamedBlockItem {
  public StandingSignBlockItem(Block block, Properties settings) {
    super(block, settings);
  }

  protected static @NotNull Stream<TextContext> getTextContextsFromNbt(@Nullable Tag nbt) {
    if (nbt == null) return Stream.empty();
    else if (nbt instanceof ListTag nbtList) {
      return nbtList.stream().map(nbt1 -> TextContext.fromNbt(nbt1, StandingSignBlockEntity.DEFAULT_TEXT_CONTEXT.clone()));
    } else {
      return Stream.of(TextContext.fromNbt(nbt, StandingSignBlockEntity.DEFAULT_TEXT_CONTEXT.clone()));
    }
  }

  @Override
  public void appendHoverText(
      ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    super.appendHoverText(stack, world, tooltip, context);
    final CompoundTag nbt = stack.getTagElement("BlockEntityTag");
    if (nbt == null) return;
    final List<TextContext> frontTexts = getTextContextsFromNbt(nbt.get("frontTexts")).toList();
    if (!frontTexts.isEmpty()) {
      tooltip.add(TextBridge.translatable("block.mishanguc.tooltip.standing_sign_block_front").withStyle(ChatFormatting.GRAY));
      tooltip.addAll(Collections2.transform(frontTexts, TextContext::asStyledText));
    }
    final List<TextContext> backTexts = getTextContextsFromNbt(nbt.get("backTexts")).toList();
    if (!backTexts.isEmpty()) {
      tooltip.add(TextBridge.translatable("block.mishanguc.tooltip.standing_sign_block_back").withStyle(ChatFormatting.GRAY));
      tooltip.addAll(Collections2.transform(backTexts, TextContext::asStyledText));
    }
  }

  @Override
  public Component getName(ItemStack stack) {
    final CompoundTag nbt = stack.getTagElement("BlockEntityTag");
    if (nbt == null) return super.getName(stack);
    final MutableComponent text = super.getName(stack).copy();
    final List<MutableComponent> texts = Stream.concat(getTextContextsFromNbt(nbt.get("frontTexts")), getTextContextsFromNbt(nbt.get("backTexts"))).map(TextContext::asStyledText).limit(20).toList();
    if (!texts.isEmpty()) {
      MutableComponent appendable = TextBridge.empty();
      texts.forEach(t -> appendable.append(" ").append(t));
      text.append(
          TextBridge.literal(" -" + appendable.getString(25)).withStyle(ChatFormatting.GRAY));
    }
    return text;
  }
}
