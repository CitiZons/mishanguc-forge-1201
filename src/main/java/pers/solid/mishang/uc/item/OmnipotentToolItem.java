package pers.solid.mishang.uc.item;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Tier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.util.TextBridge;

import java.util.List;

public class OmnipotentToolItem extends DiggerItem implements MishangucItem, InteractsWithEntity {
  protected static final OmnipotentToolMaterial MATERIAL = new OmnipotentToolMaterial();

  public OmnipotentToolItem(Properties settings) {
    super(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, MATERIAL, TagKey.create(Registries.BLOCK, new ResourceLocation("minecraft", "mineable/pickaxe")), settings);
  }

  @Override
  public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    super.appendHoverText(stack, world, tooltip, context);
    tooltip.add(TextBridge.translatable("item.mishanguc.omnipotent_tool.tooltip.1", TextBridge.keybind("key.attack").withStyle(style -> style.withColor(0xdddddd))).withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.omnipotent_tool.tooltip.2", TextBridge.keybind("key.use").withStyle(style -> style.withColor(0xdddddd))).withStyle(ChatFormatting.GRAY));
  }

  @Override
  public boolean isCorrectToolForDrops(BlockState state) {
    return true;
  }

  @Override
  public float getDestroySpeed(ItemStack stack, BlockState state) {
    return Float.POSITIVE_INFINITY;
  }

  @Override
  public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
    if (world instanceof ServerLevel serverWorld) {
      serverWorld.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 32, 0.5, 0.5, 0.5, 0);
    }
    return super.mineBlock(stack, world, state, pos, miner);
  }

  @Override
  public @NotNull InteractionResult useEntityCallback(Player player, Level world, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult) {
    if (entity instanceof LivingEntity livingEntity) {
      livingEntity.heal(Float.POSITIVE_INFINITY);
      if (world instanceof ServerLevel serverWorld) {
        serverWorld.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY(), entity.getZ(), 32, 0.5, 0.5, 0.5, 0.5);
      }
    }
    return InteractionResult.SUCCESS;
  }

  @Override
  public @NotNull InteractionResult attackEntityCallback(Player player, Level world, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult) {
    if (world instanceof ServerLevel serverWorld) {
      serverWorld.sendParticles(ParticleTypes.LARGE_SMOKE, entity.getX(), entity.getY(), entity.getZ(), 32, 0.5, 0.5, 0.5, 0.5);
    }
    return InteractionResult.PASS;
  }

  @Override
  public ItemStack getDefaultInstance() {
    final ItemStack defaultStack = super.getDefaultInstance();
    defaultStack.getOrCreateTag().putBoolean("Unbreakable", true);
    return defaultStack;
  }

  protected static class OmnipotentToolMaterial implements Tier {
    private OmnipotentToolMaterial() {
    }

    @Override
    public int getUses() {
      return Integer.MAX_VALUE;
    }

    @Override
    public float getSpeed() {
      return Float.POSITIVE_INFINITY;
    }

    @Override
    public float getAttackDamageBonus() {
      return Float.POSITIVE_INFINITY;
    }

    @Override
    public int getLevel() {
      return Integer.MAX_VALUE;
    }

    @Override
    public int getEnchantmentValue() {
      return Integer.MAX_VALUE;
    }

    @Override
    public Ingredient getRepairIngredient() {
      return Ingredient.of(Items.BEDROCK);
    }
  }
}
