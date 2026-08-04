package pers.solid.mishang.uc.render;

import it.unimi.dsi.fastutil.booleans.BooleanSet;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.ApiStatus;
import pers.solid.mishang.uc.block.StandingSignBlock;
import pers.solid.mishang.uc.blockentity.StandingSignBlockEntity;
import pers.solid.mishang.uc.text.TextContext;

@ApiStatus.AvailableSince("1.0.2")
@OnlyIn(Dist.CLIENT)
public record StandingSignBlockEntityRenderer<T extends StandingSignBlockEntity>(BlockEntityRendererProvider.Context ctx) implements BlockEntityRenderer<T> {

  @Override
  public void render(T entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
    final BooleanSet glowing = entity.glowing;
    matrices.translate(0.5, 0.75, 0.5);
    final BlockState state = entity.getBlockState();
    final int rotation = state.getValue(StandingSignBlock.ROTATION);
    matrices.mulPose(Axis.YP.rotationDegrees(-rotation * 22.5f));
    matrices.scale(1 / 16f, -1 / 16f, 1 / 16f);

    matrices.pushPose();
    matrices.translate(0, 0, 0.5125);
    for (TextContext textContext : entity.frontTexts) {
      textContext.draw(ctx.getFont(), matrices, vertexConsumers, glowing.contains(true) ? 15728880 : light, 16, entity.getHeight());
    }
    matrices.popPose();
    matrices.pushPose();
    matrices.translate(0, 0, -0.5125);
    matrices.mulPose(Axis.YP.rotationDegrees(180));
    for (TextContext textContext : entity.backTexts) {
      textContext.draw(ctx.getFont(), matrices, vertexConsumers, glowing.contains(false) ? 15728880 : light, 16, entity.getHeight());
    }
    matrices.popPose();
  }
}
