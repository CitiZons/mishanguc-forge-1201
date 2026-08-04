package pers.solid.mishang.uc.item;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangucClient;
import pers.solid.mishang.uc.MishangucRules;
import pers.solid.mishang.uc.mixin.WorldRendererInvoker;
import pers.solid.mishang.uc.render.RendersBeforeOutline;
import pers.solid.mishang.uc.util.BlockPlacementContext;
import pers.solid.mishang.uc.util.TextBridge;

import java.util.List;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.LocalPlayer;
import pers.solid.mishang.uc.data.stubs.WorldRenderContext;

public class ForcePlacingToolItem extends BlockToolItem implements InteractsWithEntity, RendersBeforeOutline {

  public ForcePlacingToolItem(Properties settings, @Nullable Boolean includesFluid) {
    super(settings, includesFluid);
  }

  @Override
  public InteractionResult useOnBlock(
      ItemStack stack, Player player,
      Level world,
      BlockHitResult blockHitResult,
      InteractionHand hand,
      boolean fluidIncluded) {
    if (!hasAccess(player, world, true)) {
      // 仅限特定情况下使用。
      return InteractionResult.PASS;
    }
    BlockPlacementContext blockPlacementContext = new BlockPlacementContext(world, blockHitResult.getBlockPos(), player, player.getItemInHand(hand), blockHitResult, fluidIncluded);
    blockPlacementContext.playSound();
    // 放置方块。对客户端和服务器均生效。
    int flags = getFlags(stack);
    suppressOnBlockAdded = true;
    blockPlacementContext.setBlockState(flags);
    suppressOnBlockAdded = false;
    blockPlacementContext.setBlockEntity();
    return InteractionResult.sidedSuccess(world.isClientSide);
  }

  public static boolean suppressOnBlockAdded = false;

  @Override
  public InteractionResult beginAttackBlock(
      ItemStack stack, Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction, boolean fluidIncluded) {
    if (!hasAccess(player, world, true)) {
      // 仅限特定情况下使用。
      return InteractionResult.PASS;
    }
    final BlockState blockState = world.getBlockState(pos);
    world.levelEvent(player, 2001, pos, Block.getId(world.getBlockState(pos)));
    FluidState fluidState = blockState.getFluidState();
    // 在破坏时，直接先将其内容清除。
    world.removeBlockEntity(pos);
    int flags = getFlags(stack);
    world.setBlock(pos, fluidIncluded ? Blocks.AIR.defaultBlockState() : fluidState.createLegacyBlock(), flags);
    return InteractionResult.sidedSuccess(world.isClientSide);
  }

  private static int getFlags(ItemStack stack) {
    return 0b11010;
  }

  @Override
  public void appendHoverText(
      ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    super.appendHoverText(stack, world, tooltip, context);
    tooltip.add(
        TextBridge.translatable("item.mishanguc.force_placing_tool.tooltip.1")
            .withStyle(ChatFormatting.GRAY));
    tooltip.add(
        TextBridge.translatable("item.mishanguc.force_placing_tool.tooltip.2")
            .withStyle(ChatFormatting.GRAY));
    if (Boolean.TRUE.equals(includesFluid(stack))) {
      tooltip.add(
          TextBridge.translatable("item.mishanguc.force_placing_tool.tooltip.fluids")
              .withStyle(ChatFormatting.GRAY));
    }
    tooltip.add(
        TextBridge.translatable("item.mishanguc.force_placing_tool.tooltip.3")
            .withStyle(ChatFormatting.GRAY));
    if ((getFlags(stack) & 128) != 0) {
      tooltip.add(TextBridge.translatable("item.mishanguc.force_placing_tool.tooltip.suspends_light")
          .withStyle(ChatFormatting.YELLOW));
    }
  }

  @OnlyIn(Dist.CLIENT)
  @Override
  public boolean renderBlockOutline(
      Player player,
      ItemStack itemStack,
      WorldRenderContext worldRenderContext,
      WorldRenderContext.BlockOutlineContext blockOutlineContext, InteractionHand hand) {
    final Minecraft minecraft = Minecraft.getInstance();
    if (!hasAccess(player, worldRenderContext.world(), false)) {
      // 只有在符合条件的情况下，才会绘制边框。
      return true;
    } else {
      final Item item = player.getMainHandItem().getItem();
      if (hand == InteractionHand.OFF_HAND && (item instanceof BlockItem || item instanceof CarryingToolItem)) {
        // 当玩家副手持有物品，主手持有方块时，直接跳过，不绘制。
        return true;
      }
    }
    final MultiBufferSource consumers = worldRenderContext.consumers();
    if (consumers == null) {
      return true;
    }
    final VertexConsumer vertexConsumer = consumers.getBuffer(RenderType.LINES);

    final BlockHitResult blockHitResult;
    final PoseStack matrices = worldRenderContext.matrixStack();
    HitResult crosshairTarget = minecraft.hitResult;
    if (crosshairTarget instanceof BlockHitResult) {
      blockHitResult = (BlockHitResult) crosshairTarget;
    } else {
      return true;
    }
    final boolean includesFluid = this.includesFluid(itemStack, player.isShiftKeyDown());
    final BlockPlacementContext blockPlacementContext =
        new BlockPlacementContext(
            worldRenderContext.world(),
            blockOutlineContext.blockPos(),
            player,
            itemStack,
            blockHitResult,
            includesFluid);
    WorldRendererInvoker.drawCuboidShapeOutline(
        matrices,
        vertexConsumer,
        blockPlacementContext.stateToPlace.getShape(
            blockPlacementContext.world, blockPlacementContext.posToPlace, CollisionContext.of(player)),
        blockPlacementContext.posToPlace.getX() - blockOutlineContext.cameraX(),
        blockPlacementContext.posToPlace.getY() - blockOutlineContext.cameraY(),
        blockPlacementContext.posToPlace.getZ() - blockOutlineContext.cameraZ(),
        0,
        1,
        1,
        0.8f);
    if (includesFluid) {
      WorldRendererInvoker.drawCuboidShapeOutline(
          matrices,
          vertexConsumer,
          blockPlacementContext
              .stateToPlace
              .getFluidState()
              .getShape(blockPlacementContext.world, blockPlacementContext.posToPlace),
          blockPlacementContext.posToPlace.getX() - blockOutlineContext.cameraX(),
          blockPlacementContext.posToPlace.getY() - blockOutlineContext.cameraY(),
          blockPlacementContext.posToPlace.getZ() - blockOutlineContext.cameraZ(),
          0,
          0.5f,
          1,
          0.5f);
    }
    if (hand == InteractionHand.MAIN_HAND) {
      // 只有当主手持有此物品时，才绘制红色边框。
      WorldRendererInvoker.drawCuboidShapeOutline(
          matrices,
          vertexConsumer,
          blockPlacementContext.hitState.getShape(
              blockPlacementContext.world, blockPlacementContext.blockPos, CollisionContext.of(player)),
          blockPlacementContext.blockPos.getX() - blockOutlineContext.cameraX(),
          blockPlacementContext.blockPos.getY() - blockOutlineContext.cameraY(),
          blockPlacementContext.blockPos.getZ() - blockOutlineContext.cameraZ(),
          1,
          0,
          0,
          0.8f);
      if (includesFluid) {
        WorldRendererInvoker.drawCuboidShapeOutline(
            matrices,
            vertexConsumer,
            blockPlacementContext
                .hitState
                .getFluidState()
                .getShape(blockPlacementContext.world, blockPlacementContext.blockPos),
            blockPlacementContext.blockPos.getX() - blockOutlineContext.cameraX(),
            blockPlacementContext.blockPos.getY() - blockOutlineContext.cameraY(),
            blockPlacementContext.blockPos.getZ() - blockOutlineContext.cameraZ(),
            1,
            0.5f,
            0,
            0.5f);
      }
    }
    return false;
  }

  @Override
  public @NotNull InteractionResult attackEntityCallback(
      Player player,
      Level world,
      InteractionHand hand,
      Entity entity,
      @Nullable EntityHitResult hitResult) {
    if (!hasAccess(player, world, true)) return InteractionResult.PASS;
    if (!world.isClientSide) {
      if (entity instanceof Player) {
        entity.kill();
      } else {
        entity.remove(Entity.RemovalReason.KILLED);
      }
      if (entity instanceof EnderDragonPart enderDragonPart) {
        enderDragonPart.parentMob.kill();
      }
    }
    return InteractionResult.SUCCESS;
  }

  /**
   * 玩家是否有权使用此物品。
   */
  @ApiStatus.AvailableSince("1.0.0")
  private static boolean hasAccess(Player player, Level world, boolean warn) {
    if (world.isClientSide) {
      return MishangucClient.CLIENT_FORCE_PLACING_TOOL_ACCESS.get().hasAccess(player);
    } else {
      final MishangucRules.ToolAccess toolAccess = world.getGameRules().getRule(MishangucRules.FORCE_PLACING_TOOL_ACCESS).get();
      return toolAccess.hasAccess(player, warn);
    }
  }

  @OnlyIn(Dist.CLIENT)
  @Override
  public void renderBeforeOutline(WorldRenderContext context, HitResult hitResult, LocalPlayer player, InteractionHand hand) {
    // 只在使用主手持有此物品时进行渲染。
    if (hand != InteractionHand.MAIN_HAND || !hasAccess(player, context.world(), false)) return;
    final PoseStack matrices = context.matrixStack();
    final MultiBufferSource consumers = context.consumers();
    if (consumers == null) return;
    final VertexConsumer vertexConsumer = consumers.getBuffer(RenderType.lines());
    final Vec3 cameraPos = context.camera().getPosition();
    if (hitResult instanceof EntityHitResult entityHitResult) {
      final Entity entity = entityHitResult.getEntity();
      WorldRendererInvoker.drawCuboidShapeOutline(matrices, vertexConsumer, Shapes.create(entity.getBoundingBox()), -cameraPos.x, -cameraPos.y, -cameraPos.z, 1.0f, 0f, 0f, 0.8f);
    }
  }
}
