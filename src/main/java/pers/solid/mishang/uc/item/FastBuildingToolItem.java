package pers.solid.mishang.uc.item;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.level.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.Util;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.mixin.WorldRendererInvoker;
import pers.solid.mishang.uc.util.BlockMatchingRule;
import pers.solid.mishang.uc.util.BlockPlacementContext;
import pers.solid.mishang.uc.util.TextBridge;

import java.util.Iterator;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import com.mojang.blaze3d.vertex.VertexConsumer;
import pers.solid.mishang.uc.data.stubs.WorldRenderContext;

/**
 * 该物品可以快速建造或者删除一个平面上的多个方块。
 *
 * @see BlockMatchingRule
 */
public class FastBuildingToolItem extends BlockToolItem implements HotbarScrollInteraction {

  private static final CrudeIncrementalIntIdentityHashBiMap<BlockMatchingRule> RULES_TO_CYCLE = Util.make(CrudeIncrementalIntIdentityHashBiMap.create(4), map -> {
    map.add(BlockMatchingRule.SAME_STATE);
    map.add(BlockMatchingRule.SAME_BLOCK);
    map.add(BlockMatchingRule.SAME_MATERIAL);
    map.add(BlockMatchingRule.ANY);
  });

  public FastBuildingToolItem(Properties settings, @Nullable Boolean includesFluid) {
    super(settings, includesFluid);
  }

  @Override
  public InteractionResult useOnBlock(
      ItemStack stack, Player player,
      Level world,
      BlockHitResult blockHitResult,
      InteractionHand hand,
      boolean fluidIncluded) {
    if (!player.isCreative()) {
      // 仅限创造模式玩家使用。
      return InteractionResult.PASS;
    }
    final Direction side = blockHitResult.getDirection();
    final BlockPos centerBlockPos = blockHitResult.getBlockPos();
    final BlockState centerState = world.getBlockState(centerBlockPos);
    final BlockPlacementContext blockPlacementContext = new BlockPlacementContext(
        world, centerBlockPos, player, stack, blockHitResult, fluidIncluded);
    final int range = this.getRange(stack);
    final BlockMatchingRule matchingRule = this.getMatchingRule(stack);
    boolean soundPlayed = false;
    for (BlockPos pos : matchingRule.getPlainValidBlockPoss(world, centerBlockPos, side, range)) {
      BlockState state = world.getBlockState(pos);
      if (matchingRule.match(centerState, state)) {
        final BlockPlacementContext offsetBlockPlacementContext =
            new BlockPlacementContext(blockPlacementContext, pos);
        if (offsetBlockPlacementContext.canPlace() && offsetBlockPlacementContext.canBeReplaced()) {
          if (!world.isClientSide) {
            offsetBlockPlacementContext.setBlockState(0b1011);
            offsetBlockPlacementContext.setBlockEntity();
          }
          if (!soundPlayed) offsetBlockPlacementContext.playSound();
          soundPlayed = true;
        }
      }
    } // end for
    return InteractionResult.SUCCESS;
  }

  @Override
  public InteractionResult beginAttackBlock(
      ItemStack stack, Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction, boolean fluidIncluded) {
    if (!player.isCreative()) {
      // 仅限创造模式玩家使用。
      return InteractionResult.PASS;
    }
    if (!world.isClientSide()) {
      final int range = this.getRange(stack);
      final BlockMatchingRule matchingRule = this.getMatchingRule(stack);
      for (BlockPos pos1 : matchingRule.getPlainValidBlockPoss(world, pos, direction, range)) {
        if (world.getBlockState(pos1).getBlock() instanceof GameMasterBlock && !player.hasPermissions(2)) {
          // 非管理员不应该破坏管理方块。
        } else if (fluidIncluded) {
          world.setBlockAndUpdate(pos1, Blocks.AIR.defaultBlockState());
        } else {
          world.removeBlock(pos1, false);
        }
      }
    }
    world.levelEvent(player, 2001, pos, Block.getId(world.getBlockState(pos)));
    return InteractionResult.SUCCESS;
  }

  @Override
  public ItemStack getDefaultInstance() {
    final ItemStack stack = super.getDefaultInstance();
    final CompoundTag tag = stack.getOrCreateTag();
    tag.putInt("Range", 5);
    tag.putString("MatchingRule", "mishanguc:same_block");
    return stack;
  }

  public int getRange(ItemStack stack) {
    final CompoundTag nbt = stack.getOrCreateTag();
    return nbt.contains("Range", Tag.TAG_ANY_NUMERIC) ? Integer.min(nbt.getInt("Range"), 128) : 8;
  }

  public @NotNull BlockMatchingRule getMatchingRule(ItemStack stack) {
    final CompoundTag tag = stack.getOrCreateTag();
    final BlockMatchingRule matchingRule = BlockMatchingRule.fromString(tag.getString("MatchingRule"));
    return matchingRule == null ? BlockMatchingRule.SAME_BLOCK : matchingRule;
  }

  @Override
  public void appendHoverText(
      ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    super.appendHoverText(stack, world, tooltip, context);
    tooltip.add(
        TextBridge.translatable("item.mishanguc.fast_building_tool.tooltip.1")
            .withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.fast_building_tool.tooltip.2").withStyle(ChatFormatting.GRAY));
    tooltip.add(
        TextBridge.translatable("item.mishanguc.fast_building_tool.tooltip.range", TextBridge.literal(Integer.toString(this.getRange(stack))).withStyle(ChatFormatting.YELLOW))
            .withStyle(ChatFormatting.GRAY));
    tooltip.add(
        TextBridge.translatable("item.mishanguc.fast_building_tool.tooltip.matchingRule", this.getMatchingRule(stack).getName().withStyle(ChatFormatting.YELLOW))
            .withStyle(ChatFormatting.GRAY));
  }

  protected ItemStack createStack(int range, BlockMatchingRule blockMatchingRule) {
    final ItemStack stack = new ItemStack(this);
    final CompoundTag nbt = stack.getOrCreateTag();
    nbt.putInt("Range", range);
    nbt.putString("MatchingRule", blockMatchingRule.getSerializedName());
    return stack;
  }

  public void appendToEntries(CreativeModeTab.Output stacks) {
    stacks.accept(createStack(1, BlockMatchingRule.SAME_BLOCK));
    stacks.accept(createStack(16, BlockMatchingRule.SAME_BLOCK));
    stacks.accept(createStack(32, BlockMatchingRule.SAME_BLOCK));
    stacks.accept(createStack(64, BlockMatchingRule.SAME_BLOCK));
  }

  @Override
  public Component getName(ItemStack stack) {
    return TextBridge.translatable("item.mishanguc.fast_building_tool.format", getDescription(), Integer.toString(getRange(stack)), getMatchingRule(stack).getName());
  }

  @OnlyIn(Dist.CLIENT)
  @Override
  public boolean renderBlockOutline(
      Player player,
      ItemStack itemStack,
      WorldRenderContext worldRenderContext,
      WorldRenderContext.BlockOutlineContext blockOutlineContext, InteractionHand hand) {
    final Minecraft minecraft = Minecraft.getInstance();
    if (!player.isCreative()) {
      // 只有在创造模式下，才会绘制边框。
      return true;
    } else if (hand == InteractionHand.OFF_HAND && player.getMainHandItem().getItem() instanceof BlockItem) {
      // 当玩家副手持有物品，主手持有方块时，直接跳过，不绘制。
      return true;
    }
    final MultiBufferSource consumers = worldRenderContext.consumers();
    if (consumers == null) return true;
    final VertexConsumer vertexConsumer = consumers.getBuffer(RenderType.LINES);
    final boolean includesFluid = this.includesFluid(itemStack, player.isShiftKeyDown());
    final BlockMatchingRule matchingRule = this.getMatchingRule(itemStack);
    final int range = this.getRange(itemStack);
    final BlockHitResult raycast;
    if (minecraft.hitResult instanceof BlockHitResult blockHitResult && blockHitResult.getType() == HitResult.Type.BLOCK) {
      raycast = blockHitResult;
    } else {
      return true;
    }
    final ClientLevel world = worldRenderContext.world();
    final BlockPlacementContext blockPlacementContext = new BlockPlacementContext(world, blockOutlineContext.blockPos(), player, itemStack, raycast, includesFluid);
    for (BlockPos pos : matchingRule.getPlainValidBlockPoss(world, raycast.getBlockPos(), raycast.getDirection(), range)) {
      final BlockState state = world.getBlockState(pos);
      final BlockPlacementContext offsetBlockPlacementContext = new BlockPlacementContext(blockPlacementContext, pos);
      if (offsetBlockPlacementContext.canPlace() && offsetBlockPlacementContext.canBeReplaced()) {
        WorldRendererInvoker.drawCuboidShapeOutline(
            worldRenderContext.matrixStack(),
            vertexConsumer,
            offsetBlockPlacementContext.stateToPlace.getShape(world, pos, CollisionContext.of(player)),
            offsetBlockPlacementContext.posToPlace.getX() - blockOutlineContext.cameraX(),
            offsetBlockPlacementContext.posToPlace.getY() - blockOutlineContext.cameraY(),
            offsetBlockPlacementContext.posToPlace.getZ() - blockOutlineContext.cameraZ(),
            0,
            1,
            1,
            0.8f);
        if (includesFluid) {
          WorldRendererInvoker.drawCuboidShapeOutline(
              worldRenderContext.matrixStack(),
              vertexConsumer,
              offsetBlockPlacementContext.stateToPlace.getFluidState().getShape(world, pos),
              offsetBlockPlacementContext.posToPlace.getX() - blockOutlineContext.cameraX(),
              offsetBlockPlacementContext.posToPlace.getY() - blockOutlineContext.cameraY(),
              offsetBlockPlacementContext.posToPlace.getZ() - blockOutlineContext.cameraZ(),
              0,
              0.5f,
              1,
              0.5f);
        }
      }
      if (hand == InteractionHand.MAIN_HAND && !(state.getBlock() instanceof GameMasterBlock && !player.hasPermissions(2))) {
        // 只有当主手持有此物品时，才绘制边框，且对非管理员玩家忽略管理员方块。
        WorldRendererInvoker.drawCuboidShapeOutline(
            worldRenderContext.matrixStack(),
            vertexConsumer,
            state.getShape(world, pos, CollisionContext.of(player)),
            pos.getX() - blockOutlineContext.cameraX(),
            pos.getY() - blockOutlineContext.cameraY(),
            pos.getZ() - blockOutlineContext.cameraZ(),
            1,
            0,
            0,
            0.8f);
        if (includesFluid) {
          WorldRendererInvoker.drawCuboidShapeOutline(
              worldRenderContext.matrixStack(),
              vertexConsumer,
              state.getFluidState().getShape(world, pos),
              pos.getX() - blockOutlineContext.cameraX(),
              pos.getY() - blockOutlineContext.cameraY(),
              pos.getZ() - blockOutlineContext.cameraZ(),
              1,
              0.5f,
              0,
              0.5f);
        }
      }
    }
    return false;
  }

  @Override
  public void onScroll(int selectedSlot, double scrollAmount, ServerPlayer player, ItemStack stack) {
    final BlockMatchingRule currentRule = getMatchingRule(stack);
    final int i = RULES_TO_CYCLE.getId(currentRule);
    if (i == -1) return;
    final int j = (int) Mth.positiveModulo(i - scrollAmount, RULES_TO_CYCLE.size());
    final BlockMatchingRule newRule = RULES_TO_CYCLE.byId(j);
    if (newRule != null) {
      stack.addTagElement("MatchingRule", StringTag.valueOf(newRule.getId().toString()));
      final MutableComponent text = TextBridge.literal("[ ");
      for (Iterator<BlockMatchingRule> iterator = RULES_TO_CYCLE.iterator(); iterator.hasNext(); ) {
        BlockMatchingRule rule = iterator.next();
        final MutableComponent name = rule.getName();
        if (rule == newRule) name.withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE);
        text.append(name);
        if (iterator.hasNext()) text.append(" | ");
      }
      text.append(" ]");
      player.displayClientMessage(text, true);
    }
  }
}
