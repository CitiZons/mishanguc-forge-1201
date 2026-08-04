package pers.solid.mishang.uc.mixin;

import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Font.class)
public interface TextRendererAccessor {
  @Invoker("getFontSet")
  FontSet invokeGetFontSet(ResourceLocation id);
}
