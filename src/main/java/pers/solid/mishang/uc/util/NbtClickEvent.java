package pers.solid.mishang.uc.util;

import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;

/**
 * The click event that, when clicked, shows the player a prettified NBT.
 *
 * @see pers.solid.mishang.uc.mixin.ScreenMixin#handleTextClickMixin
 * @since 0.1.7 This class is designed for minecraft-only, as it is related to minecraft-side clicking actions, and it cannot be serialized as JSON.
 */
public class NbtClickEvent extends ClickEvent {
  public final Tag nbt;

  public NbtClickEvent(Tag nbt) {
    super(Action.RUN_COMMAND, "/");
    this.nbt = nbt;
  }
}
