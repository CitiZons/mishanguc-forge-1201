package pers.solid.mishang.uc.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.mishang.uc.blocks.ColoredBlocks;

/**
 * 让模组的彩色下界传送门方块（{@link ColoredBlocks#COLORED_NETHER_PORTAL}）被原版传送门
 * 形状检测视为有效的内部方块。
 *
 * <p>原版 {@code PortalShape} 在计算传送门宽度与高度时，均通过静态方法 {@code isEmpty(BlockState)}
 * 判断某个位置是否为有效的传送门内部（空气 / 火焰 / 下界传送门）。因此只需在该方法注入一次，
 * 即可同时覆盖宽度和高度检测，无需脆弱的 {@code @ModifyExpressionValue} + {@code @Local} 注入。
 */
@Mixin(PortalShape.class)
public abstract class AreaHelperMixin {
  @Inject(method = "isEmpty", at = @At("RETURN"), cancellable = true)
  private static void validColoredPortal(BlockState state, CallbackInfoReturnable<Boolean> cir) {
    if (!cir.getReturnValueZ() && state.is(ColoredBlocks.COLORED_NETHER_PORTAL)) {
      cir.setReturnValue(true);
    }
  }
}
