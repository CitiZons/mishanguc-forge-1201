package pers.solid.mishang.uc;

import pers.solid.mishang.uc.MishangUtils;

import com.google.common.base.Predicates;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import pers.solid.mishang.uc.block.AbstractRoadBlock;
import pers.solid.mishang.uc.block.ColoredBlock;
import pers.solid.mishang.uc.block.StandingSignBlock;
import pers.solid.mishang.uc.blockentity.*;
import pers.solid.mishang.uc.blocks.MishangucBlocks;
import pers.solid.mishang.uc.data.stubs.ClientPlayNetworking;
import pers.solid.mishang.uc.item.CarryingToolItem;
import pers.solid.mishang.uc.item.DataTagToolItem;
import pers.solid.mishang.uc.item.MishangucItems;
import pers.solid.mishang.uc.render.*;
import pers.solid.mishang.uc.screen.HungSignBlockEditScreen;
import pers.solid.mishang.uc.screen.SignPresets;
import pers.solid.mishang.uc.screen.StandingSignBlockEditScreen;
import pers.solid.mishang.uc.screen.WallSignBlockEditScreen;

import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;
import pers.solid.mishang.uc.data.stubs.WorldRenderEvents;
import pers.solid.mishang.uc.data.stubs.ClientCommandRegistrationCallback;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "mishanguc", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class MishangucClient {
  /**
   * @see MishangucRules#FORCE_PLACING_TOOL_ACCESS
   */
  public static final AtomicReference<MishangucRules.ToolAccess> CLIENT_FORCE_PLACING_TOOL_ACCESS = new AtomicReference<>(MishangucRules.ToolAccess.CREATIVE_ONLY);
  /**
   * @see MishangucRules#CARRYING_TOOL_ACCESS
   */
  public static final AtomicReference<MishangucRules.ToolAccess> CLIENT_CARRYING_TOOL_ACCESS = new AtomicReference<>(MishangucRules.ToolAccess.ALL);

  @SubscribeEvent
  public static void onClientSetup(FMLClientSetupEvent event) {
    registerBlockLayers();

    // 方块轮廓/选区渲染由 pers.solid.mishang.uc.render.BlockOutlineRenderHandler
    // 通过 Forge 的 RenderHighlightEvent.Block 处理（自动经 @EventBusSubscriber 注册到 FORGE 总线）。

    // 注册客户端网络接收器。
    registerNetworking();

    event.enqueueWork(() -> {
      registerModelPredicateProviders();
    });
  }

  /**
   * 注册客户端的网络接收器（对应 Fabric 的 {@code ClientPlayNetworking.registerGlobalReceiver}）。
   */
  private static void registerNetworking() {
    // 客户端收到服务器发来的“编辑告示牌”数据包时，打开对应的编辑界面。
    ClientPlayNetworking.registerGlobalReceiver(
        new ResourceLocation("mishanguc", "edit_sign"),
        (client, handler, buf, responseSender) -> {
          try {
            final BlockPos blockPos = buf.readBlockPos();
            final BlockEntity blockEntity =
                client.level != null ? client.level.getBlockEntity(blockPos) : null;
            if (blockEntity instanceof final HungSignBlockEntity hungSignBlockEntity) {
              final Direction direction = buf.readEnum(Direction.class);
              client.execute(() ->
                  client.setScreen(new HungSignBlockEditScreen(hungSignBlockEntity, direction, blockPos)));
            } else if (blockEntity instanceof final WallSignBlockEntity wallSignBlockEntity) {
              client.execute(() ->
                  client.setScreen(new WallSignBlockEditScreen(wallSignBlockEntity, blockPos)));
            } else if (blockEntity instanceof final StandingSignBlockEntity standingSignBlockEntity) {
              final BlockHitResult blockHitResult = buf.readBlockHitResult();
              final Boolean isFront = StandingSignBlock.getHitSide(blockEntity.getBlockState(), blockHitResult);
              if (isFront != null) {
                client.execute(() -> client.setScreen(new StandingSignBlockEditScreen(standingSignBlockEntity, blockPos, isFront)));
              }
            }
          } catch (NullPointerException | ClassCastException exception) {
            Mishanguc.MISHANG_LOGGER.error("Error when creating sign edit screen:", exception);
          }
        });
    ClientPlayNetworking.registerGlobalReceiver(new ResourceLocation("mishanguc", "get_block_data"), new DataTagToolItem.BlockDataReceiver());
    ClientPlayNetworking.registerGlobalReceiver(new ResourceLocation("mishanguc", "get_entity_data"), new DataTagToolItem.EntityDataReceiver());
    ClientPlayNetworking.registerGlobalReceiver(new ResourceLocation("mishanguc", "rule_changed"), MishangucRules::handle);
  }

  @SubscribeEvent
  public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerBlockEntityRenderer(MishangucBlockEntities.HUNG_SIGN_BLOCK_ENTITY, HungSignBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(MishangucBlockEntities.COLORED_HUNG_SIGN_BLOCK_ENTITY, HungSignBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(MishangucBlockEntities.WALL_SIGN_BLOCK_ENTITY, WallSignBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(MishangucBlockEntities.COLORED_WALL_SIGN_BLOCK_ENTITY, WallSignBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(MishangucBlockEntities.FULL_WALL_SIGN_BLOCK_ENTITY, WallSignBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(MishangucBlockEntities.STANDING_SIGN_BLOCK_ENTITY, StandingSignBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(MishangucBlockEntities.COLORED_STANDING_SIGN_BLOCK_ENTITY, StandingSignBlockEntityRenderer::new);
  }

  @SubscribeEvent
  public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
    final Block[] coloredBlocks = MishangUtils.blocks().stream().filter(Predicates.instanceOf(ColoredBlock.class))
        .toArray(Block[]::new);
    event.register(
        (state, world, pos, tintIndex) -> {
          if (world == null || pos == null) return -1;
          BlockEntity entity = world.getBlockEntity(pos);
          // 考虑到玩家掉落产生粒子时，坐标会向上偏离一格。
          if (entity == null) entity = world.getBlockEntity(pos.below());
          if (entity instanceof ColoredBlockEntity coloredBlockEntity) {
            return coloredBlockEntity.getColor();
          } else {
            // 考虑到坐标本身的位置没有方块颜色，因此根据附近坐标来推断方块颜色。
            // 受部分渲染器影响，方块颜色会与周围插值，故需确保有自定义颜色的方块周围也会带有相同的自定义颜色。
            int accumulatedNum = 0;
            int accumulatedRed = 0;
            int accumulatedGreen = 0;
            int accumulatedBlue = 0;
            for (BlockPos outPos : BlockPos.withinManhattan(pos, 1, 1, 1)) {
              if (outPos.equals(pos)) continue;
              if (world.getBlockEntity(outPos) instanceof ColoredBlockEntity coloredBlockEntity) {
                final int color = coloredBlockEntity.getColor();
                accumulatedNum += 1;
                accumulatedRed += color >> 16 & 255;
                accumulatedGreen += color >> 8 & 255;
                accumulatedBlue += color & 255;
              }
            }
            if (accumulatedNum > 0) {
              return (accumulatedRed / accumulatedNum << 16) + (accumulatedGreen / accumulatedNum << 8) + accumulatedBlue / accumulatedNum;
            } else {
              return -1;
            }
          }
        },
        coloredBlocks
    );
  }

  @SubscribeEvent
  public static void onItemColors(RegisterColorHandlersEvent.Item event) {
    final Block[] coloredBlocks = MishangUtils.blocks().stream().filter(Predicates.instanceOf(ColoredBlock.class))
        .toArray(Block[]::new);
    event.register(
        (stack, tintIndex) -> {
          final CompoundTag nbt = stack.getTagElement("BlockEntityTag");
          if (nbt != null && nbt.contains("color", Tag.TAG_INT)) {
            return 0xff000000 | nbt.getInt("color");
          }
          return Color.HSBtoRGB(Util.getMillis() / 4096f + (stack.getItem().hashCode() >> 16) / 64f, 0.5f, 0.95f);
        },
        coloredBlocks
    );
  }

  private static void registerModelPredicateProviders() {
    // 模型谓词提供器
    ItemProperties.register(MishangucItems.EXPLOSION_TOOL,
        Mishanguc.id("explosion_power"),
        (stack, world, entity, seed) -> MishangucItems.EXPLOSION_TOOL.power(stack));

    SignPresets.loadAll();

    // TODO: ClientCommandRegistrationCallback needs Forge minecraft command conversion.
    // ClientCommandRegistrationCallback.EVENT.register(SignPresetCommand.INSTANCE);

    ItemProperties.register(MishangucItems.EXPLOSION_TOOL, Mishanguc.id("explosion_create_fire"), (stack, world, entity, seed) -> MishangucItems.EXPLOSION_TOOL.createFire(stack) ? 1 : 0);
    ItemProperties.register(MishangucItems.FAST_BUILDING_TOOL, Mishanguc.id("fast_building_range"), (stack, world, entity, seed) -> MishangucItems.FAST_BUILDING_TOOL.getRange(stack) / 64f);
    ItemProperties.register(MishangucItems.CARRYING_TOOL, Mishanguc.id("is_holding_block"), (stack, world, entity, seed) -> BooleanUtils.toInteger(CarryingToolItem.hasHoldingBlockState(stack)));
    ItemProperties.register(MishangucItems.CARRYING_TOOL, Mishanguc.id("is_holding_entity"), (stack, world, entity, seed) -> BooleanUtils.toInteger(CarryingToolItem.hasHoldingEntity(stack)));
  }

  @SuppressWarnings("deprecation")
  private static void registerBlockLayers() {
    // 设置相应的 BlockLayer
    // Note: ItemBlockRenderTypes.setRenderLayer is deprecated in newer Forge versions
    // but is the correct approach for 1.20.1.
    Validate.notEmpty(MishangucBlocks.translucentBlocks).forEach(block -> ItemBlockRenderTypes.setRenderLayer(block, RenderType.translucent()));
    Validate.notEmpty(MishangucBlocks.cutoutBlocks).forEach(block -> {
      ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutout());
      if (block instanceof AbstractRoadBlock roadBlock && roadBlock.getRoadSlab() != null) {
        ItemBlockRenderTypes.setRenderLayer(roadBlock.getRoadSlab(), RenderType.cutout());
      }
    });
    MishangucBlocks.translucentBlocks = null;
    MishangucBlocks.cutoutBlocks = null;
  }
}
