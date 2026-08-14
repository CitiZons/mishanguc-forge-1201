package pers.solid.mishang.uc.item;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.Mishanguc;
import pers.solid.mishang.uc.mixin.WorldRendererInvoker;
import pers.solid.mishang.uc.util.BlockPlacementContext;
import pers.solid.mishang.uc.util.TextBridge;

import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import com.mojang.blaze3d.vertex.VertexConsumer;
import pers.solid.mishang.uc.data.stubs.WorldRenderContext;

public class ColumnBuildingTool extends BlockToolItem implements HotbarScrollInteraction {
  /**
   * 记录放置柱的操作记录。当玩家放置了柱之后，可以对其进行撤销，其操作记录就是存储在这个里面的。
   */
  private static final WeakHashMap<ServerPlayer, Triple<ServerLevel, Block, BoundingBox>> tempMemory = new WeakHashMap<>();
  /**
   * 客户端的操作记录。类型使用 {@link Level} 而非 {@code ClientLevel}，以免专用服务器加载本类时接触客户端专有的类；
   * 该字段只会在客户端被赋值，实际存储的是 {@code ClientLevel}。
   */
  private static @Nullable Triple<Level, Block, BoundingBox> clientTempMemory = null;

  // TODO: Re-register with Forge events (PlayerEvent.PlayerLoggedOutEvent)
  public static void registerTempMemoryEvents() {
    // Forge equivalent: subscribe to PlayerEvent.PlayerLoggedOutEvent on MinecraftForge.EVENT_BUS
    // tempMemory cleanup will be handled in Mishanguc main class event handlers
  }

  public ColumnBuildingTool(Properties settings, @Nullable Boolean includesFluid) {
    super(settings, includesFluid);
  }

  @Override
  public Component getName(ItemStack stack) {
    return TextBridge.translatable("item.mishanguc.column_building_tool.format", getDescription(), Integer.toString(getLength(stack)));
  }

  @Override
  public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    super.appendHoverText(stack, world, tooltip, context);
    tooltip.add(TextBridge.translatable("item.mishanguc.column_building_tool.tooltip.1").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.column_building_tool.tooltip.2").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.column_building_tool.tooltip.3").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.column_building_tool.tooltip.length", TextBridge.literal(Integer.toString(getLength(stack))).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));
  }

  @Override
  public InteractionResult useOnBlock(ItemStack stack, Player player, Level world, BlockHitResult blockHitResult, InteractionHand hand, boolean fluidIncluded) {
    if (!player.isCreative()) {
      // 仅限创造模式玩家使用。
      return InteractionResult.PASS;
    }
    final Direction side = blockHitResult.getDirection();
    final BlockPos originBlockPos = blockHitResult.getBlockPos();
    final BlockPlacementContext blockPlacementContext = new BlockPlacementContext(world, originBlockPos, player, stack, blockHitResult, fluidIncluded);
    final int length = this.getLength(stack);
    boolean soundPlayed = false;
    final BlockPos.MutableBlockPos posToPlace = new BlockPos.MutableBlockPos().set(blockPlacementContext.posToPlace);
    if (blockPlacementContext.canPlace()) {
      for (int i = 0; i < length; i++) {
        if (world.getBlockState(posToPlace).canBeReplaced(blockPlacementContext.placementContext)) {
          if (!world.isClientSide) {
            world.setBlock(posToPlace, blockPlacementContext.stateToPlace, 0b1011);
            BlockEntity entityToPlace = world.getBlockEntity(posToPlace);
            if (blockPlacementContext.stackInHand != null) {
              BlockItem.updateCustomBlockEntityTag(world, player, posToPlace, blockPlacementContext.stackInHand);
            } else if (blockPlacementContext.hitEntity != null && entityToPlace != null) {
              entityToPlace.load(blockPlacementContext.hitEntity.saveWithFullMetadata());
              entityToPlace.setChanged();
              world.sendBlockUpdated(posToPlace, entityToPlace.getBlockState(), entityToPlace.getBlockState(), Block.UPDATE_ALL);
            }
          }
          if (!soundPlayed) blockPlacementContext.playSound();
          soundPlayed = true;
        } else {
          posToPlace.move(side, -1);
          break;
        }
        posToPlace.move(side);
      } // end for
    }
    if (soundPlayed) {
      if (!world.isClientSide) {
        tempMemory.put(((ServerPlayer) player), Triple.of(((ServerLevel) world), blockPlacementContext.stateToPlace.getBlock(), BoundingBox.fromCorners(blockPlacementContext.posToPlace, posToPlace.immutable())));
      } else if (FMLEnvironment.dist == Dist.CLIENT) {
        clientTempMemory = Triple.of(world, blockPlacementContext.stateToPlace.getBlock(), BoundingBox.fromCorners(blockPlacementContext.posToPlace, posToPlace.immutable()));
      }
    }
    return InteractionResult.SUCCESS;
  }

  public int getLength(ItemStack stack) {
    final CompoundTag nbt = stack.getOrCreateTag();
    return nbt.contains("Length", Tag.TAG_ANY_NUMERIC) ? Mth.clamp(1, nbt.getInt("Length"), 64) : 8;
  }

  @Override
  public InteractionResult beginAttackBlock(ItemStack stack, Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction, boolean fluidIncluded) {
    @Nullable BoundingBox lastPlacedBox = null;
    @Nullable Block lastPlacedBlock = null;

    // 检查是否存在上次记录的区域。如果有，且点击的方块在该区域内，则直接删除这个区域的方块。
    // 注意：只要点击了，即使点击的位置不在该区域内，也会清除有关的记录。
    if (!world.isClientSide) {
      final Triple<ServerLevel, Block, BoundingBox> pair = tempMemory.get(((ServerPlayer) player));
      if (pair != null && pair.getLeft().equals(world) && pair.getRight().isInside(pos)) {
        lastPlacedBox = pair.getRight();
        lastPlacedBlock = pair.getMiddle();
      }
      tempMemory.remove(player);
    } else if (FMLEnvironment.dist == Dist.CLIENT) {
      if (clientTempMemory != null && clientTempMemory.getLeft().equals(world) && clientTempMemory.getRight().isInside(pos)) {
        lastPlacedBox = clientTempMemory.getRight();
        lastPlacedBlock = clientTempMemory.getMiddle();
      }
      clientTempMemory = null;
    }
    if (lastPlacedBox != null && lastPlacedBlock != null && !world.isClientSide) {
      for (BlockPos posToRemove : BlockPos.betweenClosed(lastPlacedBox.minX(), lastPlacedBox.minY(), lastPlacedBox.minZ(), lastPlacedBox.maxX(), lastPlacedBox.maxY(), lastPlacedBox.maxZ())) {
        final BlockState existingState = world.getBlockState(posToRemove);
        if (lastPlacedBlock.equals(existingState.getBlock()) && !(existingState.getBlock() instanceof GameMasterBlock && !player.hasPermissions(2))) {
          // 非管理员不应该破坏管理方块。
          if (fluidIncluded) {
            world.setBlockAndUpdate(posToRemove, Blocks.AIR.defaultBlockState());
          } else {
            world.removeBlock(posToRemove, false);
          }
        }
      }
      return InteractionResult.SUCCESS;
    }
    return InteractionResult.PASS;
  }

  @Override
  public void onScroll(int selectedSlot, double scrollAmount, ServerPlayer player, ItemStack stack) {
    final int length = Mth.clamp(getLength(stack) - (int) scrollAmount, 1, 64);
    stack.getOrCreateTag().putInt("Length", length);
  }

  @OnlyIn(Dist.CLIENT)
  @Override
  public boolean renderBlockOutline(Player player, ItemStack itemStack, WorldRenderContext worldRenderContext, WorldRenderContext.BlockOutlineContext blockOutlineContext, InteractionHand hand) {
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
    final int length = getLength(itemStack);
    final BlockHitResult raycast;
    if (minecraft.hitResult instanceof BlockHitResult blockHitResult && blockHitResult.getType() == HitResult.Type.BLOCK) {
      raycast = blockHitResult;
    } else {
      return true;
    }
    final ClientLevel world = worldRenderContext.world();
    final BlockPlacementContext blockPlacementContext = new BlockPlacementContext(world, blockOutlineContext.blockPos(), player, itemStack, raycast, includesFluid);

    // 绘制将要放置的方块。

    final Direction side = blockHitResult.getDirection();
    final BlockPos.MutableBlockPos posToPlace = new BlockPos.MutableBlockPos().set(blockPlacementContext.posToPlace);
    if (blockPlacementContext.canPlace()) {
      for (int i = 0; i < length; i++) {
        if (world.getBlockState(posToPlace).canBeReplaced(blockPlacementContext.placementContext)) {
          WorldRendererInvoker.drawCuboidShapeOutline(
              worldRenderContext.matrixStack(),
              vertexConsumer,
              blockPlacementContext.stateToPlace.getShape(world, posToPlace, CollisionContext.of(player)),
              posToPlace.getX() - blockOutlineContext.cameraX(),
              posToPlace.getY() - blockOutlineContext.cameraY(),
              posToPlace.getZ() - blockOutlineContext.cameraZ(),
              0,
              1,
              1,
              0.8f);
          if (includesFluid) {
            WorldRendererInvoker.drawCuboidShapeOutline(
                worldRenderContext.matrixStack(),
                vertexConsumer,
                blockPlacementContext.stateToPlace.getFluidState().getShape(world, posToPlace),
                posToPlace.getX() - blockOutlineContext.cameraX(),
                posToPlace.getY() - blockOutlineContext.cameraY(),
                posToPlace.getZ() - blockOutlineContext.cameraZ(),
                0,
                0.5f,
                1,
                0.5f);
          }
        } else {
          posToPlace.move(side, -1);
          break;
        }
        posToPlace.move(side);
      }
    }

    // 绘制上次移除过的方块。

    if (hand == InteractionHand.MAIN_HAND && clientTempMemory != null && clientTempMemory.getLeft().equals(world) && clientTempMemory.getRight().isInside(blockHitResult.getBlockPos())) {
      final BoundingBox lastPlacedBox = clientTempMemory.getRight();
      final Block lastPlacedBlock = clientTempMemory.getMiddle();
      for (BlockPos posToRemove : BlockPos.betweenClosed(lastPlacedBox.minX(), lastPlacedBox.minY(), lastPlacedBox.minZ(), lastPlacedBox.maxX(), lastPlacedBox.maxY(), lastPlacedBox.maxZ())) {
        final BlockState existingState = world.getBlockState(posToRemove);
        if (lastPlacedBlock.equals(existingState.getBlock()) && !(existingState.getBlock() instanceof GameMasterBlock && !player.hasPermissions(2))) {
          WorldRendererInvoker.drawCuboidShapeOutline(
              worldRenderContext.matrixStack(),
              vertexConsumer,
              existingState.getShape(world, posToRemove, CollisionContext.of(player)),
              posToRemove.getX() - blockOutlineContext.cameraX(),
              posToRemove.getY() - blockOutlineContext.cameraY(),
              posToRemove.getZ() - blockOutlineContext.cameraZ(),
              1,
              0,
              0,
              0.8f);
          if (includesFluid) {
            WorldRendererInvoker.drawCuboidShapeOutline(
                worldRenderContext.matrixStack(),
                vertexConsumer,
                existingState.getFluidState().getShape(world, posToRemove),
                posToRemove.getX() - blockOutlineContext.cameraX(),
                posToRemove.getY() - blockOutlineContext.cameraY(),
                posToRemove.getZ() - blockOutlineContext.cameraZ(),
                1,
                0.5f,
                0,
                0.5f);
          }
        }
      }
      // 绘制了红色之后，就不再绘制原版的边框。
      return false;
    }
    // 由于常规的破坏方便可能仍然有效，因此保留原先的边框绘制。
    return true;
  }
}
