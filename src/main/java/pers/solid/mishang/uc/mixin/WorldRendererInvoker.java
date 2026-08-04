package pers.solid.mishang.uc.mixin;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@OnlyIn(Dist.CLIENT)
@Mixin(LevelRenderer.class)
public interface WorldRendererInvoker {
  /**
   * 在指定位置渲染指定外观。
   *
   * @see LevelRenderer
   */
  @Invoker("renderShape")
  static void drawCuboidShapeOutline(
      PoseStack matrices,
      VertexConsumer vertexConsumer,
      VoxelShape shape,
      double offsetX,
      double offsetY,
      double offsetZ,
      float red,
      float green,
      float blue,
      float alpha) {
    throw new AssertionError();
  }
}
