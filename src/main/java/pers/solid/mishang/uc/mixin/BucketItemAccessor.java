package pers.solid.mishang.uc.mixin;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.BucketItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BucketItem.class)
public interface BucketItemAccessor {
  @Accessor("content")
  Fluid getFluid();
}
