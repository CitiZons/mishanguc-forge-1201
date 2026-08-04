package pers.solid.mishang.uc.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.mishang.uc.item.HotbarScrollInteraction;

@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {
  @Shadow
  public abstract ItemStack getSelected();

  @Shadow
  public int selected;

  @Inject(method = "swapPaint", at = @At("HEAD"), cancellable = true)
  public void lockSelection(double scrollAmount, CallbackInfo ci) {
    final ItemStack mainHandStack = this.getSelected();
    if (mainHandStack.getItem() instanceof HotbarScrollInteraction interaction && interaction.shouldLockScroll(selected, scrollAmount)) {
      ci.cancel();
    }
  }
}
