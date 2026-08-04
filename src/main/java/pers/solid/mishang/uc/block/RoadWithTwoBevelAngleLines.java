package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
// TODO: Forge data gen - BlockStateModelGenerator
import pers.solid.mishang.uc.data.stubs.TextureMap;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.blocks.RoadBlocks;
import pers.solid.mishang.uc.data.FasterTextureMap;
import pers.solid.mishang.uc.data.MishangucTextureKeys;
import pers.solid.mishang.uc.util.*;

import java.util.List;

/**
 * <p>带有两个相邻斜线的道路，这两个斜线可以连成V字形。这样的双斜线道路又分为以下情况：
 * <p>是否还有一条中线：将决定道路显示是两条线还是三条线。
 * <p>是否需要适应双线连接，这种情况下道路不能是两个斜线材质的简单叠加，而应该进行特殊适应。
 */
@ApiStatus.AvailableSince("1.1.0")
public interface RoadWithTwoBevelAngleLines extends Road {
  DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

  @Override
  default void appendRoadProperties(StateDefinition.Builder<Block, BlockState> builder) {
    Road.super.appendRoadProperties(builder);
    builder.add(FACING);
  }

  @Override
  default void appendRoadTooltip(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag options) {
    Road.super.appendRoadTooltip(stack, world, tooltip, options);
    tooltip.add(TextBridge.translatable("block.mishanguc.tooltip.road_with_bi_bevel_angle_line.1").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("block.mishanguc.tooltip.road_with_bi_bevel_angle_line.2").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("block.mishanguc.tooltip.road_with_bi_bevel_angle_line.3").withStyle(ChatFormatting.GRAY));
  }

  @Override
  default BlockState rotateRoad(BlockState state, Rotation rotation) {
    return Road.super.rotateRoad(state, rotation).setValue(FACING, rotation.rotate(state.getValue(FACING)));
  }

  @Override
  default BlockState mirrorRoad(BlockState state, Mirror mirror) {
    return Road.super.mirrorRoad(state, mirror).setValue(FACING, mirror.mirror(state.getValue(FACING)));
  }

  @Override
  default BlockState withPlacementState(BlockState state, BlockPlaceContext ctx) {
    final Direction playerFacing = ctx.getHorizontalDirection();
    return Road.super.withPlacementState(state, ctx).setValue(FACING, ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown() ? playerFacing.getOpposite() : playerFacing);
  }

  class ImplWithTwoLayerTexture extends AbstractRoadBlock implements RoadWithTwoBevelAngleLines {

    public ImplWithTwoLayerTexture(Properties settings, LineColor lineColor, LineType lineType) {
      super(settings, lineColor, lineType);
      registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected <B extends Block & Road> void registerBaseOrSlabModels(B road, BlockStateModelGenerator blockStateModelGenerator) {
      TextureMap textures = new FasterTextureMap()
          .base("asphalt")
          .lineTop(MishangUtils.composeAngleLineTexture(lineColor, lineType, true))
          .lineSide(MishangUtils.composeStraightLineTexture(lineColor, lineType));
      final ResourceLocation modelId = road.uploadModel("_with_bi_angle_line", textures, blockStateModelGenerator, MishangucTextureKeys.BASE, MishangucTextureKeys.LINE_TOP, MishangucTextureKeys.LINE_SIDE);
      blockStateModelGenerator.blockStateCollector.accept(road.composeState(BlockStateModelGenerator.createSingletonBlockState(road, modelId).coordinate(BlockStateModelGenerator.createSouthDefaultHorizontalRotationStates())));
    }

    @Override
    public RoadConnectionState getConnectionStateOf(BlockState state, Direction direction) {
      final Direction facing = state.getValue(FACING);
      if (facing == direction) {
        return new RoadConnectionState(RoadConnectionState.WhetherConnected.CONNECTED, lineColor, EightHorizontalDirection.of(direction), lineType);
      } else if (facing != direction.getOpposite()) {
        return new RoadConnectionState(RoadConnectionState.WhetherConnected.CONNECTED, lineColor, EightHorizontalDirection.of(HorizontalCornerDirection.fromDirections(facing, direction.getOpposite())), lineType);
      }
      return super.getConnectionStateOf(state, direction);
    }

    @Override
    public void appendDescriptionTooltip(List<Component> tooltip, TooltipFlag options) {
      tooltip.add(TextBridge.translatable("lineType.biBevelAngleLine", lineColor.getName(), lineType.getName()).withStyle(ChatFormatting.BLUE));
    }

    @Override
    public RecipeBuilder getPaintingRecipe(Block base, Block self) {
      return ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, self, 3)
          .pattern(" **")
          .pattern("** ")
          .pattern("XXX")
          .define('*', lineColor.getIngredient())
          .define('X', base)
          .unlockedBy("has_paint", FabricRecipeProvider.has(lineColor.getIngredient()))
          .unlockedBy(FabricRecipeProvider.getHasName(base), FabricRecipeProvider.has(base));
    }
  }

  class ImplWithThreeLayerTexture extends AbstractRoadBlock implements RoadWithTwoBevelAngleLines {

    public ImplWithThreeLayerTexture(Properties settings, LineColor lineColor, LineType lineType) {
      super(settings, lineColor, lineType);
      registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected <B extends Block & Road> void registerBaseOrSlabModels(B road, BlockStateModelGenerator blockStateModelGenerator) {
      final TextureMap textures = new FasterTextureMap()
          .base("asphalt")
          .lineTop(MishangUtils.composeStraightLineTexture(lineColor, lineType))
          .lineSide(MishangUtils.composeStraightLineTexture(lineColor, lineType))
          .lineTop2(MishangUtils.composeAngleLineTexture(lineColor, lineType, true));
      final ResourceLocation modelId = road.uploadModel("_with_straight_and_bi_angle_line", textures, blockStateModelGenerator, MishangucTextureKeys.BASE, MishangucTextureKeys.LINE_TOP, MishangucTextureKeys.LINE_SIDE, MishangucTextureKeys.LINE_TOP2);
      blockStateModelGenerator.blockStateCollector.accept(road.composeState(BlockStateModelGenerator.createSingletonBlockState(road, modelId).coordinate(BlockStateModelGenerator.createSouthDefaultHorizontalRotationStates())));
    }

    @Override
    public RoadConnectionState getConnectionStateOf(BlockState state, Direction direction) {
      final Direction facing = state.getValue(FACING);
      if (facing == direction || facing == direction.getOpposite()) {
        return new RoadConnectionState(RoadConnectionState.WhetherConnected.CONNECTED, lineColor, EightHorizontalDirection.of(direction), lineType);
      } else {
        return new RoadConnectionState(RoadConnectionState.WhetherConnected.CONNECTED, lineColor, EightHorizontalDirection.of(HorizontalCornerDirection.fromDirections(facing, direction.getOpposite())), lineType);
      }
    }

    @Override
    public void appendDescriptionTooltip(List<Component> tooltip, TooltipFlag options) {
      tooltip.add(TextBridge.translatable("lineType.biBevelAngleLine", lineColor.getName(), lineType.getName()).withStyle(ChatFormatting.BLUE));
    }

    @Override
    public RecipeBuilder getPaintingRecipe(Block base, Block self) {
      Block base2 = RoadBlocks.getRoadBlockWithLine(lineColor, lineType);
      if (base instanceof SlabBlock) {
        base2 = ((AbstractRoadBlock) base2).getRoadSlab();
      }
      return ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, self, 3)
          .pattern(" *X")
          .pattern("*X*")
          .pattern("X* ")
          .define('*', lineColor.getIngredient())
          .define('X', base2)
          .unlockedBy("has_paint", FabricRecipeProvider.has(lineColor.getIngredient()))
          .unlockedBy(FabricRecipeProvider.getHasName(base2), FabricRecipeProvider.has(base2));
    }
  }
}
