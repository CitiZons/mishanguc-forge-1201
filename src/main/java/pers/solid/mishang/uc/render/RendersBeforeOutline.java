package pers.solid.mishang.uc.render;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import pers.solid.mishang.uc.data.stubs.WorldRenderContext;

/**
 * 实现此方法的物品，在玩家持有时（无论是主手还是副手），均会调用 {@link #renderBeforeOutline(WorldRenderContext, HitResult, Player, InteractionHand)} 方法。这是由仅客户端的 {@link BlockOutlineRenderHandler} 调用的。<p>
 * <p>
 * 此接口本身不注解 {@code @OnlyIn}，因为实现它的物品类也会在专用服务器上加载；但重写 {@link #renderBeforeOutline} 时，务必注解 {@code @OnlyIn(Dist.CLIENT)}。
 */
public interface RendersBeforeOutline {
  /**
   * 玩家持有此物品时，客户端进行的渲染操作。仅限客户端执行。覆盖此方法时，请一并注解上 {@code @OnlyIn(Dist.CLIENT)}。<p>
   * 注意该方法是在旁观者检查之前调用的，也就是说，即使是在旁观模式下也会调用此方法，因此你可能需要手动检查玩家是否为旁观模式。
   *
   * @param context   当前渲染场景中的参数。
   * @param hitResult 客户端的追星目标。
   * @param player    执行此渲染的客户端玩家。实际传入的对象是 {@code LocalPlayer}，如需客户端专有的方法，可在客户端专有的实现中强制转换。
   * @param hand      玩家持有此物品的手，可以用来指定只有玩家使用特定的手持有此物品时才会进行渲染。
   */
  void renderBeforeOutline(
      WorldRenderContext context,
      HitResult hitResult,
      Player player, InteractionHand hand);
}
