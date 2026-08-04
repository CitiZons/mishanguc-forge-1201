package pers.solid.mishang.uc.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.mishang.uc.item.ForcePlacingToolItem;

@Mixin(LevelChunk.class)
public class WorldChunkMixin {
  @WrapWithCondition(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;onPlace(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V"))
  public boolean wrappedCallOnBlockAdded(BlockState instance, Level world, BlockPos pos, BlockState state, boolean notify) {
    return !ForcePlacingToolItem.suppressOnBlockAdded;
  }
}
