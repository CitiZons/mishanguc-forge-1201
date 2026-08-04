package pers.solid.mishang.uc.item;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.block.HungSignBlock;
import pers.solid.mishang.uc.block.StandingSignBlock;
import pers.solid.mishang.uc.blockentity.HungSignBlockEntity;
import pers.solid.mishang.uc.blockentity.StandingSignBlockEntity;
import pers.solid.mishang.uc.blockentity.WallSignBlockEntity;
import pers.solid.mishang.uc.text.TextContext;
import pers.solid.mishang.uc.util.RoadConnectionState;
import pers.solid.mishang.uc.util.TextBridge;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import net.minecraft.network.chat.contents.LiteralContents;
import pers.solid.mishang.uc.data.stubs.WorldRenderContext;
import com.mojang.math.Axis;

/**
 * 用于复制粘贴文本的工具。持有该工具，“攻击”（默认左键）告示牌（含原版告示牌、悬挂告示牌和墙上的告示牌）可以将文本复制到物品中，"使用"（默认右键）告示牌可将文本粘贴上去。
 */
public class TextCopyToolItem extends BlockToolItem implements MishangucItem {
  // 1.18.1 之前用 apache 的 Logger，自 1.18.2 用 slf4j 的 Logger。
  public static final Logger LOGGER = LoggerFactory.getLogger(TextCopyToolItem.class);

  public TextCopyToolItem(Properties settings, @Nullable Boolean includesFluid) {
    super(settings, includesFluid);
  }

  @Override
  public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    super.appendHoverText(stack, world, tooltip, context);
    tooltip.add(TextBridge.translatable("item.mishanguc.text_copy_tool.tooltip.1", TextBridge.keybind("key.attack").withStyle(style -> style.withColor(0xdddddd))).withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.text_copy_tool.tooltip.2", TextBridge.keybind("key.use").withStyle(style -> style.withColor(0xdddddd))).withStyle(ChatFormatting.GRAY));
    final CompoundTag tag = stack.getTag();
    if (tag != null && tag.contains("texts", Tag.TAG_LIST)) {
      final ListTag texts = tag.getList("texts", Tag.TAG_COMPOUND);
      if (!texts.isEmpty()) {
        tooltip.add(TextBridge.translatable("item.mishanguc.text_copy_tool.tooltip.3").withStyle(ChatFormatting.GRAY));
        texts.stream().map(TextContext::fromNbt).map(TextContext::asStyledText).peek(text -> {
          final TextColor color = text.getStyle().getColor();
          if (color != null && color.getValue() == 0) {
            // 考虑黑色的文本看不清楚，因此这种情况依然显示为灰色。
            text.withStyle(ChatFormatting.GRAY);
          }
        }).forEach(tooltip::add);
      }
    }
  }

  @Override
  public Component getName(ItemStack stack) {
    final CompoundTag nbt = stack.getTag();
    if (nbt == null || !nbt.contains("texts", Tag.TAG_LIST))
      return super.getName(stack);
    final MutableComponent text = super.getName(stack).copy();
    final List<MutableComponent> texts = ImmutableList.copyOf(
        nbt.getList("texts", Tag.TAG_COMPOUND).stream()
            .map(TextContext::fromNbt)
            .map(TextContext::asStyledText)
            .iterator());
    if (!texts.isEmpty()) {
      MutableComponent appendable = TextBridge.empty();
      texts.forEach(t -> appendable.append(" ").append(t));
      text.append(
          TextBridge.literal(" -" + appendable.getString(25)).withStyle(ChatFormatting.GRAY));
    }
    return text;
  }

  @Override
  public InteractionResult useOnBlock(ItemStack stack, Player player, Level world, BlockHitResult blockHitResult, InteractionHand hand, boolean fluidIncluded) {
    final BlockPos blockPos = blockHitResult.getBlockPos();
    final BlockState blockState = world.getBlockState(blockPos);
    final BlockEntity blockEntity = world.getBlockEntity(blockPos);
    final ListTag texts;
    final CompoundTag tag = stack.getTag();
    if (tag == null || !tag.contains("texts", Tag.TAG_LIST)) {
      player.displayClientMessage(TextBridge.translatable("item.mishanguc.text_copy_tool.message.fail.null_tag", TextBridge.keybind("key.attack").withStyle(Style.EMPTY.withColor(0xdeb305))).withStyle(ChatFormatting.RED), true);
      return InteractionResult.FAIL;
    } else {
      texts = tag.getList("texts", Tag.TAG_COMPOUND);
    }
    try {
      if (blockEntity instanceof SignBlockEntity signBlockEntity) {
        if (world.isClientSide)
          return InteractionResult.SUCCESS;
        final SignText textFacing = signBlockEntity.getText(signBlockEntity.isFacingFrontText(player));
        final Component[] messagesUnfiltered = textFacing.getMessages(false);
        Arrays.fill(messagesUnfiltered, CommonComponents.EMPTY);
        @Nullable DyeColor color = null;
        for (int i = 0; i < texts.size(); i++) {
          final TextContext textContext = TextContext.fromNbt(texts.get(i));
          final MutableComponent styledText = textContext.asStyledText();
          if (i < messagesUnfiltered.length) {
            // 设置告示牌文字
            messagesUnfiltered[i] = styledText;

            // 设置告示牌颜色
            final DyeColor possibleColor = MishangUtils.colorBySignColor(textContext.color);
            if (possibleColor != null) {
              if (color == null) {
                color = possibleColor;
              }
            }
          } else {
            player.displayClientMessage(TextBridge.translatable("item.mishanguc.text_copy_tool.message.warn.outOfBound", styledText, messagesUnfiltered.length).withStyle(ChatFormatting.YELLOW), false);
          }
        }
        signBlockEntity.setText(color == null ? textFacing : textFacing.setColor(color), signBlockEntity.isFacingFrontText(player));
        blockEntity.setChanged();
        world.sendBlockUpdated(blockPos, blockState, blockState, 3);
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.text_copy_tool.message.success.paste", Math.min(texts.size(), 4)), true);
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        return InteractionResult.SUCCESS;
      } else if (blockEntity instanceof WallSignBlockEntity wallSignBlockEntity) {
        if (world.isClientSide)
          return InteractionResult.SUCCESS;
        wallSignBlockEntity.textContexts = ImmutableList.copyOf(texts.stream().map(nbtElement -> TextContext.fromNbt(nbtElement, wallSignBlockEntity.createDefaultTextContext())).iterator());
        if (stack.getOrCreateTag().getBoolean("fromVanillaSign")) {
          MishangUtils.rearrange(wallSignBlockEntity.textContexts);
        }
        blockEntity.setChanged();
        world.sendBlockUpdated(blockPos, blockState, blockState, 3);
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.text_copy_tool.message.success.paste", wallSignBlockEntity.textContexts.size()), true);
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        return InteractionResult.SUCCESS;
      } else if (blockEntity instanceof HungSignBlockEntity hungSignBlockEntity) {
        if (world.isClientSide)
          return InteractionResult.SUCCESS;
        final Direction hitSide = blockHitResult.getDirection();
        final Direction.Axis axis = blockState.getValue(HungSignBlock.AXIS);
        if (!axis.test(hitSide)) {
          final Iterator<Direction> validDirections = Arrays.stream(Direction.values()).filter(axis).iterator();
          // 如果点击的方向不正确，则无法复制和粘贴文本。
          player.displayClientMessage(TextBridge.translatable("item.mishanguc.text_copy_tool.message.fail.wrong_side", RoadConnectionState.text(hitSide).withStyle(style -> style.withColor(0xeecc44)), RoadConnectionState.text(validDirections.next()).withStyle(style -> style.withColor(0xb3ee45)), RoadConnectionState.text(validDirections.next()).withStyle(style -> style.withColor(0xb3ee45))).withStyle(ChatFormatting.RED), true);
          return InteractionResult.FAIL;
        }
        final HashMap<@NotNull Direction, @Unmodifiable @NotNull List<@NotNull TextContext>> newTexts = new HashMap<>(hungSignBlockEntity.texts);
        final ImmutableList<@NotNull TextContext> newTextsThisSide = ImmutableList.copyOf(texts.stream().map(nbtElement -> TextContext.fromNbt(nbtElement, hungSignBlockEntity.createDefaultTextContext())).iterator());
        if (stack.getOrCreateTag().getBoolean("fromVanillaSign")) {
          MishangUtils.rearrange(newTextsThisSide);
        }
        if (newTextsThisSide.isEmpty()) {
          newTexts.remove(hitSide);
        } else {
          newTexts.put(hitSide, newTextsThisSide);
        }
        hungSignBlockEntity.texts = ImmutableMap.copyOf(newTexts);
        blockEntity.setChanged();
        world.sendBlockUpdated(blockPos, blockState, blockState, 3);
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.text_copy_tool.message.success.paste", newTextsThisSide.size()), true);
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        return InteractionResult.SUCCESS;
      } else if (blockEntity instanceof StandingSignBlockEntity standingSignBlockEntity) {
        if (world.isClientSide)
          return InteractionResult.SUCCESS;
        final Boolean isFront = StandingSignBlock.getHitSide(blockState, blockHitResult);
        if (isFront != null) {
          standingSignBlockEntity.setTextsOnSide(isFront, texts.stream().map(nbtElement -> TextContext.fromNbt(nbtElement, standingSignBlockEntity.createDefaultTextContext())).collect(ImmutableList.toImmutableList()));
          if (stack.getOrCreateTag().getBoolean("fromVanillaSign")) {
            MishangUtils.rearrange(standingSignBlockEntity.getTextsOnSide(isFront));
          }
          blockEntity.setChanged();
          world.sendBlockUpdated(blockPos, blockState, blockState, 3);
          player.displayClientMessage(TextBridge.translatable("item.mishanguc.text_copy_tool.message.success.paste", standingSignBlockEntity.getTextsOnSide(isFront).size()), true);
          stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
          return InteractionResult.SUCCESS;
        }
      } else {
        if (world.isClientSide)
          return InteractionResult.PASS;
        // 点击的方块不是可以识别的告示牌方块。
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.text_copy_tool.message.fail.not_sign").withStyle(ChatFormatting.RED), true);
        return InteractionResult.FAIL;
      }
    } catch (
        Throwable throwable) {
      player.displayClientMessage(TextBridge.translatable("item.mishanguc.text_copy_tool.message.fail.unexpected").withStyle(ChatFormatting.RED), true);
      LOGGER.error("Unexpected error found when pasting text", throwable);
    }
    return InteractionResult.PASS;
  }

  /**
   * 持有该物品，左键（攻击，默认为左键）点击告示牌可复制其文字。如果被点击的告示牌不是文字，则不产生效果。若点击悬挂的告示牌，则只会复制其中一边的文字。
   */
  @Override
  public InteractionResult beginAttackBlock(ItemStack stack, Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction, boolean fluidIncluded) {
    // 本方法仅限在服务器上使用。
    final BlockEntity blockEntity = world.getBlockEntity(pos);
    if (blockEntity instanceof SignBlockEntity signBlockEntity) {
      if (world.isClientSide)
        return InteractionResult.SUCCESS;
      // 原版的告示牌
      final ListTag texts = new ListTag();
      final SignText textFacing = signBlockEntity.getText(signBlockEntity.isFacingFrontText(player));
      for (int i = 0; i < 4; i++) {
        final TextContext textContext = new TextContext();
        textContext.text = textFacing.getMessage(i, false).copy();
        if (TextBridge.isEmpty(textContext.text))
          continue;
        textContext.color = textFacing.getColor().getTextColor();

        final Style style = textContext.text.getStyle();
        if (textContext.text.getContents() instanceof LiteralContents && textContext.text.getSiblings().isEmpty() && style.getClickEvent() == null && style.getHoverEvent() == null && style.getFont() == Style.DEFAULT_FONT && style.getInsertion() == null) {
          // 对于文本为 literalText 的情况，应该将其 style 对象中的属性转化为 textContent 中的属性，除非 style 中有无法转换的部分。
          textContext.bold = style.isBold();
          textContext.italic = style.isItalic();
          textContext.strikethrough = style.isStrikethrough();
          textContext.underline = style.isUnderlined();
          textContext.obfuscated = style.isObfuscated();
          if (style.getColor() != null) {
            textContext.color = style.getColor().getValue();
          }
          textContext.text = TextBridge.literal(((LiteralContents) textContext.text.getContents()).text());
        }
        final CompoundTag nbt0 = textContext.createNbt();
        nbt0.remove("size"); // 原版告示牌的文本没有 size
        texts.add(nbt0);
      }
      stack.addTagElement("fromVanillaSign", ByteTag.valueOf(true));
      stack.addTagElement("texts", texts);
      player.displayClientMessage(TextBridge.translatable("item.mishanguc.text_copy_tool.message.success.copy", texts.size()), true);
      return InteractionResult.SUCCESS;
    } else if (blockEntity instanceof WallSignBlockEntity wallSignBlockEntity) {
      if (world.isClientSide)
        return InteractionResult.SUCCESS;
      // 迷上城建模组的墙上告示牌方块
      final ListTag texts = new ListTag();
      for (TextContext textContext : wallSignBlockEntity.textContexts) {
        texts.add(textContext.createNbt());
      }
      stack.addTagElement("texts", texts);
      stack.addTagElement("fromVanillaSign", ByteTag.valueOf(false));
      player.displayClientMessage(TextBridge.translatable("item.mishanguc.text_copy_tool.message.success.copy", texts.size()), true);
      return InteractionResult.SUCCESS;
    } else {
      final BlockState blockState = world.getBlockState(pos);
      if (blockEntity instanceof HungSignBlockEntity hungSignBlockEntity) {
        if (world.isClientSide)
          return InteractionResult.SUCCESS;
        final Direction.Axis axis = blockState.getValue(HungSignBlock.AXIS);
        if (!axis.test(direction)) {
          final Iterator<Direction> validDirections = Arrays.stream(Direction.values()).filter(axis).iterator();
          // 如果点击的方向不正确，则无法复制和粘贴文本。
          player.displayClientMessage(TextBridge.translatable("item.mishanguc.text_copy_tool.message.fail.wrong_side", RoadConnectionState.text(direction).withStyle(style -> style.withColor(0xeecc44)), RoadConnectionState.text(validDirections.next()).withStyle(style -> style.withColor(0xb3ee45)), RoadConnectionState.text(validDirections.next()).withStyle(style -> style.withColor(0xb3ee45))).withStyle(ChatFormatting.RED), true);
          return InteractionResult.FAIL;
        }
        final List<@NotNull TextContext> textContexts = hungSignBlockEntity.texts.getOrDefault(direction, ImmutableList.of());
        final ListTag texts = new ListTag();
        for (TextContext textContext : textContexts) {
          texts.add(textContext.createNbt());
        }
        stack.addTagElement("texts", texts);
        stack.addTagElement("fromVanillaSign", ByteTag.valueOf(false));
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.text_copy_tool.message.success.copy", texts.size()), true);
        return InteractionResult.SUCCESS;
      } else if (blockEntity instanceof StandingSignBlockEntity standingSignBlockEntity) {
        Boolean hitSide = StandingSignBlock.getHitSide(blockState, direction);
        if (hitSide == null) {
          final HitResult raycast0 = player.pick(4.5, 0, includesFluid(stack, false));
          if (raycast0 instanceof BlockHitResult)
            hitSide = StandingSignBlock.getHitSide(blockState, (BlockHitResult) raycast0);
        }
        if (hitSide == null)
          return world.isClientSide ? InteractionResult.PASS : InteractionResult.FAIL;
        final List<TextContext> textContexts = standingSignBlockEntity.getTextsOnSide(hitSide);
        final ListTag texts = new ListTag();
        texts.addAll(Collections2.transform(textContexts, TextContext::createNbt));
        stack.addTagElement("texts", texts);
        stack.addTagElement("fromVanillaSign", ByteTag.valueOf(false));
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.text_copy_tool.message.success.copy", texts.size()), true);
        return InteractionResult.SUCCESS;
      } else {
        if (world.isClientSide)
          return InteractionResult.SUCCESS;
        // 点击的方块不是可以识别的告示牌方块。
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.text_copy_tool.message.fail.not_sign").withStyle(ChatFormatting.RED), true);
        return InteractionResult.FAIL;
      }
    }
  }

  @OnlyIn(Dist.CLIENT)
  @Override
  public boolean renderBlockOutline(Player player, ItemStack itemStack, WorldRenderContext worldRenderContext, WorldRenderContext.BlockOutlineContext blockOutlineContext, InteractionHand hand) {
    final BlockEntity blockEntity = worldRenderContext.world().getBlockEntity(blockOutlineContext.blockPos());
    if (blockEntity instanceof SignBlockEntity || blockEntity instanceof HungSignBlockEntity || blockEntity instanceof WallSignBlockEntity || blockEntity instanceof StandingSignBlockEntity) {
      return super.renderBlockOutline(player, itemStack, worldRenderContext, blockOutlineContext, hand);
    } else {
      return false;
    }
  }

  @Override
  public @NotNull RecipeBuilder getCraftingRecipe() {
    return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, this)
        .pattern("SPS")
        .pattern(" / ")
        .pattern(" / ")
        .define('P', Items.PAPER)
        .define('S', Items.SLIME_BALL)
        .define('/', Items.STICK)
        .unlockedBy("has_paper", FabricRecipeProvider.has(Items.PAPER))
        .unlockedBy("has_slime_ball", FabricRecipeProvider.has(Items.SLIME_BALL));
  }
}
