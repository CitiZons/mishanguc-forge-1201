package pers.solid.mishang.uc.item;

import net.minecraft.world.level.block.state.pattern.BlockInWorld;

import net.minecraft.world.phys.shapes.Shapes;

import pers.solid.mishang.uc.MishangUtils;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.Mishanguc;
import pers.solid.mishang.uc.block.AbstractRoadBlock;
import pers.solid.mishang.uc.blocks.RoadSlabBlocks;
import pers.solid.mishang.uc.data.stubs.ClientPlayNetworking;
import pers.solid.mishang.uc.data.stubs.PacketByteBufs;
import pers.solid.mishang.uc.data.stubs.ServerPlayNetworking;
import pers.solid.mishang.uc.mixin.WorldRendererInvoker;
import pers.solid.mishang.uc.render.RendersBlockOutline;
import pers.solid.mishang.uc.util.TextBridge;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.player.LocalPlayer;
import pers.solid.mishang.uc.data.stubs.WorldRenderContext;
import pers.solid.mishang.uc.data.stubs.PacketSender;

/**
 * 用于处理台阶的工具。
 */

public class SlabToolItem extends Item implements RendersBlockOutline, MishangucItem {
  /**
   * 从原版的 {@link BlockFamilies} 提取的方块至台阶方块的映射。
   */
  @ApiStatus.AvailableSince("0.1.3")
  protected static final BiMap<Block, Block> BLOCK_TO_SLAB = BlockFamilies.getAllFamilies()
      .filter(blockFamily -> blockFamily.get(BlockFamily.Variant.SLAB) != null)
      .map(blockFamily -> {
        final Block variant = blockFamily.get(BlockFamily.Variant.SLAB);
        final Block baseBlock = blockFamily.getBaseBlock();
        return baseBlock == null || variant == null ? null : Maps.immutableEntry(baseBlock, variant);
      })
      .filter(Objects::nonNull)
      .collect(ImmutableBiMap.toImmutableBiMap(Map.Entry::getKey, Map.Entry::getValue));
  /**
   * @since 1.0.3 用于协调处理 canMine 与 performBreak。服务器不知道客户端的 crosshairTarget，需要由客户端发送。服务器先判断为允许挖掘，再根据这里面的内容还原该方块。
   */
  private static final Map<Pair<ServerLevel, BlockPos>, Runnable> SERVER_BLOCK_BREAKING_BRIDGE = new Object2ObjectOpenHashMap<>();

  public SlabToolItem(Properties settings) {
    super(settings);
  }

  /**
   * 将基础方块的方块状态转化为台阶方块，并尝试移植相应的方块状态属性。
   *
   * @param baseBlockState 基础方块的方块状态。
   * @param slabBlock      台阶方块，不是具体的方块状态。
   * @return 台阶方块的方块状态。
   */
  protected static BlockState toDoubleSlab(BlockState baseBlockState, Block slabBlock) {
    final BlockState slabState = MishangUtils.getStateWithProperties(slabBlock, baseBlockState);
    return slabState.hasProperty(BlockStateProperties.SLAB_TYPE) ? slabState.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE) : slabState;
  }

  /**
   * 尝试将 blockState 转化为双台阶。当它可以转化为双台阶，或者自身已经就是双台阶时，返回这个双台阶，否会返回 {@code null}。
   */
  protected static BlockState tryToDoubleSlab(BlockState state) {
    final Block block = state.getBlock();
    if (BLOCK_TO_SLAB.containsKey(block)) {
      state = toDoubleSlab(state, BLOCK_TO_SLAB.get(block));
    } else if (block instanceof AbstractRoadBlock && RoadSlabBlocks.BLOCK_TO_SLABS.containsKey(block)) {
      state = toDoubleSlab(state, RoadSlabBlocks.BLOCK_TO_SLABS.get(block));
    } else {
      final Block slab = ExtShapeBridge.getExtShapeSlabBlock(block);
      if (slab != null) {
        state = toDoubleSlab(state, slab);
      } else {
        // 尝试根据方块的 id 来判断对应的台阶方块。
        final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        final String idPath = id.getPath();
        final ResourceLocation slabId = new ResourceLocation(id.getNamespace(), idPath + "_slab");
        if (BuiltInRegistries.BLOCK.containsKey(slabId)) {
          state = toDoubleSlab(state, BuiltInRegistries.BLOCK.get(slabId));
        } else {
          final ResourceLocation slabId2;
          if (idPath.endsWith("_bricks") || idPath.endsWith("_tiles")) {
            slabId2 = new ResourceLocation(id.getNamespace(), idPath.substring(0, idPath.length() - 1) + "_slab");
          } else if (idPath.endsWith("_planks")) {
            slabId2 = new ResourceLocation(id.getNamespace(), idPath.substring(0, idPath.length() - 7) + "_slab");
          } else {
            slabId2 = null;
          }
          if (slabId2 != null && BuiltInRegistries.BLOCK.containsKey(slabId2)) {
            state = toDoubleSlab(state, BuiltInRegistries.BLOCK.get(slabId2));
          }
        }
      }
    }
    if (state.hasProperty(BlockStateProperties.SLAB_TYPE) && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) {
      return state;
    } else {
      return null;
    }
  }

  private static boolean performBreak(Level world, BlockPos pos, Player miner, boolean isTop) {
    BlockState state = world.getBlockState(pos);
    final Block block = state.getBlock();
    final BlockState doubleSlabState = tryToDoubleSlab(state);
    if (doubleSlabState != null) {
      state = doubleSlabState;
    }
    if (state.hasProperty(BlockStateProperties.SLAB_TYPE) && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) {
      final SlabType slabTypeToSet = isTop ? SlabType.BOTTOM : SlabType.TOP;
      final SlabType slabTypeBroken = isTop ? SlabType.TOP : SlabType.BOTTOM;
      // 破坏方块
      final BlockEntity blockEntity = world.getBlockEntity(pos);
      final CompoundTag nbt;
      if (blockEntity != null) {
        nbt = blockEntity.saveWithFullMetadata();
        world.removeBlockEntity(pos);
      } else {
        nbt = null;
      }
      final boolean bl1 = world.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.SLAB_TYPE, slabTypeToSet));
      final BlockEntity newBlockEntity = world.getBlockEntity(pos);
      if (newBlockEntity != null && nbt != null) {
        newBlockEntity.load(nbt);
      }
      final BlockState brokenState = state.setValue(BlockStateProperties.SLAB_TYPE, slabTypeBroken);
      block.playerWillDestroy(world, pos, brokenState, miner);
      if (bl1) {
        block.destroy(world, pos, brokenState);
        if (!miner.isCreative()) {
          block.playerDestroy(world, miner, pos, brokenState, world.getBlockEntity(pos), miner.getMainHandItem().copy());
        }
        miner.getItemInHand(InteractionHand.MAIN_HAND).hurtAndBreak(1, miner, player -> player.broadcastBreakEvent(InteractionHand.MAIN_HAND));
      }
      return bl1;
    }
    return false;
  }

  @Override
  public void appendHoverText(
      ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    super.appendHoverText(stack, world, tooltip, context);
    tooltip.add(TextBridge.translatable("item.mishanguc.slab_tool.tooltip").withStyle(ChatFormatting.GRAY));
  }

  /**
   * 破坏台阶的一部分。
   *
   * @see Handler#receive
   */
  @Override
  public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player miner) {
    // 处理双台阶的情况。
    if (world.isClientSide && miner instanceof LocalPlayer) {
      final HitResult raycast = Minecraft.getInstance().hitResult;
      if (!(raycast instanceof BlockHitResult) || raycast.getType() == HitResult.Type.MISS) return false;
      boolean isTop = raycast.getLocation().y - (double) ((BlockHitResult) raycast).getBlockPos().getY() > 0.5D;
      final boolean bl1 = performBreak(world, pos, miner, isTop);
      final FriendlyByteBuf buf = PacketByteBufs.create();
      buf.writeBlockPos(pos);
      buf.writeBoolean(isTop);
      ClientPlayNetworking.send(new ResourceLocation("mishanguc", "slab_tool"), buf);
      return !bl1;
    } else {
      // 注意：需要考虑这样的情况：
      // 客户端使用工具破坏方块后，发送 mishanguc:slab_tool 的 packet 到服务器
      // 服务器收到 packet 之后，执行 performBreak，然后再收到原版 packet，执行此处的 canMine，得出不准确的结果。
      // 因此，需要确保服务器上的 canMine 在 performBreak 之前执行。
      final Runnable remove = SERVER_BLOCK_BREAKING_BRIDGE.remove(Pair.of(world, pos));
      if (remove instanceof PacketReceivedFirst) {
        // 执行从封包的 receive 中推迟过来的。
        remove.run();
        return false;
      } else {
        // 服务器还没有执行 performBreak。可能它根本就不是台阶，也有可能是本来就在 canMine 完成之后再执行 performBreak。
        final boolean b = tryToDoubleSlab(state) == null;
        if (remove == null && !b) SERVER_BLOCK_BREAKING_BRIDGE.put(Pair.of((ServerLevel) world, pos), CAN_MINE_CALLED_FIRST);
        return b;
      }
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
    if (consumers == null || hand != InteractionHand.MAIN_HAND) return true;
    final ClientLevel world = worldRenderContext.world();
    BlockState state = blockOutlineContext.blockState();
    final HitResult crosshairTarget = Minecraft.getInstance().hitResult;
    if (!(crosshairTarget instanceof final BlockHitResult blockHitResult)) {
      return true;
    }
    boolean isTop = crosshairTarget.getLocation().y - (double) blockHitResult.getBlockPos().getY() > 0.5D;
    state = tryToDoubleSlab(state);
    if (state != null) {
      // 渲染时需要使用的方块状态。
      final BlockState halfState =
          state.setValue(BlockStateProperties.SLAB_TYPE, isTop ? SlabType.TOP : SlabType.BOTTOM);
      final BlockPos blockPos = blockOutlineContext.blockPos();
      WorldRendererInvoker.drawCuboidShapeOutline(
          worldRenderContext.matrixStack(),
          consumers.getBuffer(RenderType.LINES),
          halfState.getShape(world, blockPos, CollisionContext.of(blockOutlineContext.entity())),
          (double) blockPos.getX() - blockOutlineContext.cameraX(),
          (double) blockPos.getY() - blockOutlineContext.cameraY(),
          (double) blockPos.getZ() - blockOutlineContext.cameraZ(),
          0.0F,
          0.0F,
          0.0F,
          0.4F);
      return false;
    }
    return true;
  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, this)
        .pattern("SCS")
        .pattern(" | ")
        .pattern(" | ")
        .define('S', Items.SHEARS)
        .define('C', Items.STONE)
        .define('|', Items.STICK)
        .unlockedBy("has_shears", FabricRecipeProvider.has(Items.SHEARS))
        .unlockedBy("has_stone", FabricRecipeProvider.has(Items.STONE));
  }

  @ApiStatus.AvailableSince("1.0.3")
  public enum Handler implements ServerPlayNetworking.PlayChannelHandler {
    INSTANCE;

    /**
     * @see #canAttackBlock(BlockState, Level, BlockPos, Player)
     */
    @Override
    public void receive(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
      final BlockPos blockPos = buf.readBlockPos();
      final boolean isTop = buf.readBoolean();
      server.execute(() -> {
        if (player.getEyePosition().distanceToSqr(Vec3.atCenterOf(blockPos)) > net.minecraft.server.network.ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE) {
          return;
        }
        final ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SlabToolItem) || !(player.getAbilities().mayBuild || stack.hasAdventureModeBreakTagForBlock(server.registryAccess().registryOrThrow(Registries.BLOCK), new BlockInWorld(player.level(), blockPos, false)))) {
          return;
        }
        final Runnable remove = SERVER_BLOCK_BREAKING_BRIDGE.remove(Pair.of(player.level(), blockPos));
        if (remove == CAN_MINE_CALLED_FIRST) {
          performBreak(player.level(), blockPos, player, isTop);
        } else if (tryToDoubleSlab(player.serverLevel().getBlockState(blockPos)) != null) {
          // 收到封包之后，送到 canMine 中执行。
          SERVER_BLOCK_BREAKING_BRIDGE.put(Pair.of(player.serverLevel(), blockPos), (PacketReceivedFirst) () -> performBreak(player.serverLevel(), blockPos, player, isTop));
        }
      });
    }
  }

  private interface PacketReceivedFirst extends Runnable {
  }

  private static final Runnable CAN_MINE_CALLED_FIRST = () -> {
  };

  @ApiStatus.AvailableSince("1.0.4")
  private static final class ExtShapeBridge {
    private static final Class<?> extshape_BlockMappings_class;
    private static final Method extshape_getBlockOf_method;
    private static final Object extshape_slab_shape;

    static {
      Object extshape_slab_shape1 = null;
      Method extshape_getBlockOf_method1 = null;
      Class<?> extshape_BlockMappings_class1 = null;
      Class<?> extshape_BlockShape_class;

      if (ModList.get().isLoaded("extshape")) try {
        extshape_BlockMappings_class1 = Class.forName("pers.solid.extshape.util.BlockBiMaps");
        extshape_BlockShape_class = Class.forName("pers.solid.extshape.builder.BlockShape");
        extshape_getBlockOf_method1 = MethodUtils.getAccessibleMethod(extshape_BlockMappings_class1, "getBlockOf", extshape_BlockShape_class, Block.class);
        extshape_slab_shape1 = FieldUtils.getDeclaredField(extshape_BlockShape_class, "SLAB").get(null);
      } catch (Throwable e) {
        extshape_BlockMappings_class1 = null;
        extshape_getBlockOf_method1 = null;
        if (!(e instanceof ClassNotFoundException || e instanceof ClassCastException)) {
          Mishanguc.MISHANG_LOGGER.error("Unknown exception when trying to connect with Extended Block Shape mod:", e);
        }
      }
      extshape_slab_shape = extshape_slab_shape1;
      extshape_getBlockOf_method = extshape_getBlockOf_method1;
      extshape_BlockMappings_class = extshape_BlockMappings_class1;
      if (extshape_slab_shape != null && extshape_getBlockOf_method != null) {
        Mishanguc.MISHANG_LOGGER.info("Mishang Urban Construction mod has successfully created bridged into Extended Block Shapes mod!");
      }
    }

    public static @Nullable Block getExtShapeSlabBlock(Block baseBlock) {
      if (extshape_BlockMappings_class == null || extshape_getBlockOf_method == null || extshape_slab_shape == null) {
        return null;
      }
      try {
        return (Block) extshape_getBlockOf_method.invoke(null, extshape_slab_shape, baseBlock);
      } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException | ClassCastException e) {
        Mishanguc.MISHANG_LOGGER.error("Unexpected error when trying to get slab of block {}. This should not happen no matter whether you have installed Mishang Urban Construction mod.", baseBlock, e);
        return null;
      }
    }
  }
}
