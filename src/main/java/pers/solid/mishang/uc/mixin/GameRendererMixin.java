package pers.solid.mishang.uc.mixin;

import net.minecraft.world.phys.HitResult;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pers.solid.mishang.uc.item.BlockToolItem;
import net.minecraft.client.player.LocalPlayer;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
  @Shadow
  @Final
  Minecraft minecraft;

  @ModifyArg(
      method = "pick(F)V",
      at =
      @At(
          value = "INVOKE",
          target =
              "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"),
      index = 2)
  private boolean modifyRaycastCall(boolean includeFluids) {
    //        return true;
    final LocalPlayer player = this.minecraft.player;
    if (player == null) {
      return includeFluids;
    }
    final ItemStack itemStack =
        player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()
            ? player.getItemInHand(InteractionHand.OFF_HAND)
            : player.getItemInHand(InteractionHand.MAIN_HAND);
    final Item item = itemStack.getItem();
    if (item instanceof final BlockToolItem blockToolItem) {
      return blockToolItem.includesFluid(itemStack, player.isShiftKeyDown());
    } else {
      return includeFluids;
    }
  }
}
