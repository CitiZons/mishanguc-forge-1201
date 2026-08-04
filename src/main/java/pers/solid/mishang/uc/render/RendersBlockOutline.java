package pers.solid.mishang.uc.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import pers.solid.mishang.uc.MishangucClient;
import pers.solid.mishang.uc.data.stubs.WorldRenderContext;
import pers.solid.mishang.uc.data.stubs.WorldRenderEvents;

/**
 * <p>物品实现此接口后，玩家拿着物品时就会调用 {@link #renderBlockOutline}。{@link #RENDERER} 是个匿名的 {@link
 * WorldRenderEvents.BlockOutline} 实例，并且会在 {@link MishangucClient} 中注册。
 * <p>Item implements this interface, and when a player holds this item, {@link #renderBlockOutline}
 * will be called. The {@link #RENDERER} is in an anonymous {@link WorldRenderEvents.BlockOutline}
 * instance, and was registered in {@link MishangucClient}.
 * <p>物品实现此接口时，需要注解为：
 * <p>Items implementing this interface must be annotated as:
 *
 * <pre>
 * {@code }</pre>
 */
@OnlyIn(Dist.CLIENT)
public interface RendersBlockOutline {
  @OnlyIn(Dist.CLIENT)
  WorldRenderEvents.BlockOutline RENDERER =
      (worldRenderContext, blockOutlineContext) -> {
        if (!(blockOutlineContext.entity() instanceof final Player player)) {
          return true;
        }
        for (final InteractionHand hand : new InteractionHand[]{InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND}) {
          final ItemStack stackInHand = player.getItemInHand(hand);
          final Item item = stackInHand.getItem();
          if (item instanceof final RendersBlockOutline rendersBlockOutline) {
            if (!rendersBlockOutline.renderBlockOutline(player, stackInHand, worldRenderContext, blockOutlineContext, hand)) {
              return false;
            }
          }
        }
        return true;
      };

  /**
   * <p>玩家持有该物品的物品堆时，进行渲染。将会被 {@link #RENDERER} 中的 {@link WorldRenderEvents.BlockOutline#onBlockOutline} 调用。
   * <p>Render when a player holds an item stack of this item. Called in {@link WorldRenderEvents.BlockOutline#onBlockOutline} of {@link #RENDERER}.
   * <p>子类覆盖此方法时，必须注解 <code>@{@link Environment}({@link EnvType#CLIENT})</code>。
   * <p><code>@{@link Environment}({@link EnvType#CLIENT})</code> must be annotated when overridden by
   * subtype methods.
   *
   * @since 0.2.0 加入了参数 hand，表示持有此物品的手。这是考虑到主手和副手都有可能持有此物品，当副手持有此物品时，只能应用“使用”效果，但不能应用“攻击”效果。此参数可以用来进行区分。
   */
  @OnlyIn(Dist.CLIENT)
  boolean renderBlockOutline(
      Player player,
      ItemStack itemStack,
      WorldRenderContext worldRenderContext,
      WorldRenderContext.BlockOutlineContext blockOutlineContext, InteractionHand hand);
}
