package pers.solid.mishang.uc.render;

import com.google.common.collect.ImmutableSet;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.Direction;
import com.mojang.math.Axis;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.mishang.uc.block.WallSignBlock;
import pers.solid.mishang.uc.blockentity.WallSignBlockEntity;
import pers.solid.mishang.uc.blocks.WallSignBlocks;
import pers.solid.mishang.uc.mixin.WorldRendererInvoker;
import pers.solid.mishang.uc.text.TextContext;

import java.util.Collection;
import net.minecraft.client.player.LocalPlayer;

@OnlyIn(Dist.CLIENT)
public class WallSignBlockEntityRenderer<T extends WallSignBlockEntity> implements BlockEntityRenderer<T> {

  /**
   * 这个集合中的方块，在渲染时是视为没有厚度的，直接渲染在靠墙的位置，而不是离墙 1 格的位置。
   */
  private static final @Unmodifiable Collection<Block> INVISIBLE_BLOCKS =
      ImmutableSet.of(WallSignBlocks.INVISIBLE_WALL_SIGN, WallSignBlocks.INVISIBLE_GLOWING_WALL_SIGN);

  private final BlockEntityRendererProvider.Context ctx;

  public WallSignBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
    this.ctx = ctx;
  }

  @Override
  public void render(T entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
    final Block block = entity.getBlockState().getBlock();
    // 若方块为隐形方块，且玩家手中拿着该方块，则显示该方块轮廓。
    final LocalPlayer player = Minecraft.getInstance().player;
    if (INVISIBLE_BLOCKS.contains(block) && player != null) {
      final Item mainHandStackItem = player.getMainHandItem().getItem();
      if (mainHandStackItem instanceof final BlockItem blockItem
          && INVISIBLE_BLOCKS.contains(blockItem.getBlock())) {
        boolean glowing = entity.getBlockState().is(WallSignBlocks.INVISIBLE_GLOWING_WALL_SIGN);
        WorldRendererInvoker.drawCuboidShapeOutline(
            matrices,
            vertexConsumers.getBuffer(RenderType.LINES),
            entity.getBlockState().getShape(entity.getLevel(), entity.getBlockPos(), CollisionContext.of(player)),
            0,
            0,
            0,
            glowing ? 0.9f : 0.3f,
            0.8f,
            glowing ? 0.3f : 0.9f,
            0.9f);
      }
    }

    if (entity.glowing) {
      light = 15728880;
    }
    matrices.translate(0.5, 0.5, 0.5);
    final BlockState state = entity.getBlockState();
    final Direction facing = state.getValue(WallSignBlock.FACING);
    final AttachFace face = state.getValue(WallSignBlock.FACE);
    matrices.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
    matrices.mulPose(
        Axis.XP.rotationDegrees(
            face == AttachFace.CEILING ? 90 : face == AttachFace.FLOOR ? -90 : 0));
    if (face != AttachFace.WALL) {
      matrices.mulPose(Axis.ZP.rotationDegrees(180));
    }
    matrices.scale(1 / 16f, -1 / 16f, 1 / 16f);
    matrices.translate(0, 0, (INVISIBLE_BLOCKS.contains(block) ? -8 : -7) + .0125);
    for (TextContext textContext : entity.textContexts) {
      textContext.draw(
          ctx.getFont(), matrices, vertexConsumers, light, 16, entity.getHeight());
    }
  }
}
