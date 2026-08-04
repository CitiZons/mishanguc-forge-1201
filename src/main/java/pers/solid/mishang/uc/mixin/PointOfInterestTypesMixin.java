package pers.solid.mishang.uc.mixin;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.Set;

@Mixin(PoiTypes.class)
public abstract class PointOfInterestTypesMixin {
  @Redirect(method = "bootstrap", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/village/poi/PoiTypes;getBlockStates(Lnet/minecraft/world/level/block/Block;)Ljava/util/Set;", ordinal = 0), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/Blocks;NETHER_PORTAL:Lnet/minecraft/world/level/block/Block;")))
  private static Set<BlockState> injected2(Block block) {
    // 通过注册表按 id 查找，避免在 vanilla bootstrap 阶段强制加载 ColoredBlocks 类
    // （否则会在模组注册其方块之前就实例化它们，导致 intrusive holder 泄漏）。
    final Block coloredNetherPortal = BuiltInRegistries.BLOCK.get(new ResourceLocation("mishanguc", "colored_nether_portal"));
    if (coloredNetherPortal == Blocks.AIR) {
      return ImmutableSet.copyOf(block.getStateDefinition().getPossibleStates());
    }
    return ImmutableSet.copyOf(Iterables.concat(block.getStateDefinition().getPossibleStates(), coloredNetherPortal.getStateDefinition().getPossibleStates()));
  }
}
