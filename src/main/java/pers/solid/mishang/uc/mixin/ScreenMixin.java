package pers.solid.mishang.uc.mixin;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.mishang.uc.util.NbtClickEvent;
import pers.solid.mishang.uc.util.NbtPrettyPrinter;
import pers.solid.mishang.uc.util.TextClickEvent;

@OnlyIn(Dist.CLIENT)
@Mixin(Screen.class)
public class ScreenMixin {
  @Shadow
  @Nullable
  protected Minecraft minecraft;

  /**
   * This injection is used for an extended "clickEvent" of JSON string. It does not add to an enum
   * element, but instead, uses {@link TextClickEvent} that extends vanilla {@link ClickEvent}s.
   */
  @Inject(
      method = "handleComponentClicked",
      at = @At("HEAD"),
      cancellable = true)
  public void handleTextClickMixin(Style style, CallbackInfoReturnable<Boolean> cir) {
    final ClickEvent clickEvent = style.getClickEvent();
    if (clickEvent instanceof final TextClickEvent textClickEvent && minecraft != null && minecraft.player != null) {
      this.minecraft.player.sendSystemMessage(
          textClickEvent.text);
      cir.setReturnValue(true);
      cir.cancel();
    } else if (clickEvent instanceof final NbtClickEvent nbtClickEvent && minecraft != null && minecraft.player != null) {
      this.minecraft.player.sendSystemMessage(
          NbtPrettyPrinter.serialize(nbtClickEvent.nbt));
      cir.setReturnValue(true);
      cir.cancel();
    }
  }
}
