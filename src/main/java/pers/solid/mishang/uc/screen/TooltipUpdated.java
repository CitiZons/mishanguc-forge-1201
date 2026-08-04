package pers.solid.mishang.uc.screen;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 这个接口主要用于 {@link BooleanButtonWidget} 和 {@link FloatButtonWidget}。当更改选择的对象时，更新其 tooltip。
 */
@OnlyIn(Dist.CLIENT)
public interface TooltipUpdated {
  void updateTooltip();
}
