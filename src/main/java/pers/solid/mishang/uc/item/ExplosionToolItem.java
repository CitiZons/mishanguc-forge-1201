package pers.solid.mishang.uc.item;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;

import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangucRules;
import pers.solid.mishang.uc.util.TextBridge;

import java.util.List;

public class ExplosionToolItem extends Item implements HotbarScrollInteraction, DispenseItemBehavior {
  public ExplosionToolItem(Properties settings) {
    super(settings);
    DispenserBlock.registerBehavior(this, this);
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
    final ItemStack stack = user.getItemInHand(hand);
    final HitResult raycast = user.pick(128, 0, user.isShiftKeyDown());
    if (raycast.getType() == HitResult.Type.MISS) {
      return InteractionResultHolder.fail(stack);
    }
    if (world.isClientSide) {
      return InteractionResultHolder.pass(stack);
    }
    if (!world.getGameRules().getRule(MishangucRules.EXPLOSION_TOOL_ACCESS).get().hasAccess(user, true)) {
      return InteractionResultHolder.pass(super.use(world, user, hand).getObject());
    }
    final Vec3 pos = raycast.getLocation();
    final GameRules.BooleanValue booleanRule = world.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS);
    final boolean backup = booleanRule.get();
    if (user.isCreative()) {
      // 创造模式下，将游戏规则临时设为不掉落。
      booleanRule.set(false, null);
    }
    Explosion explosion = new Explosion(world, user, user.isShiftKeyDown() ? world.damageSources().explosion(null) : null, null, pos.x, pos.y, pos.z, power(stack), createFire(stack), destructionType(stack));
    explosion.explode();
    explosion.finalizeExplosion(true);

    // 适用于 1.19.3，因为不是通过 world.createExplosion 实现的，没有向客户端发送消息，所以需要在这里手动发送
    if (!explosion.interactsWithBlocks()) {
      explosion.clearToBlow();
    }
    for (Player playerEntity : world.players()) {
      ServerPlayer serverPlayerEntity = (ServerPlayer) playerEntity;
      if (serverPlayerEntity.distanceToSqr(pos.x, pos.y, pos.z) < 4096.0) {
        serverPlayerEntity.connection.send(new ClientboundExplodePacket(pos.x, pos.y, pos.z, power(stack), explosion.getToBlow(), explosion.getHitPlayers().get(serverPlayerEntity)));
      }
    }
    stack.hurtAndBreak((int) power(stack), user, e -> e.broadcastBreakEvent(hand));
    if (user.isCreative()) {
      booleanRule.set(backup, null);
    }
    return InteractionResultHolder.success(stack);
  }

  @Override
  public Component getName(ItemStack stack) {
    return TextBridge.translatable(getDescriptionId(stack) + ".formatted", power(stack), TextBridge.translatable("item.mishanguc.explosion_tool.createFire." + createFire(stack)), TextBridge.translatable("item.mishanguc.explosion_tool.destructionType." + destructionType(stack).name().toLowerCase()));
  }

  /**
   * @return 该物品产生的爆炸类型。
   */
  public Explosion.BlockInteraction destructionType(ItemStack stack) {
    final String destructionType = stack.getOrCreateTag().getString("destructionType");
    return switch (destructionType) {
      case "none", "keep" -> Explosion.BlockInteraction.KEEP;
      case "destroy_with_decay", "destroy" -> Explosion.BlockInteraction.DESTROY_WITH_DECAY;
      default -> Explosion.BlockInteraction.DESTROY;
    };
  }

  /**
   * @return 物品产生爆炸时，是否造成火焰。
   */
  public boolean createFire(ItemStack stack) {
    return stack.getOrCreateTag().getBoolean("createFire");
  }

  /**
   * @return 该物品的爆炸力量，用于在爆炸时使用。默认为 4。
   */
  public float power(ItemStack stack) {
    final CompoundTag nbt = stack.getOrCreateTag();
    return nbt.contains("power", Tag.TAG_ANY_NUMERIC) ? Mth.clamp(nbt.getFloat("power"), -128, 128) : 4;
  }

  public void appendToEntries(CreativeModeTab.Output stacks) {
    stacks.accept(new ItemStack(this));
    ItemStack stack = new ItemStack(this);
    stack.getOrCreateTag().putBoolean("createFire", true);
    stacks.accept(stack);

    stack = new ItemStack(this);
    stack.getOrCreateTag().putString("destructionType", "keep");
    stacks.accept(stack);

    stack = new ItemStack(this);
    stack.getOrCreateTag().putString("destructionType", "destroy_with_decay");
    stacks.accept(stack);
  }

  @Override
  public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    super.appendHoverText(stack, world, tooltip, context);
    tooltip.add(TextBridge.translatable("item.mishanguc.explosion_tool.tooltip.1", TextBridge.keybind("key.use").withStyle(style -> style.withColor(0xdddddd))).withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.explosion_tool.tooltip.2").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.explosion_tool.tooltip.3").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.explosion_tool.tooltip.4").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.explosion_tool.tooltip.5").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.explosion_tool.tooltip.power", TextBridge.literal(String.valueOf(power(stack))).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.explosion_tool.tooltip.createFire", createFire(stack) ? CommonComponents.GUI_YES.copy().withStyle(ChatFormatting.GREEN) : CommonComponents.GUI_NO.copy().withStyle(ChatFormatting.RED)).withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.explosion_tool.tooltip.destructionType", TextBridge.translatable("item.mishanguc.explosion_tool.destructionType." + destructionType(stack).name().toLowerCase()).withStyle(style -> style.withColor(0x779999))).withStyle(ChatFormatting.GRAY));
  }

  @Override
  public void onScroll(int selectedSlot, double scrollAmount, ServerPlayer player, ItemStack stack) {
    final boolean creative = player.isCreative();
    final float power = Mth.clamp(power(stack) - (float) scrollAmount, creative ? -128 : 0, creative ? 128 : 64);
    stack.getOrCreateTag().putFloat("power", power);
  }

  @Override
  public ItemStack dispense(BlockSource pointer, ItemStack stack) {
    final ServerLevel world = pointer.getLevel();
    if (!world.getGameRules().getRule(MishangucRules.EXPLOSION_TOOL_ACCESS).get().hasAccess(null)) {
      return stack;
    }
    final BlockPos basePos = pointer.getPos();
    final Direction direction = pointer.getBlockState().getValue(DispenserBlock.FACING);
    for (int i = 1; i < 33; i++) {
      final BlockPos pos = basePos.relative(direction, i);
      if (world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
          && world.getEntitiesOfClass(Entity.class, new AABB(pos), EntitySelector.NO_SPECTATORS.and(Entity::isPickable).and(EntityFlagsPredicate.Builder.flags().setCrouching(false).build()::matches)).isEmpty()
      ) {
        continue;
      }
      Explosion explosion = new Explosion(world, null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power(stack), createFire(stack), destructionType(stack));
      explosion.explode();
      explosion.finalizeExplosion(true);
      // 适用于 1.19.3，因为不是通过 world.createExplosion 实现的，没有向客户端发送消息，所以需要在这里手动发送
      if (!explosion.interactsWithBlocks()) {
        explosion.clearToBlow();
      }
      for (ServerPlayer playerEntity : world.players()) {
        if (playerEntity.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 4096.0) {
          playerEntity.connection.send(new ClientboundExplodePacket(pos.getX(), pos.getY(), pos.getZ(), power(stack), explosion.getToBlow(), explosion.getHitPlayers().get(playerEntity)));
        }
      }
      stack.hurtAndBreak((int) power(stack), (net.minecraft.world.entity.LivingEntity) null, e -> stack.setCount(0));
    }
    return stack;
  }
}
