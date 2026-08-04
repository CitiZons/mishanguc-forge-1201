package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
// TODO: Forge data gen - BlockStateModelGenerator
import pers.solid.mishang.uc.data.stubs.BlockStateSupplier;
import pers.solid.mishang.uc.data.stubs.TextureKey;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.util.*;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.blocks.RoadBlocks;
import pers.solid.mishang.uc.util.EightHorizontalDirection;
import pers.solid.mishang.uc.util.LineColor;
import pers.solid.mishang.uc.util.LineType;
import pers.solid.mishang.uc.util.RoadConnectionState;

import java.util.List;

/**
 * 所有道路方块类型均实现的接口。接口可以多重继承，并直接实现于已有类上，因此使用接口。
 */
public interface Road extends MishangucBlock {

  /**
   * 获取该方块状态中，某个特定方向上的连接状态。连接状态可用于自动路块。
   *
   * @param state     方块状态。
   * @param direction 水平方向。
   * @return 连接状态。
   */
  default RoadConnectionState getConnectionStateOf(BlockState state, Direction direction) {
    return new RoadConnectionState(RoadConnectionState.WhetherConnected.NOT_CONNECTED, getLineColor(state, direction), EightHorizontalDirection.of(direction), LineType.NORMAL);
  }

  /**
   * 实现此接口的类，应当覆盖 <code>createBlockStateDefinition</code> 并使用此方法。
   *
   * @param builder <code>createBlockStateDefinition</code> 方法中的 builder。
   */
  default void appendRoadProperties(StateDefinition.Builder<Block, BlockState> builder) {
  }

  /**
   * 实现此接口的类，应当覆盖 <code>mirror</code> 并使用此方法。
   *
   * @param state  <code>mirror</code> 中的 state。
   * @param mirror <code>mirror</code> 中的 mirror。
   * @return 镜像后的方块状态。
   */
  default BlockState mirrorRoad(BlockState state, Mirror mirror) {
    return state;
  }

  /**
   * 实现此接口的类，应当覆盖 <code>rotate</code> 并使用此方法。
   *
   * @param state    <code>rotate</code> 中的 state。
   * @param rotation <code>rotate</code> 中的 rotation。
   * @return 旋转后的方块状态。
   */
  default BlockState rotateRoad(BlockState state, Rotation rotation) {
    return state;
  }

  /**
   * 追加放置状态。 实现此接口的类，应当覆盖 <code>getStateForPlacement</code> 并使用此方法。
   *
   * @param state 需要被修改的方块状态，一般是 <code>super.getStateForPlacement(ctx)</code> 或者 <code>
   *              this.getDefaultState</code>（其中 this 是方块）。
   * @param ctx   <code>getStateForPlacement</code> 中的 ctx。
   * @return 追加后的方块状态。
   */
  default BlockState withPlacementState(BlockState state, BlockPlaceContext ctx) {
    return state;
  }

  /**
   * 处理方块更新。实现此接口的类，应该覆盖 {@link Block#updateShape} 并使用此方法。
   *
   * @since 0.2.4
   */
  @ApiStatus.AvailableSince("0.2.4")
  default BlockState withStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
    return state;
  }

  /**
   * 对道路进行使用的操作。
   *
   * @param state  该道路方块的方块状态。
   * @param world  所在的世界。
   * @param pos    该道路所在的坐标。
   * @param player 使用的玩家。
   * @param hand   玩家使用道路时使用的手。
   * @param hit    玩家使用道路时的碰撞结果。
   * @return 行为结果。
   */
  default InteractionResult onUseRoad(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    return InteractionResult.PASS;
  }

  /**
   * 附近有方块更新时的操作。
   *
   * @param state       方块状态。
   * @param world       世界。
   * @param pos         坐标。
   * @param sourceBlock 方块。
   * @param sourcePos   导致触发方块更新的方块。
   * @param notify      一个布尔值。
   * @see AbstractRoadBlock#neighborChanged
   * @see AbstractRoadSlabBlock#neighborChanged
   * @see Block#neighborChanged
   * @see BlockState#neighborChanged
   */
  @SuppressWarnings("deprecation")
  default void neighborRoadUpdate(
      BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
  }

  /**
   * 在物品栏中为该道路添加提示信息。<br>
   * 对于 1.16.5 之前的版本，子类覆盖此方法时，必须注解为 {@code @OnlyIn(Dist.CLIENT)}。<br>
   * 新版本中，由于 {@link Block#appendHoverText(ItemStack, BlockGetter, List, TooltipFlag)} 没有再被注解，故此方法也无需再被注解。
   *
   * @param stack   物品堆。
   * @param world   世界。
   * @param tooltip 提示文字。
   * @param options 提示选项。
   * @see AbstractRoadBlock#appendRoadTooltip
   * @see AbstractRoadSlabBlock#appendHoverText
   * @see Block#appendHoverText
   */
  default void appendRoadTooltip(
      ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag options) {
  }

  LineColor getLineColor(BlockState blockState, Direction direction);

  LineType getLineType(BlockState blockState, Direction direction);

  /**
   * 给道路添加描述性内容，这部分文本通常是蓝色的。
   */
  @ApiStatus.AvailableSince("0.2.4")
  void appendDescriptionTooltip(List<Component> tooltip, TooltipFlag options);

  default RecipeBuilder getPaintingRecipe(Block base, Block self) {
    return null;
  }

  default ResourceLocation getPaintingRecipeId() {
    return FabricRecipeProvider.getConversionRecipeName((ItemLike) this).withSuffix("_from_painting");
  }

  default @Nullable String getRecipeGroup() {
    final ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(((ItemLike) this).asItem());
    return itemId.getNamespace() + ":" + StringUtils.replaceEach(itemId.getPath(), new String[]{"_white_", "_yellow_", "_w_", "_y_"}, new String[]{"_", "_", "_", "_"});
  }

  CauldronInteraction CLEAN_ROAD_BLOCK = (state, world, pos, player, hand, stack) -> {
    if (stack.getItem() instanceof BlockItem blockItem) {
      final Block block = blockItem.getBlock();
      if ((block instanceof AbstractRoadBlock || block instanceof AbstractRoadSlabBlock) && block != RoadBlocks.ROAD_BLOCK && block != RoadBlocks.ROAD_BLOCK.getRoadSlab()) {
        if (!world.isClientSide) {
          if (!player.getAbilities().instabuild) {
            stack.shrink(1);
          }
          final ItemStack itemStack = block instanceof AbstractRoadSlabBlock ? new ItemStack(RoadBlocks.ROAD_BLOCK.getRoadSlab()) : new ItemStack(RoadBlocks.ROAD_BLOCK);
          if (stack.isEmpty()) {
            player.setItemInHand(hand, itemStack);
          } else if (player.getInventory().add(itemStack)) {
            player.containerMenu.broadcastChanges();
          } else {
            player.drop(itemStack, false);
          }
          LayeredCauldronBlock.lowerFillLevel(state, world, pos);
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
      }
    }
    return InteractionResult.PASS;
  };

  /**
   * 对于道路方块，返回 {@code "road" + suffix}。对于道路台阶方块，返回 {@code "road_slab" + suffix}。
   */
  String getModelName(String suffix);

  /**
   * 生成方块的模型。如果此方块是台阶方块，则生成下半和上半台阶方块的模型，共两个模型，其中返回下半台阶方块的模型的 ID。
   *
   * @return 生成的方块模型的 ID。如果是台阶方块，则是下半台阶方块的 ID。
   */
  ResourceLocation uploadModel(String suffix, TextureMap textureMap, BlockStateModelGenerator blockStateModelGenerator, TextureKey... textureKeys);

  /**
   * 生成方块的模型。如果此方块是台阶方块，则生成下半和上半台阶方块的模型，共两个模型，其中返回下半台阶方块的模型的 ID。
   *
   * @return 生成的方块模型的 ID。如果是台阶方块，则是下半台阶方块的 ID。
   */
  ResourceLocation uploadModel(String suffix, String variant, TextureMap textureMap, BlockStateModelGenerator blockStateModelGenerator, TextureKey... textureKeys);

  /**
   * 对于道路方块，直接返回 {@code stateForFull}。对于道路台阶方块，会将其转化为台阶的方块状态再返回。
   */
  BlockStateSupplier composeState(@NotNull BlockStateSupplier stateForFull);

  @Override
  default String customRecipeCategory() {
    return "roads";
  }
}
