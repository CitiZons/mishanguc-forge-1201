package pers.solid.mishang.uc.util;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * <p>The click event that, when clicked, shows the player another {@link Text}.</p>
 *
 * @see pers.solid.mishang.uc.mixin.ScreenMixin#handleTextClickMixin(Style, CallbackInfoReturnable)
 * @since 0.1.7 This class is designed for minecraft-only, as it is related to minecraft-side clicking actions, and it cannot be serialized as JSON.
 */
public class TextClickEvent extends ClickEvent {
  public final Component text;

  public TextClickEvent(Component text) {
    super(Action.RUN_COMMAND, "/");
    this.text = text;
  }
}
