package pers.solid.mishang.uc.item;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.mishang.uc.blockentity.HungSignBlockEntity;
import pers.solid.mishang.uc.text.TextContext;
import pers.solid.mishang.uc.util.TextBridge;

import java.util.List;
import java.util.Map;

/**
 * 类似于一般的方块物品，但是会读取 BlockEntityTag 中的内容来显示文字。
 *
 * @see pers.solid.mishang.uc.blockentity.HungSignBlockEntity#load(CompoundTag)
 */
public class HungSignBlockItem extends NamedBlockItem {
  public HungSignBlockItem(Block block, Properties settings) {
    super(block, settings);
  }

  /**
   * 根据 nbt 数据返回对应的 {@code Map<}{@code Direction, List<}{@code TextContext>>}。
   */
  protected static @Unmodifiable Map<Direction, @Unmodifiable List<TextContext>>
  getTextContextMapFromNbt(@NotNull CompoundTag nbt) {
    ImmutableMap.Builder<Direction, List<TextContext>> builder = new ImmutableMap.Builder<>();
    for (Direction direction : Direction.Plane.HORIZONTAL) {
      final Tag element = nbt.get(direction.getSerializedName());
      if (element == null) continue;
      if (element instanceof ListTag) {
        ImmutableList.Builder<TextContext> listBuilder = new ImmutableList.Builder<>();
        for (Tag nbtElement : ((ListTag) element)) {
          TextContext textContext = TextContext.fromNbt(nbtElement, HungSignBlockEntity.DEFAULT_TEXT_CONTEXT.clone());
          listBuilder.add(textContext);
        }
        final ImmutableList<TextContext> build = listBuilder.build();
        if (!build.isEmpty()) builder.put(direction, build);
      } else {
        builder.put(
            direction,
            ImmutableList.of(
                TextContext.fromNbt(element, HungSignBlockEntity.DEFAULT_TEXT_CONTEXT.clone())));
      }
    }
    return builder.build();
  }

  @Override
  public void appendHoverText(
      ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    super.appendHoverText(stack, world, tooltip, context);
    final CompoundTag nbt = stack.getTag();
    if (nbt == null) return;
    final Map<Direction, List<TextContext>> map =
        getTextContextMapFromNbt(nbt.getCompound("BlockEntityTag"));
    map.forEach(
        (direction, textContexts) -> {
          tooltip.add(
              TextBridge.translatable("block.mishanguc.tooltip.hung_sign_block", TextBridge.translatable("direction." + direction.getSerializedName()))
                  .withStyle(ChatFormatting.GRAY));
          textContexts.forEach(
              textContext -> {
                final MutableComponent mutableText = textContext.asStyledText();
                tooltip.add(mutableText);
              });
        });
  }

  @Override
  public Component getName(ItemStack stack) {
    final MutableComponent text = super.getName(stack).copy();
    final CompoundTag nbt = stack.getTag();
    if (nbt == null) return text;
    final ImmutableList.Builder<Component> appendable = new ImmutableList.Builder<>();
    final Map<Direction, List<TextContext>> map =
        getTextContextMapFromNbt(nbt.getCompound("BlockEntityTag"));
    map.forEach(
        (direction, textContexts) ->
            textContexts.forEach(
                textContext -> {
                  final MutableComponent styledText = textContext.asStyledText();
                  appendable.add(styledText);
                }));
    final ImmutableList<Component> build = appendable.build();
    if (!build.isEmpty()) {
      final MutableComponent appendableText = TextBridge.literal("");
      build.forEach(t -> appendableText.append(" ").append(t));
      text.append(
          TextBridge.literal(" -" + appendableText.getString(20)).withStyle(ChatFormatting.GRAY));
    }
    return text;
  }
}
