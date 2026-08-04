package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import pers.solid.mishang.uc.data.stubs.When;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.*;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
import pers.solid.mishang.uc.data.stubs.BlockStateSupplier;
import pers.solid.mishang.uc.data.stubs.VariantSettings;
import pers.solid.mishang.uc.data.stubs.Model;
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.*;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.*;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.blockentity.BlockEntityWithText;
import pers.solid.mishang.uc.blockentity.HungSignBlockEntity;
import pers.solid.mishang.uc.blocks.WallSignBlocks;
import pers.solid.mishang.uc.data.MishangucModels;
import pers.solid.mishang.uc.data.MishangucTextureKeys;
import pers.solid.mishang.uc.data.ModelHelper;
import pers.solid.mishang.uc.data.stubs.PacketByteBufs;
import pers.solid.mishang.uc.data.stubs.ServerPlayNetworking;
import pers.solid.mishang.uc.mixin.ItemUsageContextInvoker;
import pers.solid.mishang.uc.render.HungSignBlockEntityRenderer;
import pers.solid.mishang.uc.text.TextContext;
import pers.solid.mishang.uc.util.TextBridge;
import net.minecraft.ChatFormatting;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.player.LocalPlayer;
import com.mojang.math.Axis;

/**
 * @see HungSignBlockEntity
 * @see HungSignBlockEntityRenderer
 */
public class HungSignBlock extends Block implements SimpleWaterloggedBlock, EntityBlock, MishangucBlock {
  public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
  public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
  /**
   * 告示牌是否对左侧连接。若 axis=x，则 left 表示南方；若 axis=z，则 left 表示西方。<br>
   * Whether the sign is connected to the left. The "left" represents "south" if "axis=x", or "west"
   * if "axis=z".
   */
  public static final BooleanProperty LEFT = BooleanProperty.create("left");
  /**
   * 告示牌是否对右侧连接。若 axis=x，则 right 表示北方；若 axis=z，则 right 表示东方。<br>
   * Whether the sign is connected to the right. The "right" represents "north" if "axis=x", or
   * "east" if "axis=z".
   */
  public static final BooleanProperty RIGHT = BooleanProperty.create("right");

  private static final VoxelShape SHAPE_X =
      Shapes.or(
          box(7.5, 5, 0, 8.5, 14, 16), box(7.25, 12, 0, 8.75, 13, 16));
  private static final VoxelShape SHAPE_Z =
      Shapes.or(
          box(0, 5, 7.5, 16, 14, 8.5), box(0, 12, 7.25, 16, 13, 8.75));
  private static final Map<Direction, @Nullable VoxelShape> BAR_SHAPES =
      MishangUtils.createHorizontalDirectionToShape(7.5, 13, 11, 8.5, 16, 12);
  private static final Map<Direction, @Nullable VoxelShape> BAR_SHAPES_EDGE =
      MishangUtils.createHorizontalDirectionToShape(7.5, 13, 13, 8.5, 16, 14);
  private static final VoxelShape SHAPE_WIDENED_X = box(6.5, 5, 0, 9.5, 16, 16);
  private static final VoxelShape SHAPE_WIDENED_Z = box(0, 5, 6.5, 16, 16, 9.5);

  public final @Nullable Block baseBlock;
  /**
   * 基础方块的纹理。{@link #getBaseTexture()} 会使用到此值。如果此值为 {@code null}，则根据 {@link #baseBlock} 来推断纹理。<br>
   * 非 final，可直接进行修改。
   */
  @ApiStatus.AvailableSince("0.1.7")
  public @Nullable ResourceLocation baseTexture;
  /**
   * 告示牌杆的纹理。可能为 {@code null}。生成模型时，可直接作为 null 传入，转化为 json 时会被忽略。
   */
  @ApiStatus.AvailableSince("0.1.7")
  public @Nullable ResourceLocation barTexture;
  /**
   * 告示牌顶部的纹理。可能为 {@code null}。生成模型时，可直接作为 null 传入，转化为 json 时会被忽略。
   */
  @ApiStatus.AvailableSince("0.1.7")
  public @Nullable ResourceLocation textureTop;

  public HungSignBlock(@Nullable Block baseBlock, Properties settings) {
    super(settings);
    this.baseBlock = baseBlock;
    this.registerDefaultState(defaultBlockState()
        .setValue(WATERLOGGED, false)
        .setValue(AXIS, Direction.Axis.X)
        .setValue(LEFT, false)
        .setValue(RIGHT, false));
  }

  @ApiStatus.AvailableSince("0.1.7")
  public HungSignBlock(@NotNull Block baseBlock) {
    this(baseBlock, BlockBehaviour.Properties.copy(baseBlock).mapColor(baseBlock.defaultMapColor()));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(AXIS, WATERLOGGED, LEFT, RIGHT);
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext ctx) {
    final BlockState placementState = super.getStateForPlacement(ctx);
    if (placementState == null) {
      return null;
    }
    final Level world = ctx.getLevel();
    final BlockPos blockPos = ctx.getClickedPos();
    final BlockState blockState =
        world.getBlockState(((ItemUsageContextInvoker) ctx).invokeGetHitResult().getBlockPos());
    return placementState
        .setValue(AXIS,
            blockState.getBlock() instanceof HungSignBlock && blockState.hasProperty(AXIS)
                ? blockState.getValue(AXIS)
                : ctx.getHorizontalDirection().getAxis())
        .setValue(WATERLOGGED, world.getFluidState(blockPos).getType() == Fluids.WATER)
        .updateShape(
            Direction.UP, world.getBlockState(blockPos.above()), world, blockPos, blockPos.above());
  }

  @NotNull
  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new HungSignBlockEntity(pos, state);
  }

  @SuppressWarnings("deprecation")
  @Override
  public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    final Direction.Axis axis = state.getValue(AXIS);
    return switch (axis) {
      case X -> SHAPE_WIDENED_X;
      case Z -> SHAPE_WIDENED_Z;
      default -> Shapes.empty();
    };
  }

  @SuppressWarnings("deprecation")
  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    final Direction.Axis axis = state.getValue(AXIS);
    final boolean left = state.getValue(LEFT);
    final boolean right = state.getValue(RIGHT);
    switch (axis) {
      case X:
        if (!left && !right)
          return Shapes.or(
              SHAPE_X, BAR_SHAPES_EDGE.get(Direction.SOUTH), BAR_SHAPES_EDGE.get(Direction.NORTH));
        else
          return Shapes.or(
              SHAPE_X,
              !left ? BAR_SHAPES.get(Direction.SOUTH) : Shapes.empty(),
              !right ? BAR_SHAPES.get(Direction.NORTH) : Shapes.empty());
      case Z:
        if (!left && !right)
          return Shapes.or(
              SHAPE_Z, BAR_SHAPES_EDGE.get(Direction.WEST), BAR_SHAPES_EDGE.get(Direction.EAST));
        else
          return Shapes.or(
              SHAPE_Z,
              !left ? BAR_SHAPES.get(Direction.WEST) : Shapes.empty(),
              !right ? BAR_SHAPES.get(Direction.EAST) : Shapes.empty());
      default:
        return Shapes.empty();
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public VoxelShape getBlockSupportShape(BlockState state, BlockGetter world, BlockPos pos) {
    return getShape(state, world, pos, CollisionContext.empty());
  }

  @SuppressWarnings("deprecation")
  @Override
  public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
    return getCollisionShape(state, world, pos, CollisionContext.empty());
  }

  @SuppressWarnings("deprecation")
  @Override
  public FluidState getFluidState(BlockState state) {
    return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
  }

  /**
   * 当这个指定的方向连接有同类方块时，这个方块（left 和 right）就会为 true，此时上方将不会显示栏杆。<br>
   * 如果连接有非同类方块，且上方没有连接带有碰撞箱的方块，则这个方向也会为 true。
   */
  @SuppressWarnings("deprecation")
  @Override
  public BlockState updateShape(
      BlockState state,
      Direction direction,
      BlockState neighborState,
      LevelAccessor world,
      BlockPos pos,
      BlockPos neighborPos) {
    state = super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    if (state.getValue(WATERLOGGED)) {
      world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
    }
    if (Direction.Plane.HORIZONTAL.test(direction)) {
      if (world.getBlockState(pos.above()).getCollisionShape(world, pos.above()).min(Direction.Axis.Y) == 0) {
        state = prepareNeighborState(state, direction, neighborState);
      } else {
        state = state.setValue(LEFT, true).setValue(RIGHT, true);
      }
    } else if (direction == Direction.UP) {
      if (neighborState.getCollisionShape(world, neighborPos).min(Direction.Axis.Y) == 0) {
        for (Direction horizontalDirection : Direction.Plane.HORIZONTAL) {
          final BlockPos offsetPos = pos.relative(horizontalDirection);
          state = prepareNeighborState(state, horizontalDirection, world.getBlockState(offsetPos));
        }
      } else {
        state = state.setValue(LEFT, true).setValue(RIGHT, true);
      }
    }
    return state;
  }

  /**
   * 计算与周围某个方向连接时应有的方块状态，以确定杆子是否显示。与 {@link #updateShape} 相比，该方法不会检测上方方块是否已连接（调用此方法时，就假定上方已经连接了方块），也不会更新流体状态。
   *
   * @param state         该方块原先的方块状态。
   * @param direction     毗邻方块所属的方向。
   * @param neighborState 批零的方块状态。
   * @return 对应的方块状态。
   */
  public BlockState prepareNeighborState(BlockState state, Direction direction, BlockState neighborState) {
    if (Direction.Plane.VERTICAL.test(direction)) return state;
    final @Nullable BooleanProperty property;
    final Direction.Axis axis = state.getValue(AXIS);
    property = switch (axis) {
      case X -> direction == Direction.SOUTH ? LEFT : direction == Direction.NORTH ? RIGHT : null;
      case Z -> direction == Direction.WEST ? LEFT : direction == Direction.EAST ? RIGHT : null;
      default -> null;
    };
    if (property != null) {
      return state.setValue(property, neighborState.getBlock() instanceof HungSignBlock && neighborState.getValue(AXIS) == axis);
    } else {
      return state;
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState rotate(BlockState state, Rotation rotation) {
    final Direction.Axis oldAxis = state.getValue(AXIS);
    state = super.rotate(state, rotation)
        .setValue(AXIS,
            rotation == Rotation.CLOCKWISE_90
                || rotation == Rotation.COUNTERCLOCKWISE_90
                ? (oldAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X)
                : oldAxis);
    if (rotation == Rotation.CLOCKWISE_180
        || (oldAxis == Direction.Axis.X && rotation == Rotation.COUNTERCLOCKWISE_90)
        || (oldAxis == Direction.Axis.Z && rotation == Rotation.CLOCKWISE_90)) {
      state = state.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    return state;
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState mirror(BlockState state, Mirror mirror) {
    state = super.mirror(state, mirror);
    final Direction.Axis axis = state.getValue(AXIS);
    if ((axis == Direction.Axis.Z && mirror == Mirror.FRONT_BACK) || (axis == Direction.Axis.X && mirror == Mirror.LEFT_RIGHT)) {
      state = state.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    return state;
  }

  /**
   * 点击告示牌方块时，允许玩家对告示牌进行编辑。冒险模式的玩家无权进行编辑。 <br>
   * 本方法在编写时，适当参照了 {@link net.minecraft.item.SignItem#postPlacement(BlockPos, Level, Player,
   * ItemStack, BlockState)}。<br>
   * 告示牌界面的打开逻辑又可以参考 {@link
   * net.minecraft.client.network.LocalPlayer#openEditSignScreen(SignBlockEntity, boolean)} 和 {@link
   * net.minecraft.server.network.ServerPlayer#openEditSignScreen(SignBlockEntity, boolean)}。
   */
  @SuppressWarnings("deprecation")
  @Override
  public InteractionResult use(
      BlockState state,
      Level world,
      BlockPos pos,
      Player player,
      InteractionHand hand,
      BlockHitResult hit) {
    final InteractionResult actionResult = super.use(state, world, pos, player, hand, hit);
    if (actionResult != InteractionResult.PASS) return actionResult;
    final BlockEntity blockEntity = world.getBlockEntity(pos);
    final Direction side = hit.getDirection();
    if (!(blockEntity instanceof final HungSignBlockEntity entity)) {
      return InteractionResult.PASS;
    } else if (!state.getValue(AXIS).test(side)) {
      // 若方块实体不对应，或者编辑的这一侧不可编辑，则在客户端和服务器均略过。
      // Skip if the block entity does not correspond, or the side is not editable.
      return InteractionResult.PASS;
    } else if (!player.getAbilities().mayBuild) {
      // 冒险模式玩家无权编辑。Adventure players have no permission to edit.
      return InteractionResult.FAIL;
    } else if (world.isClientSide) {
      return InteractionResult.SUCCESS;
    } else {
      final ItemStack stackInHand = player.getItemInHand(hand);
      if (stackInHand.getItem() instanceof HoneycombItem) {
        // 处理告示牌的涂蜡
        if (!entity.waxed.contains(side)) {
          entity.waxed = addToSet(entity.waxed, side);
          player.displayClientMessage(BlockEntityWithText.MESSAGE_WAX_ON, true);
          world.levelEvent(null, 3003, entity.getBlockPos(), 0);
          entity.markDirtyAndUpdate();
          if (!player.isCreative()) stackInHand.shrink(1);
          return InteractionResult.SUCCESS;
        } else if (player.isCreative()) {
          entity.waxed = removeFromSet(entity.waxed, side);
          player.displayClientMessage(BlockEntityWithText.MESSAGE_WAX_OFF, true);
          world.levelEvent(null, 3004, entity.getBlockPos(), 0);
          entity.markDirtyAndUpdate();
          return InteractionResult.SUCCESS;
        }
      }
      if (entity.waxed.contains(side)) {
        // 涂蜡的告示牌不应该进行操作。
        world.playSound(null, entity.getBlockPos(), SoundEvents.WAXED_SIGN_INTERACT_FAIL, SoundSource.BLOCKS);
        return InteractionResult.PASS;
      } else if (stackInHand.is(Items.MAGMA_CREAM)) {
        // 玩家手持岩浆膏时，可快速进行重整。
        final List<@NotNull TextContext> textContexts = entity.texts.get(side);
        if (textContexts != null) MishangUtils.rearrange(textContexts);
        entity.markDirtyAndUpdate();
        return InteractionResult.SUCCESS;
      } else if (stackInHand.getItem() instanceof GlowInkSacItem) {
        if (!entity.glowing.contains(side)) {
          entity.glowing = addToSet(entity.glowing, side);
          player.displayClientMessage(BlockEntityWithText.MESSAGE_GLOW_ON, true);
          world.playSound(null, entity.getBlockPos(), SoundEvents.GLOW_INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
          entity.markDirtyAndUpdate();
          if (!player.isCreative()) stackInHand.shrink(1);
          return InteractionResult.SUCCESS;
        }
      } else if (stackInHand.getItem() instanceof InkSacItem) {
        if (entity.glowing.contains(side)) {
          entity.glowing = removeFromSet(entity.glowing, side);
          player.displayClientMessage(BlockEntityWithText.MESSAGE_GLOW_OFF, true);
          world.playSound(null, entity.getBlockPos(), SoundEvents.INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
          entity.markDirtyAndUpdate();
          if (!player.isCreative()) stackInHand.shrink(1);
          return InteractionResult.SUCCESS;
        }
      }
    }

    entity.checkEditorValidity();
    final Player editor = entity.getEditor();
    if (editor != null && editor != player) {
      // 这种情况下，告示牌被占用，玩家无权编辑。
      // In this case, the sign is occupied, and the player has no editing permission.
      player.displayClientMessage(TextBridge.translatable("message.mishanguc.no_editing_permission.occupied", editor.getName()), false);
      return InteractionResult.FAIL;
    }
    entity.editedSide = side;
    entity.setEditor(player);
    ServerPlayNetworking.send(
        ((ServerPlayer) player),
        new ResourceLocation("mishanguc", "edit_sign"),
        PacketByteBufs.create().writeBlockPos(pos).writeEnum(side));
    return InteractionResult.SUCCESS;
  }

  @Override
  public MutableComponent getName() {
    if (baseBlock != null) {
      return TextBridge.translatable("block.mishanguc.hung_sign", baseBlock.getName());
    }
    return super.getName();
  }

  @Override
  public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag options) {
    super.appendHoverText(stack, world, tooltip, options);
    tooltip.add(TextBridge.translatable("block.mishanguc.hung_sign.tooltip.1").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("block.mishanguc.hung_sign.tooltip.2").withStyle(ChatFormatting.GRAY));
  }

  public ResourceLocation getBaseTexture() {
    if (baseTexture != null) return baseTexture;
    return ModelHelper.getTextureOf(baseBlock == null ? this : baseBlock);
  }

  @Override
  public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    final ResourceLocation texture = getBaseTexture();
    final TextureMap textures = TextureMap.texture(texture);
    if (barTexture != null) textures.put(MishangucTextureKeys.BAR, barTexture);
    if (textureTop != null) textures.put(MishangucTextureKeys.TEXTURE_TOP, textureTop);

    final ResourceLocation id = MishangucModels.HUNG_SIGN.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation bodyId = MishangucModels.HUNG_SIGN_BODY.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation topBarId = MishangucModels.HUNG_SIGN_TOP_BAR.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation topBarEdgeId = MishangucModels.HUNG_SIGN_TOP_BAR_EDGE.upload(this, textures, blockStateModelGenerator.modelCollector);

    blockStateModelGenerator.blockStateCollector.accept(createBlockStates(bodyId, topBarId, topBarEdgeId));
    blockStateModelGenerator.registerParentedItemModel(this, id);
  }

  public @NotNull BlockStateSupplier createBlockStates(ResourceLocation bodyId, ResourceLocation topBarId, ResourceLocation topBarEdgeId) {
    return MultipartBlockStateSupplier.create(this)
        .setValue(When.create().set(AXIS, Direction.Axis.Z), BlockStateVariant.create().put(VariantSettings.MODEL, bodyId).put(VariantSettings.UVLOCK, true))
        .setValue(When.create().set(AXIS, Direction.Axis.X), BlockStateVariant.create().put(VariantSettings.MODEL, bodyId).put(VariantSettings.UVLOCK, true).put(VariantSettings.Y, VariantSettings.Rotation.R90))
        .setValue(When.create().set(AXIS, Direction.Axis.Z).set(LEFT, false).set(RIGHT, true), BlockStateVariant.create().put(VariantSettings.MODEL, topBarId).put(VariantSettings.UVLOCK, true))
        .setValue(When.create().set(AXIS, Direction.Axis.Z).set(LEFT, true).set(RIGHT, false), BlockStateVariant.create().put(VariantSettings.MODEL, topBarId).put(VariantSettings.UVLOCK, true).put(MishangUtils.INT_Y_VARIANT, 180))
        .setValue(When.create().set(AXIS, Direction.Axis.X).set(LEFT, false).set(RIGHT, true), BlockStateVariant.create().put(VariantSettings.MODEL, topBarId).put(VariantSettings.UVLOCK, true).put(MishangUtils.INT_Y_VARIANT, -90))
        .setValue(When.create().set(AXIS, Direction.Axis.X).set(LEFT, true).set(RIGHT, false), BlockStateVariant.create().put(VariantSettings.MODEL, topBarId).put(VariantSettings.UVLOCK, true).put(MishangUtils.INT_Y_VARIANT, 90))
        .setValue(When.create().set(AXIS, Direction.Axis.Z).set(LEFT, false).set(RIGHT, false), BlockStateVariant.create().put(VariantSettings.MODEL, topBarEdgeId).put(VariantSettings.UVLOCK, true))
        .setValue(When.create().set(AXIS, Direction.Axis.Z).set(LEFT, false).set(RIGHT, false), BlockStateVariant.create().put(VariantSettings.MODEL, topBarEdgeId).put(VariantSettings.UVLOCK, true).put(MishangUtils.INT_Y_VARIANT, 180))
        .setValue(When.create().set(AXIS, Direction.Axis.X).set(LEFT, false).set(RIGHT, false), BlockStateVariant.create().put(VariantSettings.MODEL, topBarEdgeId).put(VariantSettings.UVLOCK, true).put(MishangUtils.INT_Y_VARIANT, 90))
        .setValue(When.create().set(AXIS, Direction.Axis.X).set(LEFT, false).set(RIGHT, false), BlockStateVariant.create().put(VariantSettings.MODEL, topBarEdgeId).put(VariantSettings.UVLOCK, true).put(MishangUtils.INT_Y_VARIANT, 270));
  }

  private @Nullable String getRecipeGroup() {
    if (baseBlock instanceof ColoredBlock) return null;
    if (MishangUtils.isWood(baseBlock)) return "mishanguc:wood_hung_sign";
    if (MishangUtils.isStrippedWood(baseBlock)) return "mishanguc:stripped_wood_hung_sign";
    if (MishangUtils.isPlanks(baseBlock)) return "mishanguc:plank_wood_hung_sign";
    if (MishangUtils.isConcrete(baseBlock)) return "mishanguc:concrete_hung_sign";
    if (MishangUtils.isTerracotta(baseBlock)) return "mishanguc:terracotta_hung_sign";
    if (baseBlock == Blocks.ICE || baseBlock == Blocks.PACKED_ICE || baseBlock == Blocks.BLUE_ICE) {
      return "mishanguc:ice_hung_sign";
    }
    return null;
  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    if (baseBlock == null) return null;
    return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, this, 6)
        .pattern("-#-")
        .pattern("-#-")
        .pattern("-#-")
        .define('#', baseBlock)
        .define('-', WallSignBlocks.INVISIBLE_WALL_SIGN)
        .unlockedBy("has_base_block", FabricRecipeProvider.has(baseBlock))
        .unlockedBy("has_sign", FabricRecipeProvider.has(WallSignBlocks.INVISIBLE_WALL_SIGN))
        .group(getRecipeGroup());
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
    if (direction.getAxis().isHorizontal() && state.getBlock() instanceof HungSignBlock && stateFrom.getBlock() instanceof HungSignBlock hungSignBlockFrom && state.getValue(AXIS) == stateFrom.getValue(AXIS) && direction.getAxis() != state.getValue(AXIS)) {
      if (hungSignBlockFrom.baseBlock instanceof HalfTransparentBlock) {
        if (baseBlock instanceof HalfTransparentBlock) {
          // 自身和相邻方块都为透明方块，则双方均为同一方块时隐藏。
          return baseBlock == hungSignBlockFrom.baseBlock;
        } else {
          return false;
        }
      }
      return true;
    } else {
      return super.skipRendering(state, stateFrom, direction);
    }
  }

  /**
   * 往集合中添加一个值，并返回添加后的集合（可能会是新集合）。这样做是为了避免使用空集时创建集合对象。
   */
  private static <T> Set<T> addToSet(Set<T> set, T element) {
    if (set.isEmpty()) {
      final HashSet<T> newSet = new HashSet<>(2);
      newSet.add(element);
      return newSet;
    } else {
      set.add(element);
      return set;
    }
  }

  /**
   * 从集合中移除一个值，如果移除后的集合为空集合，则返回不可变的空集合，以避免产生不必要的对象。
   */
  private static <T> Set<T> removeFromSet(Set<T> set, T element) {
    if (set.isEmpty()) {
      return set;
    } else if (set.remove(element) && set.isEmpty()) {
      return Set.of();
    } else {
      set.remove(element);
      return set;
    }
  }

  @Override
  public String customRecipeCategory() {
    return "signs";
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {
    return false;
  }
}
