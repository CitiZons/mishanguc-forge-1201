package pers.solid.mishang.uc.render;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import pers.solid.mishang.uc.data.stubs.WorldRenderContext;

/**
 * <p>物品实现此接口后，玩家拿着物品时就会调用 {@link #renderBlockOutline}。该方法由仅客户端的
 * {@link BlockOutlineRenderHandler} 在每帧渲染方块轮廓时调用。
 * <p>Item implements this interface, and when a player holds this item, {@link #renderBlockOutline}
 * will be called. The method is invoked by the client-only {@link BlockOutlineRenderHandler} when
 * rendering the block outline each frame.
 * <p>此接口本身不注解 {@code @OnlyIn}，因为实现它的物品类也会在专用服务器上加载；
 * 但重写 {@link #renderBlockOutline} 时必须注解 {@code @OnlyIn(Dist.CLIENT)}。
 * <p>The interface itself is not annotated {@code @OnlyIn}, as item classes implementing it are
 * also loaded on dedicated servers; however, overrides of {@link #renderBlockOutline} must be
 * annotated {@code @OnlyIn(Dist.CLIENT)}.
 */
public interface RendersBlockOutline {

  /**
   * <p>玩家持有该物品的物品堆时，进行渲染。将会被 {@link BlockOutlineRenderHandler} 调用，仅客户端执行。
   * <p>Render when a player holds an item stack of this item. Called by
   * {@link BlockOutlineRenderHandler}, client side only.
   * <p>子类覆盖此方法时，必须注解 {@code @OnlyIn(Dist.CLIENT)}。
   * <p>{@code @OnlyIn(Dist.CLIENT)} must be annotated when overridden by subtype methods.
   *
   * @since 0.2.0 加入了参数 hand，表示持有此物品的手。这是考虑到主手和副手都有可能持有此物品，当副手持有此物品时，只能应用“使用”效果，但不能应用“攻击”效果。此参数可以用来进行区分。
   */
  boolean renderBlockOutline(
      Player player,
      ItemStack itemStack,
      WorldRenderContext worldRenderContext,
      WorldRenderContext.BlockOutlineContext blockOutlineContext, InteractionHand hand);
}
