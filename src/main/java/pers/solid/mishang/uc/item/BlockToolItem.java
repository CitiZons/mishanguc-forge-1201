package pers.solid.mishang.uc.item;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.Mishanguc;
import pers.solid.mishang.uc.mixin.WorldRendererInvoker;
import pers.solid.mishang.uc.render.RendersBlockOutline;

import java.util.Objects;
import com.mojang.blaze3d.vertex.VertexConsumer;
import pers.solid.mishang.uc.data.stubs.WorldRenderContext;

public abstract class BlockToolItem extends Item implements RendersBlockOutline {
  /**
   * 该物品是否包括流体。<br>
   * 如果该值为 <code>null</code>，则一般表示“视情况”，通常情况下是仅潜行时包括流体。 Whether fluids are included.<br>
   * If the value of it is <code>null</code>, it usually means "it depends", typically "does while
   * sneaking".
   */
  protected final @Nullable Boolean includesFluid;

  public BlockToolItem(Properties settings, @Nullable Boolean includesFluid) {
    super(settings);
    this.includesFluid = includesFluid;
  }

  /**
   * 玩家手持物品点击方块的行为。通常此时准星已经指向一个非流体方块。<br>
   * Behaviour when a player clicks a block holding the item. Usually the crossbar has already
   * focused on a non-fluid block.<br>
   * 如果 {@link #includesFluid} 为 <code>true</code>，则该方法不会执行，因为 {@link #use} 会执行，并执行可以包含流体的视线追踪。<br>
   * If {@link #includesFluid} is <code>false</code>, it does not execute, because {@link #use}
   * executes, and performs raycast that may include fluids.<br>
   * 会在服务端和客户端同时执行。Executes both on the minecraft and server side.
   *
   * @see Item#useOn(UseOnContext)
   */
  @Override
  public InteractionResult useOn(UseOnContext context) {
    return InteractionResult.PASS;
  }

  /**
   * 默认情况下，该方法仅在 {@link #includesFluid} 为 <code>false</code> 的情况下执行，此时会进行视线追踪并获取可能为流体的方块触及结果。<br>
   * By default these methods only perform when {@link #includesFluid} returns <code>false</code>,
   * when it performs raycast and get the {@link BlockHitResult} that may be of a fluid.
   * 会在服务端和客户端同时执行。Executes both on the minecraft and server side.
   *
   * @see #raycast
   * @see Item#use(Level, Player, InteractionHand)
   */
  @Override
  public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
    return super.use(world, user, hand);
  }

  /**
   * 使用此物品右键单击物品时的反应。 The reaction when right-clicking the block with the item.
   */
  public abstract InteractionResult useOnBlock(
      ItemStack stack, Player player,
      Level world,
      BlockHitResult blockHitResult,
      InteractionHand hand,
      boolean fluidIncluded);

  /**
   * 使用此物品开始破坏方块时的反应。
   *
   * @see Mishanguc#BEGIN_ATTACK_BLOCK_EVENT
   */
  public abstract InteractionResult beginAttackBlock(
      ItemStack stack, Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction, boolean fluidIncluded);

  /**
   * 使用此物品中途破坏方块时的反应。
   *
   * @see Mishanguc#PROGRESS_ATTACK_BLOCK_EVENT
   */
  public InteractionResult progressAttackBlock(
      Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction, boolean fluidIncluded) {
    return InteractionResult.FAIL;
  }

  public boolean includesFluid(ItemStack stack, boolean def) {
    final @Nullable Boolean includesFluid = this.includesFluid(stack);
    return Objects.requireNonNullElse(includesFluid, def);
  }

  /**
   * 如果物品堆的物品标签包含 IncludesFluid 标签，则返回其值，否则返回物品对象中的 {@link #includesFluid}。 Returns the value of tag
   * IncludesFluid if it exists, otherwise returns {@link #includesFluid}.
   *
   * @param stack The item stack.
   * @return Whether it can detect fluid. May be {@code null}able, which means it depends.
   */
  public @Nullable Boolean includesFluid(ItemStack stack) {
    final CompoundTag tag = stack.getTag();
    if (tag == null || !tag.contains("IncludesFluid")) {
      return this.includesFluid;
    } else {
      return tag.getBoolean("IncludesFluid");
    }
  }

  @OnlyIn(Dist.CLIENT)
  @Override
  public boolean renderBlockOutline(
      Player player,
      ItemStack itemStack,
      WorldRenderContext worldRenderContext,
      WorldRenderContext.BlockOutlineContext blockOutlineContext, InteractionHand hand) {
    final MultiBufferSource consumers = worldRenderContext.consumers();
    if (consumers == null) return true;
    final VertexConsumer vertexConsumer = consumers.getBuffer(RenderType.LINES);
    final ClientLevel world = worldRenderContext.world();
    final BlockPos blockPos = blockOutlineContext.blockPos();
    final BlockState state = blockOutlineContext.blockState();
    WorldRendererInvoker.drawCuboidShapeOutline(
        worldRenderContext.matrixStack(),
        vertexConsumer,
        state.getShape(world, blockPos, CollisionContext.of(player)),
        blockPos.getX() - blockOutlineContext.cameraX(),
        blockPos.getY() - blockOutlineContext.cameraY(),
        blockPos.getZ() - blockOutlineContext.cameraZ(),
        0,
        1,
        0,
        0.8f);
    if (includesFluid(itemStack, player.isShiftKeyDown())) {
      WorldRendererInvoker.drawCuboidShapeOutline(
          worldRenderContext.matrixStack(),
          vertexConsumer,
          state.getFluidState().getShape(world, blockPos),
          blockPos.getX() - blockOutlineContext.cameraX(),
          blockPos.getY() - blockOutlineContext.cameraY(),
          blockPos.getZ() - blockOutlineContext.cameraZ(),
          0,
          1,
          0.5f,
          0.5f);
    }
    return false;
  }
}
