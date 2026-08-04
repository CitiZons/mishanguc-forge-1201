package pers.solid.mishang.uc.mixin;

import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerEventHandler.class)
public interface ContainerWidgetAccessor {
  @Accessor("focused")
  void setFocusedElement(GuiEventListener focusedElement);
}
