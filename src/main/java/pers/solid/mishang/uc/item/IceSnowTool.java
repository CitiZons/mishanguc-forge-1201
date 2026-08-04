package pers.solid.mishang.uc.item;

import net.minecraft.world.level.block.LiquidBlock;

import net.minecraft.world.level.block.SnowLayerBlock;

import net.minecraft.world.level.block.*;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.util.TextBridge;

import java.util.List;
import net.minecraft.world.level.block.state.BlockState;

public class IceSnowTool extends Item implements MishangucItem, DispenseItemBehavior, HotbarScrollInteraction {
  public IceSnowTool(Properties settings) {
    super(settings);
    DispenserBlock.registerBehavior(this, this);
  }

  @Override
  public Component getName(ItemStack stack) {
    return TextBridge.translatable("item.mishanguc.ice_snow_tool.format", getDescription(), Integer.toString(getStrength(stack)));
  }

  @Override
  public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    super.appendHoverText(stack, world, tooltip, context);
    tooltip.add(TextBridge.translatable("item.mishanguc.ice_snow_tool.tooltip.1").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.ice_snow_tool.tooltip.2").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.ice_snow_tool.tooltip.3").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.ice_snow_tool.tooltip.4").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.ice_snow_tool.tooltip.strength", TextBridge.literal(Integer.toString(getStrength(stack))).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
    final ItemStack stack = user.getItemInHand(hand);
    if (!(world instanceof ServerLevel serverWorld))
      return InteractionResultHolder.success(stack);
    final HitResult hitResult = user.pick(64, 0, false);
    if (hitResult.getType() == HitResult.Type.MISS)
      return InteractionResultHolder.fail(stack);
    final Vec3 pos = hitResult.getLocation();
    final int strength = getStrength(stack);
    if (user.isShiftKeyDown()) {
      applyHeat(serverWorld, pos, strength);
    } else {
      applyIce(serverWorld, pos, strength);
    }
    stack.hurtAndBreak(strength + 1, user, playerEntity -> playerEntity.broadcastBreakEvent(hand));
    return InteractionResultHolder.success(stack);
  }

  /**
   * @see IceBlock
   * @see ServerLevel#tickChunk
   */
  public void applyIce(@NotNull ServerLevel world, @NotNull Vec3 pos, int strength) {
    final float probability = getProbability(strength);
    final int range = getRange(strength);
    final BlockPos centerBlockPos = BlockPos.containing(pos);
    for (final BlockPos blockPos : BlockPos.betweenClosed(centerBlockPos.offset(-range,0,-range), centerBlockPos.offset(range,0,range))) {
      if (world.random.nextFloat() > probability) {
        continue;
      }

      final BlockPos topBlockPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPos);

      // 结冰
      final boolean isInsufficientBlockLight = world.getBrightness(LightLayer.BLOCK, topBlockPos) < 10;
      final BlockPos waterBlockPos = topBlockPos.below();
      final boolean isWaterInRange = centerBlockPos.getY() - range <= waterBlockPos.getY() && blockPos.getY() <= centerBlockPos.getY() + range;
      final boolean isWater = isWaterInRange && isInsufficientBlockLight && world.getBlockState(waterBlockPos).getBlock() instanceof LiquidBlock && world.getFluidState(waterBlockPos).getType() == Fluids.WATER;
      if (isWater) {
        world.setBlockAndUpdate(waterBlockPos, Blocks.ICE.defaultBlockState());
      }

      // 模拟降雪
      final boolean isSnowInRange = centerBlockPos.getY() - range <= topBlockPos.getY() && topBlockPos.getY() <= centerBlockPos.getY() + range;
      final int snowAccumulationHeight = world.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT);
      if (snowAccumulationHeight > 0 && isInsufficientBlockLight && isSnowInRange && Blocks.SNOW.defaultBlockState().canSurvive(world, topBlockPos)) {
        final BlockState blockState = world.getBlockState(topBlockPos);
        if (blockState.is(Blocks.SNOW)) {
          int layers = blockState.getValue(SnowLayerBlock.LAYERS);
          if (layers < Math.min(snowAccumulationHeight, 8)) {
            BlockState blockState2 = blockState.setValue(SnowLayerBlock.LAYERS, layers + 1);
            Block.pushEntitiesUp(blockState, blockState2, world, topBlockPos);
            world.setBlockAndUpdate(topBlockPos, blockState2);
          }
        } else if (blockState.isAir()) {
          world.setBlockAndUpdate(topBlockPos, Blocks.SNOW.defaultBlockState());
        }
      }
    }
    world.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, (int) Math.pow((range * 2 + 1), 3) / 16, range, range, range, 0);
  }

  public void applyHeat(@NotNull ServerLevel world, @NotNull Vec3 pos, int strength) {
    final float probability = getProbability(strength);
    final int range = getRange(strength);
    for (BlockPos blockPos : BlockPos.betweenClosed(BlockPos.containing(pos).offset(-range,-range,-range), BlockPos.containing(pos).offset(range,range,range))) {
      if (world.random.nextFloat() > probability) {
        continue;
      }

      // 结冰
      final BlockState blockState = world.getBlockState(blockPos);
      if (blockState.getBlock() instanceof IceBlock) {
        if (world.dimensionType().ultraWarm()) {
          world.removeBlock(blockPos, false);
        } else {
          world.setBlockAndUpdate(blockPos, Blocks.WATER.defaultBlockState());
          world.neighborChanged(blockPos, Blocks.WATER.defaultBlockState().getBlock(), blockPos);
        }
      }

      // 模拟降雪
      if (blockState.is(Blocks.SNOW)) {
        Block.dropResources(blockState, world, blockPos);
        world.removeBlock(blockPos, false);
      }
    }
    world.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, (int) Math.pow((range * 2 + 1), 3) / 16, range, range, range, 0);
  }

  @Override
  public ItemStack dispense(BlockSource pointer, ItemStack stack) {
    final int strength = getStrength(stack);
    applyIce(pointer.getLevel(), pointer.getPos().relative(pointer.getBlockState().getValue(DispenserBlock.FACING), getRange(strength)).getCenter(), strength);
    stack.hurtAndBreak(strength + 1, (net.minecraft.world.entity.LivingEntity) null, e -> {});
    return stack;
  }

  public static int getStrength(ItemStack stack) {
    final CompoundTag nbt = stack.getTag();
    return nbt == null || !nbt.contains("strength", Tag.TAG_ANY_NUMERIC) ? 4 : Mth.clamp(nbt.getInt("strength"), 0, 10);
  }

  public static float getProbability(int strength) {
    return Mth.clamp(0.7f + strength * 0.1f, 0.7f, 1f);
  }

  public static int getRange(int strength) {
    return Mth.clamp(4 + strength * strength / 2, 4, 64);
  }

  @Override
  public void onScroll(int selectedSlot, double scrollAmount, ServerPlayer player, ItemStack stack) {
    final int strength = getStrength(stack);
    final int newStrength = Mth.positiveModulo(strength - (int) scrollAmount, 8);
    stack.getOrCreateTag().putInt("strength", newStrength);
  }
}
