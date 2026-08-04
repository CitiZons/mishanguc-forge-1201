package pers.solid.mishang.uc.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.mishang.uc.block.HandrailCentralBlock;

@Mixin(FenceBlock.class)
public class FenceBlockMixin {
  @Inject(method = "connectsTo", at = @At("RETURN"), cancellable = true)
  private void modifiedCanConnect(BlockState state, boolean neighborIsFullSquare, Direction dir, CallbackInfoReturnable<Boolean> cir) {
    if (HandrailCentralBlock.connectsHandrailTo(dir, state)) cir.setReturnValue(true);
  }
}
