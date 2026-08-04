package pers.solid.mishang.uc.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pers.solid.mishang.uc.blockentity.ColoredBlockEntity;

import java.util.Arrays;
import java.util.List;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {
  @Unique
  private static final TagKey<Block> TINTS_BEACON_BEAMS = TagKey.create(Registries.BLOCK, new ResourceLocation("mishanguc", "tints_beacon_beams"));

  @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
  private static void acceptColoredBlocksInTick(Level world, BlockPos pos, BlockState state, BeaconBlockEntity blockEntity, CallbackInfo ci, int i, int j, int k, BlockPos blockPos, BeaconBlockEntity.BeaconBeamSection beamSegment, int l, int m, BlockState blockState, @Share("is_colored") LocalBooleanRef localBooleanRef, @Local LocalRef<BeaconBlockEntity.BeaconBeamSection> beamSegmentLocalRef) {
    final List<BeaconBlockEntity.BeaconBeamSection> checkingBeamSegments = ((BeaconBlockEntityAccessor) blockEntity).getCheckingBeamSegments();
    if (world.getBlockEntity(blockPos) instanceof ColoredBlockEntity coloredBlockEntity && blockState.is(TINTS_BEACON_BEAMS)) {
      localBooleanRef.set(true);
      int color = coloredBlockEntity.getColor();
      float[] fs = {(color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f};
      if (checkingBeamSegments.size() <= 1) {
        beamSegment = new BeaconBlockEntity.BeaconBeamSection(fs);
        beamSegmentLocalRef.set(beamSegment);
        checkingBeamSegments.add(beamSegment);
      } else if (beamSegment != null) {
        final float[] beamSegmentColor = beamSegment.getColor();
        if (Arrays.equals(fs, beamSegmentColor)) {
          ((BeaconBlockEntityAccessor.BeamSegmentAccessor) beamSegment).invokeIncreaseHeight();
        } else {
          beamSegment = new BeaconBlockEntity.BeaconBeamSection(new float[]{(beamSegmentColor[0] + fs[0]) / 2.0F, (beamSegmentColor[1] + fs[1]) / 2.0F, (beamSegmentColor[2] + fs[2]) / 2.0F});
          beamSegmentLocalRef.set(beamSegment);
          checkingBeamSegments.add(beamSegment);
        }
      }
    } else {
      localBooleanRef.set(false);
    }
  }

  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BeaconBlockEntity$BeaconBeamSection;increaseHeight()V"))
  private static boolean wrappedIncreaseHeight(BeaconBlockEntity.BeaconBeamSection instance, @Share("is_colored") LocalBooleanRef localBooleanRef) {
    return !localBooleanRef.get();
  }
}
