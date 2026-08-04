package pers.solid.mishang.uc.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import pers.solid.mishang.uc.data.stubs.WorldRenderContext;

/**
 * 把 Forge 的 {@link RenderHighlightEvent.Block}（每帧渲染方块准星轮廓时触发）转换为本模组工具所需的
 * {@link WorldRenderContext}，并调用 {@link RendersBlockOutline#RENDERER} 与
 * {@link RendersBeforeOutline#RENDERER}，使各建造工具能够绘制自定义的选区/轮廓高亮。
 *
 * <p>对应 Fabric 中的 {@code WorldRenderEvents.BLOCK_OUTLINE} / {@code BEFORE_BLOCK_OUTLINE}。
 * 当工具的 {@code renderBlockOutline} 返回 {@code false}（表示已自行绘制并希望取代原版轮廓）时，
 * 取消该事件以隐藏原版的方块轮廓。
 */
@Mod.EventBusSubscriber(modid = "mishanguc", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BlockOutlineRenderHandler {
  private BlockOutlineRenderHandler() {
  }

  @SubscribeEvent
  public static void onRenderBlockHighlight(RenderHighlightEvent.Block event) {
    final Minecraft minecraft = Minecraft.getInstance();
    final ClientLevel level = minecraft.level;
    if (level == null) {
      return;
    }
    final BlockHitResult target = event.getTarget();
    if (target == null || target.getType() != HitResult.Type.BLOCK) {
      return;
    }
    final Camera camera = event.getCamera();
    final Vec3 cameraPos = camera.getPosition();
    final BlockPos blockPos = target.getBlockPos();
    final BlockState blockState = level.getBlockState(blockPos);

    final WorldRenderContext context = new WorldRenderContext(
        event.getPoseStack(),
        event.getMultiBufferSource(),
        camera,
        event.getPartialTick(),
        level);

    // 先调用“轮廓之前”的渲染（对应 Fabric BEFORE_BLOCK_OUTLINE），其返回值在本模组中恒为 true，不影响后续。
    RendersBeforeOutline.RENDERER.onBeforeBlockOutline(context, target);

    final WorldRenderContext.BlockOutlineContext blockOutlineContext = new WorldRenderContext.BlockOutlineContext(
        camera.getEntity(),
        blockState,
        blockPos,
        blockState.getShape(level, blockPos),
        cameraPos.x,
        cameraPos.y,
        cameraPos.z);

    final boolean keepVanillaOutline = RendersBlockOutline.RENDERER.onBlockOutline(context, blockOutlineContext);
    if (!keepVanillaOutline) {
      event.setCanceled(true);
    }
  }
}
