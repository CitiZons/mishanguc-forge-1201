package pers.solid.mishang.uc.item;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.*;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangucClient;
import pers.solid.mishang.uc.MishangucRules;
import pers.solid.mishang.uc.mixin.WorldRendererInvoker;
import pers.solid.mishang.uc.render.RendersBeforeOutline;
import pers.solid.mishang.uc.util.BlockPlacementContext;
import pers.solid.mishang.uc.util.TextBridge;
import net.minecraft.ChatFormatting;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionResultHolder;
import pers.solid.mishang.uc.data.stubs.WorldRenderContext;

@ApiStatus.AvailableSince("0.2.4")
public class CarryingToolItem extends BlockToolItem
    implements MishangucItem, InteractsWithEntity, RendersBeforeOutline {
  public CarryingToolItem(Properties settings, @Nullable Boolean includesFluid) {
    super(settings, includesFluid);
  }

  @Contract(pure = true)
  public static boolean hasHoldingBlockState(@NotNull ItemStack stack) {
    return stack.getTagElement("holdingBlockState") != null;
  }

  @Contract(pure = true)
  public static boolean hasHoldingEntity(@NotNull ItemStack stack) {
    final CompoundTag nbt = stack.getTag();
    return nbt != null && nbt.contains("holdingEntityType", Tag.TAG_STRING);
  }

  @Contract(pure = true)
  public static @Nullable Block getHoldingBlock(@NotNull ItemStack stack) {
    final CompoundTag holdingBlockStateNbt = stack.getTagElement("holdingBlockState");
    if (holdingBlockStateNbt == null)
      return null;
    final ResourceLocation identifier = ResourceLocation.tryParse(holdingBlockStateNbt.getString("Name"));
    return BuiltInRegistries.BLOCK.get(identifier);
  }

  @Contract(pure = true)
  public static @Nullable BlockState getHoldingBlockState(@NotNull ItemStack stack, LevelReader world) {
    final CompoundTag holdingBlockStateNbt = stack.getTagElement("holdingBlockState");
    if (holdingBlockStateNbt != null) {
      try {
        return NbtUtils.readBlockState(world.holderLookup(Registries.BLOCK), holdingBlockStateNbt);
      } catch (
          Exception e) {
        return null;
      }
    } else {
      return null;
    }
  }

  @Contract(mutates = "param1")
  public static void setHoldingEntity(@NotNull ItemStack stack, @Nullable Entity entity) {
    if (entity == null) {
      stack.getOrCreateTag().remove("holdingEntityType");
      stack.getOrCreateTag().remove("EntityTag");
      stack.getOrCreateTag().remove("holdingEntityName");
      stack.getOrCreateTag().remove("holdingEntityWidth");
      stack.getOrCreateTag().remove("holdingEntityHeight");
    } else {
      CompoundTag entityTag = new CompoundTag();
      entity.saveWithoutId(entityTag);
      final CompoundTag nbt = stack.getOrCreateTag();
      nbt.put("EntityTag", entityTag);
      nbt.putString("holdingEntityType", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
      nbt.putString("holdingEntityName", Component.Serializer.toJson(entity.getName()));
      nbt.putFloat("holdingEntityWidth", entity.getBbWidth());
      nbt.putFloat("holdingEntityHeight", entity.getBbHeight());
    }
  }

  /**
   * 避免重复召唤实体时，因为 UUID 雷同而无法召唤。
   */
  @Contract(mutates = "param1")
  private static void setHoldingEntityUUID(ItemStack stack, UUID uuid) {
    final CompoundTag entityTag = stack.getTagElement("EntityTag");
    if (entityTag != null) {
      entityTag.putUUID("UUID", uuid);
    }
  }

  @Contract(mutates = "param1")
  public static void setHoldingBlockState(@NotNull ItemStack stack, @Nullable BlockState state) {
    if (state == null) {
      stack.getOrCreateTag().remove("holdingBlockState");
    } else {
      stack.addTagElement("holdingBlockState", NbtUtils.writeBlockState(state));
    }
  }

  @Contract(pure = true)
  public static @Nullable Entity createHoldingEntity(@NotNull ItemStack stack, ServerLevel world, Player player) {
    final CompoundTag nbt = stack.getTag();
    if (nbt != null) {
      final String holdingEntityType = nbt.getString("holdingEntityType");
      final ResourceLocation entityTypeId = ResourceLocation.tryParse(holdingEntityType);
      if (entityTypeId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(entityTypeId)) {
        // 无效的 id，予以 null。
        return null;
      } else {
        final EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityTypeId);
        return entityType.create(world, nbt, null, player.blockPosition(), MobSpawnType.EVENT, false, false);
      }
    }
    return null;
  }

  private static MutableComponent getEntityName(@NotNull ItemStack stack) {
    final CompoundTag nbt = stack.getTag();
    if (nbt == null)
      return TextBridge.empty();
    if (nbt.contains("holdingEntityName", Tag.TAG_STRING)) {
      return Component.Serializer.fromJson(nbt.getString("holdingEntityName"));
    } else if (nbt.contains("holdingEntityType", Tag.TAG_STRING)) {
      final ResourceLocation holdingEntityType = ResourceLocation.tryParse(nbt.getString("holdingEntityType"));
      return BuiltInRegistries.ENTITY_TYPE.containsKey(holdingEntityType) ? BuiltInRegistries.ENTITY_TYPE.get(holdingEntityType).getDescription().copy() : TextBridge.literal(String.valueOf(holdingEntityType));
    } else {
      return TextBridge.empty();
    }
  }

  @Override
  public Component getName(ItemStack stack) {
    final Component name = super.getName(stack);
    final Block holdingBlock = getHoldingBlock(stack);
    if (hasHoldingEntity(stack)) {
      return TextBridge.translatable("item.mishanguc.carrying_tool.holding", name, getEntityName(stack));
    } else if (holdingBlock == null) {
      return TextBridge.translatable("item.mishanguc.carrying_tool.empty", name);
    } else {
      return TextBridge.translatable("item.mishanguc.carrying_tool.holding", name, holdingBlock.getName());
    }
  }

  @Override
  public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    super.appendHoverText(stack, world, tooltip, context);
    tooltip.add(TextBridge.translatable("item.mishanguc.carrying_tool.tooltip.1").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.carrying_tool.tooltip.2").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.carrying_tool.tooltip.3").withStyle(ChatFormatting.GRAY));
    final Block holdingBlock = getHoldingBlock(stack);
    if (holdingBlock != null) {
      tooltip.add(TextBridge.translatable("item.mishanguc.carrying_tool.tooltip.currently", holdingBlock.getName().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));
    } else if (hasHoldingEntity(stack)) {
      tooltip.add(TextBridge.translatable("item.mishanguc.carrying_tool.tooltip.currently", getEntityName(stack).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));
    }
  }

  @Override
  public InteractionResult useOnBlock(ItemStack stack, Player player, Level world, BlockHitResult blockHitResult, InteractionHand hand, boolean fluidIncluded) {
    if (!hasAccess(player, world, true)) {
      return InteractionResult.PASS;
    }
    if (hasHoldingBlockState(stack)) {
      final BlockPlacementContext blockPlacementContext = new BlockPlacementContext(world, blockHitResult.getBlockPos(), player, stack, blockHitResult, fluidIncluded);
      if (blockPlacementContext.canPlace()) {
        blockPlacementContext.setBlockState(3);
        blockPlacementContext.setBlockEntity();
        if (world.isClientSide) {
          blockPlacementContext.playSound();
        } else {
          player.displayClientMessage(TextBridge.translatable(player.isCreative() ? "item.mishanguc.carrying_tool.message.placed_creative" : "item.mishanguc.carrying_tool.message.placed", blockPlacementContext.stateToPlace.getBlock().getName()), true);
        }
        if (!player.isCreative()) {
          setHoldingBlockState(stack, null);
          stack.getOrCreateTag().remove("BlockEntityTag");
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
      } else {
        return InteractionResult.PASS;
      }
    } else if (hasHoldingEntity(stack)) {
      if (world instanceof ServerLevel serverWorld) {
        final Entity entity = createHoldingEntity(stack, serverWorld, player);
        if (entity == null)
          return InteractionResult.PASS;
        final Vec3 pos = blockHitResult.getLocation();
        entity.setPos(pos.x, pos.y, pos.z);
        final boolean spawnEntity = world.addFreshEntity(entity);
        if (spawnEntity) {
          player.displayClientMessage(TextBridge.translatable(player.isCreative() ? "item.mishanguc.carrying_tool.message.spawned_creative" : "item.mishanguc.carrying_tool.message.spawned", getEntityName(stack)), true);
          if (!player.isCreative()) {
            setHoldingEntity(stack, null);
          } else {
            setHoldingEntityUUID(stack, Mth.createInsecureUUID());
          }
          return InteractionResult.SUCCESS;
        } else {
          return InteractionResult.FAIL;
        }
      } else {
        // 客户端部分。
        return InteractionResult.SUCCESS;
      }
    } else {
      final BlockState blockState = world.getBlockState(blockHitResult.getBlockPos());
      final InteractionResult actionResult = blockState.use(world, player, hand, blockHitResult);
      if (actionResult.consumesAction()) {
        return actionResult;
      } else {
        if (world.isClientSide) {
          return InteractionResult.PASS;
        } else {
          player.displayClientMessage(TextBridge.translatable("item.mishanguc.carrying_tool.message.no_placing").withStyle(ChatFormatting.RED), true);
          return InteractionResult.FAIL;
        }
      }
    }
  }

  private boolean hasAccess(Player player, Level world, boolean warn) {
    if (world.isClientSide) {
      return MishangucClient.CLIENT_CARRYING_TOOL_ACCESS.get().hasAccess(player);
    } else {
      final MishangucRules.ToolAccess toolAccess = world.getGameRules().getRule(MishangucRules.CARRYING_TOOL_ACCESS).get();
      return toolAccess.hasAccess(player, warn);
    }
  }

  @Override
  public InteractionResult beginAttackBlock(ItemStack stack, Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction, boolean fluidIncluded) {
    if (!hasAccess(player, world, true))
      return InteractionResult.PASS;
    final Block alreadyHolding = getHoldingBlock(stack);
    if (alreadyHolding != null && !player.isCreative()) {
      if (!world.isClientSide) {
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.carrying_tool.message.no_picking", Optional.ofNullable(getHoldingBlock(stack)).map(Block::getName).orElse(TextBridge.empty())).withStyle(ChatFormatting.RED), true);
        return InteractionResult.FAIL;
      } else {
        return InteractionResult.CONSUME;
      }
    }
    final boolean alreadyHoldingEntity = hasHoldingEntity(stack);
    if (alreadyHoldingEntity && !player.isCreative()) {
      if (world.isClientSide)
        return InteractionResult.CONSUME;
      else {
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.carrying_tool.message.no_picking", getEntityName(stack)).withStyle(ChatFormatting.RED), true);
        return InteractionResult.FAIL;
      }
    }
    final BlockState removed = world.getBlockState(pos);
    if (removed.getBlock() instanceof GameMasterBlock && !player.hasPermissions(2)) {
      return InteractionResult.FAIL;
    }
    setHoldingBlockState(stack, removed);
    final BlockEntity blockEntity = world.getBlockEntity(pos);
    if (blockEntity != null) {
      blockEntity.saveToItem(stack);
    } else {
      stack.getOrCreateTag().remove("BlockEntityTag");
    }
    world.removeBlockEntity(pos);
    world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    if (world.isClientSide) {
      world.levelEvent(2001, pos, Block.getId(removed));
    }
    if (!world.isClientSide) {
      if (alreadyHolding == null) {
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.carrying_tool.message.pick", removed.getBlock().getName()), true);
      } else if (alreadyHoldingEntity) {
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.carrying_tool.message.pick_overriding", getEntityName(stack)), true);
      } else {
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.carrying_tool.message.picked_overriding", removed.getBlock().getName(), alreadyHolding.getName()), true);
      }
    }
    setHoldingEntity(stack, null);
    return InteractionResult.SUCCESS;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
    final InteractionResultHolder<ItemStack> use = super.use(world, user, hand);
    if (use.getResult().consumesAction() || !hasAccess(user, world, true)) {
      return use;
    }
    final ItemStack stack = user.getItemInHand(hand);
    final BlockState holdingBlockState = getHoldingBlockState(stack, world);
    if (holdingBlockState != null) {
      if (holdingBlockState.getBlock() instanceof GameMasterBlock && !user.hasPermissions(2)) {
        return InteractionResultHolder.fail(stack);
      }
      if (world.isClientSide)
        return InteractionResultHolder.success(use.getObject());
      final FallingBlockEntity fallingBlockEntity = new FallingBlockEntity(EntityType.FALLING_BLOCK, world);
      CompoundTag nbt = new CompoundTag();
      nbt.put("BlockState", stack.getTagElement("holdingBlockState"));
      fallingBlockEntity.load(nbt);
      final Vec3 eyePos = user.getEyePosition();
      fallingBlockEntity.moveTo(eyePos.x, eyePos.y, eyePos.z, user.getYRot(), user.getXRot());
      fallingBlockEntity.setDeltaMovement(Vec3.directionFromRotation(user.getXRot(), user.getYRot()).scale(2).add(user.getDeltaMovement()));
      fallingBlockEntity.dropItem = true;
      fallingBlockEntity.blockData = stack.getTagElement("BlockEntityTag");
      fallingBlockEntity.setHurtsEntities(holdingBlockState.getBlock().getExplosionResistance(), Integer.MAX_VALUE);
      final boolean spawnEntity = world.addFreshEntity(fallingBlockEntity);
      if (spawnEntity) {
        if (!user.isCreative()) {
          setHoldingBlockState(stack, null);
          stack.getOrCreateTag().remove("BlockEntityTag");
        }
        user.displayClientMessage(TextBridge.translatable(user.isCreative() ? "item.mishanguc.carrying_tool.message.block_thrown_creative" : "item.mishanguc.carrying_tool.message.block_thrown", holdingBlockState.getBlock().getName()), true);
        return InteractionResultHolder.success(use.getObject());
      } else {
        return InteractionResultHolder.fail(use.getObject());
      }
    } else if (hasHoldingEntity(stack)) {
      if (world instanceof ServerLevel serverWorld) {
        final Entity entity = createHoldingEntity(stack, serverWorld, user);
        if (entity == null)
          return use;
        final Vec3 pos = user.position();
        entity.moveTo(pos.x, pos.y, pos.z, user.getYRot(), user.getXRot());
        entity.setDeltaMovement(Vec3.directionFromRotation(user.getXRot(), user.getYRot()).scale(2).add(user.getDeltaMovement()));
        final boolean spawnEntity = world.addFreshEntity(entity);
        if (spawnEntity) {
          user.displayClientMessage(TextBridge.translatable(user.isCreative() ? "item.mishanguc.carrying_tool.message.entity_thrown_creative" : "item.mishanguc.carrying_tool.message.entity_thrown", getEntityName(stack)), true);
          if (!user.isCreative()) {
            setHoldingEntity(stack, null);
          } else {
            setHoldingEntityUUID(stack, Mth.createInsecureUUID());
          }
          return InteractionResultHolder.success(use.getObject());
        } else {
          return InteractionResultHolder.fail(use.getObject());
        }
      } else {
        return InteractionResultHolder.success(use.getObject());
      }
    } else {
      return use;
    }
  }

  @Override
  public @NotNull InteractionResult attackEntityCallback(Player player, Level world, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult) {
    if (!hasAccess(player, world, true) || player.isSpectator())
      return InteractionResult.PASS;
    final ItemStack stack = player.getItemInHand(hand);
    if (entity instanceof Player) {
      if (world.isClientSide) {
        return InteractionResult.PASS;
      } else {
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.carrying_tool.message.pick_player").withStyle(ChatFormatting.RED), false);
        return InteractionResult.FAIL;
      }
    } else if (hasHoldingEntity(stack) && !player.isCreative()) {
      if (world.isClientSide)
        return InteractionResult.SUCCESS;
      else {
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.carrying_tool.message.no_picking", getEntityName(stack)).withStyle(ChatFormatting.RED), true);
        return InteractionResult.FAIL;
      }
    } else if (hasHoldingBlockState(stack) && !player.isCreative()) {
      if (world.isClientSide)
        return InteractionResult.SUCCESS;
      else {
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.carrying_tool.message.no_picking", Optional.ofNullable(getHoldingBlock(stack)).map(Block::getName).orElse(TextBridge.empty())).withStyle(ChatFormatting.RED), true);
        return InteractionResult.FAIL;
      }
    }
    if (!world.isClientSide) {
      if (hasHoldingEntity(stack)) {
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.carrying_tool.message.pick_entity_overriding", entity.getName(), getEntityName(stack)), true);
      } else if (hasHoldingBlockState(stack)) {
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.carrying_tool.message.pick_entity_overriding", entity.getName(), Objects.requireNonNull(getHoldingBlock(stack)).getName()), true);
      } else {
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.carrying_tool.message.pick_entity", entity.getName()), true);
      }
      setHoldingBlockState(stack, null);
      setHoldingEntity(stack, entity);
      entity.remove(Entity.RemovalReason.DISCARDED);
      if (entity instanceof EnderDragonPart enderDragonPart) {
        enderDragonPart.parentMob.kill();
      }
    }
    return InteractionResult.SUCCESS;
  }

  @OnlyIn(Dist.CLIENT)
  @Override
  public boolean renderBlockOutline(Player player, ItemStack itemStack, WorldRenderContext worldRenderContext, WorldRenderContext.BlockOutlineContext blockOutlineContext, InteractionHand hand) {
    if (!hasAccess(player, worldRenderContext.world(), true))
      return true;
    final Minecraft minecraft = Minecraft.getInstance();
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
    final BlockPos pos = blockOutlineContext.blockPos();
    if (hasHoldingBlockState(itemStack)) {
      final BlockPlacementContext blockPlacementContext =
          new BlockPlacementContext(worldRenderContext.world(), pos, player, itemStack, blockHitResult, includesFluid);
      if (blockPlacementContext.canPlace()) {
        WorldRendererInvoker.drawCuboidShapeOutline(matrices, vertexConsumer, blockPlacementContext.stateToPlace.getShape(
            blockPlacementContext.world, blockPlacementContext.posToPlace, CollisionContext.of(player)), blockPlacementContext.posToPlace.getX() - blockOutlineContext.cameraX(), blockPlacementContext.posToPlace.getY() - blockOutlineContext.cameraY(), blockPlacementContext.posToPlace.getZ() - blockOutlineContext.cameraZ(), 0, 1, 1, 0.8f);
        WorldRendererInvoker.drawCuboidShapeOutline(matrices, vertexConsumer, blockPlacementContext
            .stateToPlace
            .getFluidState()
            .getShape(blockPlacementContext.world, blockPlacementContext.posToPlace), blockPlacementContext.posToPlace.getX() - blockOutlineContext.cameraX(), blockPlacementContext.posToPlace.getY() - blockOutlineContext.cameraY(), blockPlacementContext.posToPlace.getZ() - blockOutlineContext.cameraZ(), 0, 0.5f, 1, 0.5f);
      }
    }
    if (hand == InteractionHand.MAIN_HAND && (!hasHoldingBlockState(itemStack) && !hasHoldingEntity(itemStack) || player.isCreative())) {
      final BlockState hitState = worldRenderContext.world().getBlockState(pos);
      // 只有当主手持有此物品时，才绘制红色边框。
      WorldRendererInvoker.drawCuboidShapeOutline(matrices, vertexConsumer, hitState.getShape(
          worldRenderContext.world(), pos, CollisionContext.of(player)), pos.getX() - blockOutlineContext.cameraX(), pos.getY() - blockOutlineContext.cameraY(), pos.getZ() - blockOutlineContext.cameraZ(), 1, 0, 0, 0.8f);
      WorldRendererInvoker.drawCuboidShapeOutline(matrices, vertexConsumer, hitState
          .getFluidState()
          .getShape(worldRenderContext.world(), pos), pos.getX() - blockOutlineContext.cameraX(), pos.getY() - blockOutlineContext.cameraY(), pos.getZ() - blockOutlineContext.cameraZ(), 1, 0.5f, 0, 0.5f);
    }
    return false;
  }

  @OnlyIn(Dist.CLIENT)
  @Override
  public void renderBeforeOutline(WorldRenderContext context, HitResult hitResult, LocalPlayer player, InteractionHand hand) {
    // 只在使用主手且有权限时持有此物品时进行渲染。
    if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !hasAccess(player, context.world(), true))
      return;
    final ItemStack stack = player.getMainHandItem();
    final CompoundTag nbt = stack.getTag();
    final MultiBufferSource consumers = context.consumers();
    final PoseStack matrices = context.matrixStack();
    if (consumers == null)
      return;
    final VertexConsumer vertexConsumer = consumers.getBuffer(RenderType.lines());
    final Vec3 cameraPos = context.camera().getPosition();
    if (hitResult.getType() == HitResult.Type.BLOCK && hasHoldingEntity(stack) && nbt != null) {
      final float width = nbt.getFloat("holdingEntityWidth");
      final float height = nbt.getFloat("holdingEntityHeight");
      final Vec3 pos = hitResult.getLocation();
      WorldRendererInvoker.drawCuboidShapeOutline(matrices, vertexConsumer, Shapes.create(pos.x - width / 2, pos.y, pos.z - width / 2, pos.x + width / 2, pos.y + height, pos.z + width / 2), -cameraPos.x, -cameraPos.y, -cameraPos.z, 0, 1, 1, 0.8f);
    }
    if (!player.isCreative() && (hasHoldingBlockState(stack) || hasHoldingEntity(stack)))
      return;
    if (hitResult instanceof EntityHitResult entityHitResult) {
      final Entity entity = entityHitResult.getEntity();
      WorldRendererInvoker.drawCuboidShapeOutline(matrices, vertexConsumer, Shapes.create(entity.getBoundingBox()), -cameraPos.x, -cameraPos.y, -cameraPos.z, 1.0f, 0f, 0f, 0.8f);
    }
  }
}
