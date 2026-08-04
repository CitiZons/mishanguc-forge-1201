package pers.solid.mishang.uc.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import net.minecraft.client.player.LocalPlayer;
import pers.solid.mishang.uc.data.stubs.WorldRenderContext;
import pers.solid.mishang.uc.data.stubs.WorldRenderEvents;

/**
 * 实现此方法的物品，在玩家持有时（无论是主手还是副手），均会调用 {@link #renderBeforeOutline(WorldRenderContext, HitResult, LocalPlayer, InteractionHand)} 方法。这是在 {@link pers.solid.mishang.uc.MishangucClient} 中注册的。<p>
 * <p>
 * 此方法为客户端专有，因此实现此方法时，务必注解为 {@link OnlyIn}，并在重写的方法上注解 {@link OnlyIn}。
 */
@OnlyIn(Dist.CLIENT)
public interface RendersBeforeOutline {
  /**
   * 玩家持有此物品时，客户端进行的渲染操作。仅限客户端执行。覆盖此方法时，请一并注解上 {@link OnlyIn}。<p>
   * 注意该方法是在旁观者检查之前调用的，也就是说，即使是在旁观模式下也会调用此方法，因此你可能需要手动检查玩家是否为旁观模式。
   *
   * @param context   当前渲染场景中的参数，参见 {@link WorldRenderEvents#BEFORE_DEBUG_RENDER}。
   * @param hitResult 客户端的追星目标。
   * @param player    执行此渲染的客户端玩家。
   * @param hand      玩家持有此物品的手，可以用来指定只有玩家使用特定的手持有此物品时才会进行渲染。
   */
  void renderBeforeOutline(
      WorldRenderContext context,
      HitResult hitResult,
      LocalPlayer player, InteractionHand hand);

  WorldRenderEvents.BeforeBlockOutline RENDERER = (context, hitResult) -> {
    final Minecraft minecraft = Minecraft.getInstance();
    final LocalPlayer player = minecraft.player;
    if (player == null) return true;
    for (final InteractionHand hand : new InteractionHand[]{InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND}) {
      final ItemStack stackInHand = player.getItemInHand(hand);
      if (stackInHand.getItem() instanceof RendersBeforeOutline rendersBeforeOutline) {
        rendersBeforeOutline.renderBeforeOutline(context, hitResult, player, hand);
      }
    }
    return true;
  };
}
