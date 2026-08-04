package pers.solid.mishang.uc.data.stubs;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 原本是 Fabric {@code WorldRenderContext} 的占位类，现已改为真实实现：
 * 由 Forge 的 {@code RenderHighlightEvent.Block} 在每帧渲染方块轮廓时构造，
 * 携带当前帧的渲染参数（矩阵、缓冲、相机、世界），供各工具的
 * {@link pers.solid.mishang.uc.render.RendersBlockOutline#renderBlockOutline} 绘制选区/轮廓。
 *
 * <p>保留原有的访问器方法名（{@code matrixStack()}/{@code consumers()} 等），
 * 因此所有调用方无需改动。
 */
public class WorldRenderContext {
    private final PoseStack matrixStack;
    private final MultiBufferSource consumers;
    private final Camera camera;
    private final float tickDelta;
    private final ClientLevel world;

    public WorldRenderContext(PoseStack matrixStack, MultiBufferSource consumers, Camera camera, float tickDelta, ClientLevel world) {
        this.matrixStack = matrixStack;
        this.consumers = consumers;
        this.camera = camera;
        this.tickDelta = tickDelta;
        this.world = world;
    }

    public PoseStack matrixStack() { return matrixStack; }

    public MultiBufferSource consumers() { return consumers; }

    public Camera camera() { return camera; }

    public float tickDelta() { return tickDelta; }

    public ClientLevel world() { return world; }

    public static class BlockOutlineContext {
        private final Entity entity;
        private final BlockState blockState;
        private final BlockPos blockPos;
        private final VoxelShape cameraCollisionShape;
        private final double cameraX;
        private final double cameraY;
        private final double cameraZ;

        public BlockOutlineContext(Entity entity, BlockState blockState, BlockPos blockPos, VoxelShape cameraCollisionShape, double cameraX, double cameraY, double cameraZ) {
            this.entity = entity;
            this.blockState = blockState;
            this.blockPos = blockPos;
            this.cameraCollisionShape = cameraCollisionShape;
            this.cameraX = cameraX;
            this.cameraY = cameraY;
            this.cameraZ = cameraZ;
        }

        public Entity entity() { return entity; }

        public BlockState blockState() { return blockState; }

        public BlockPos blockPos() { return blockPos; }

        public VoxelShape cameraCollisionShape() { return cameraCollisionShape; }

        public double cameraX() { return cameraX; }

        public double cameraY() { return cameraY; }

        public double cameraZ() { return cameraZ; }
    }
}
