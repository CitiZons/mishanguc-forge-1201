package pers.solid.mishang.uc.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Direction;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;
import pers.solid.mishang.uc.block.HungSignBlock;
import pers.solid.mishang.uc.blockentity.HungSignBlockEntity;
import pers.solid.mishang.uc.text.TextContext;

import java.util.List;
import java.util.Map;

/**
 * @see pers.solid.mishang.uc.block.HungSignBlock
 * @see HungSignBlockEntity
 */
@OnlyIn(Dist.CLIENT)
public class HungSignBlockEntityRenderer<T extends HungSignBlockEntity> implements BlockEntityRenderer<T> {

  private final BlockEntityRendererProvider.Context ctx;

  public HungSignBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
    this.ctx = ctx;
  }

  @Override
  public void render(
      HungSignBlockEntity entity,
      float tickDelta,
      PoseStack matrices,
      MultiBufferSource vertexConsumers,
      int light,
      int overlay) {
    matrices.translate(0.5, 9 / 16f, 0.5);
    final Direction.Axis axis = entity.getBlockState().getValue(HungSignBlock.AXIS);
    for (Map.Entry<@NotNull Direction, @NotNull List<@NotNull TextContext>> entry :
        entity.texts.entrySet()) {
      final Direction direction = entry.getKey();
      final List<@NotNull TextContext> textContexts = entry.getValue();
      if (direction.getAxis() != axis) {
        continue;
      }
      final boolean glowing = entity.glowing.contains(direction);
      matrices.pushPose();
      matrices.mulPose(Axis.YP.rotationDegrees(-direction.toYRot()));
      matrices.translate(0, 0, 1.0125 / 32f);
      matrices.scale(1 / 16f, -1 / 16f, 1 / 16f);
      for (TextContext textContext : textContexts) {
        textContext.draw(ctx.getFont(), matrices, vertexConsumers, glowing ? 15728880 : light, 16, entity.getHeight());
      }
      matrices.popPose();
    }
  }
}
