package pers.solid.mishang.uc.item;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.mishang.uc.blockentity.WallSignBlockEntity;
import pers.solid.mishang.uc.text.TextContext;
import pers.solid.mishang.uc.util.TextBridge;

import java.util.List;

/**
 * 类似于一般的方块物品，但是会读取 BlockEntityTag 中的内容来显示文字。
 *
 * @see pers.solid.mishang.uc.blockentity.WallSignBlockEntity#load(CompoundTag)
 */
public class WallSignBlockItem extends NamedBlockItem {
  public WallSignBlockItem(Block block, Properties settings) {
    super(block, settings);
  }

  /**
   * 根据 nbt 数据返回文本内容。
   *
   * @param nbt 物品的 nbt 数据，通常为 {@code BlockEntityTag} 的值。
   * @return 该 nbt 对应的 {@code List<}{@code TextContext>}。
   */
  protected static @NotNull @Unmodifiable List<TextContext> getTextContextsFromNbt(
      @NotNull CompoundTag nbt) {
    final Tag nbtText = nbt.get("text");
    if (nbtText instanceof StringTag) {
      return ImmutableList.of(TextContext.fromNbt(nbt, WallSignBlockEntity.DEFAULT_TEXT_CONTEXT.clone()));
    } else if (nbtText instanceof CompoundTag) {
      return ImmutableList.of(
          TextContext.fromNbt(nbtText, WallSignBlockEntity.DEFAULT_TEXT_CONTEXT.clone()));
    } else if (nbtText instanceof ListTag) {
      ImmutableList.Builder<TextContext> builder = new ImmutableList.Builder<>();
      for (Tag nbtElement : ((ListTag) nbtText)) {
        builder.add(TextContext.fromNbt(nbtElement, WallSignBlockEntity.DEFAULT_TEXT_CONTEXT.clone()));
      }
      return builder.build();
    }
    return ImmutableList.of();
  }

  @Override
  public void appendHoverText(
      ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    super.appendHoverText(stack, world, tooltip, context);
    final CompoundTag nbt = stack.getTagElement("BlockEntityTag");
    if (nbt == null) return;
    final List<MutableComponent> texts = ImmutableList.copyOf(
        getTextContextsFromNbt(nbt).stream()
            .map(TextContext::asStyledText)
            .iterator());
    if (!texts.isEmpty()) {
      tooltip.add(
          TextBridge.translatable("block.mishanguc.tooltip.wall_sign_block")
              .withStyle(ChatFormatting.GRAY));
      tooltip.addAll(texts);
    }
  }

  @Override
  public Component getName(ItemStack stack) {
    final CompoundTag nbt = stack.getTagElement("BlockEntityTag");
    if (nbt == null) return super.getName(stack);
    final MutableComponent text = super.getName(stack).copy();
    final List<MutableComponent> texts = ImmutableList.copyOf(
        getTextContextsFromNbt(nbt).stream()
            .map(TextContext::asStyledText)
            .limit(20)
            .iterator());
    if (!texts.isEmpty()) {
      MutableComponent appendable = TextBridge.empty();
      texts.forEach(t -> appendable.append(" ").append(t));
      text.append(
          TextBridge.literal(" -" + appendable.getString(25)).withStyle(ChatFormatting.GRAY));
    }
    return text;
  }
}
