package pers.solid.mishang.uc.block;

import net.minecraft.Util;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.*;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
import pers.solid.mishang.uc.data.stubs.VariantSettings;
import pers.solid.mishang.uc.data.stubs.Model;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.util.*;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.data.FasterTextureMap;
import pers.solid.mishang.uc.data.MishangucTextureKeys;
import pers.solid.mishang.uc.util.*;
import net.minecraft.ChatFormatting;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;

public interface RoadWithAngleLineWithOnePartOffset extends RoadWithAngleLine {
  /**
   * 该道路方块的直角两边中，哪个轴上的保持中心（另一个轴的将会偏移）。
   */
  EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

  @Override
  default void appendRoadProperties(StateDefinition.Builder<Block, BlockState> builder) {
    RoadWithAngleLine.super.appendRoadProperties(builder);
    builder.add(AXIS);
  }

  @Override
  default BlockState mirrorRoad(BlockState state, Mirror mirror) {
    return RoadWithAngleLine.super.mirrorRoad(state, mirror);
  }

  @Override
  default BlockState rotateRoad(BlockState state, Rotation rotation) {
    return RoadWithAngleLine.super
        .rotateRoad(state, rotation)
        .setValue(
            AXIS,
            Util.make(
                () -> {
                  final Direction.Axis axis = state.getValue(AXIS);
                  return switch (rotation) {
                    case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (axis) {
                      case X -> (Direction.Axis.Z);
                      case Z -> (Direction.Axis.X);
                      default -> axis;
                    };
                    default -> axis;
                  };
                }));
  }

  @Override
  default RoadConnectionState getConnectionStateOf(BlockState state, Direction direction) {
    final RoadConnectionState connectionState = RoadWithAngleLine.super.getConnectionStateOf(state, direction);
    if (connectionState.mayConnect() && direction.getAxis() != state.getValue(AXIS)) {
      return connectionState.createWithOffset(LineOffset.of(state.getValue(FACING).getDirectionInAxis(direction.getClockWise().getAxis()).getOpposite(), offsetOutwards()));
    } else {
      return connectionState;
    }
  }

  @Override
  default BlockState withPlacementState(BlockState state, BlockPlaceContext ctx) {
    return RoadWithAngleLine.super
        .withPlacementState(state, ctx)
        .setValue(AXIS, ctx.getHorizontalDirection().getAxis());
  }

  @Override
  default void appendRoadTooltip(
      ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag options) {
    RoadWithAngleLine.super.appendRoadTooltip(stack, world, tooltip, options);
    tooltip.add(
        TextBridge.translatable("block.mishanguc.tooltip.road_with_angle_line_with_one_part_offset.1")
            .withStyle(ChatFormatting.GRAY));
    tooltip.add(
        TextBridge.translatable("block.mishanguc.tooltip.road_with_angle_line_with_one_part_offset.2")
            .withStyle(ChatFormatting.GRAY));
  }

  int offsetOutwards();

  class Impl extends RoadWithAngleLine.Impl implements RoadWithAngleLineWithOnePartOffset {
    private final String lineSide2;
    private final int offsetOutwards;

    public Impl(Properties settings, LineColor lineColor, boolean isBevel, String lineSide, String lineTop, int offsetOutwards) {
      super(settings, lineColor, LineType.NORMAL, lineSide, isBevel, lineTop);
      this.lineSide2 = MishangUtils.composeStraightLineTexture(lineColor, LineType.NORMAL);
      this.offsetOutwards = offsetOutwards;
    }

    @Override
    public int offsetOutwards() {
      return offsetOutwards;
    }

    @Override
    protected <B extends Block & Road> void registerBaseOrSlabModels(B road, BlockStateModelGenerator blockStateModelGenerator) {
      final FasterTextureMap textures = new FasterTextureMap().base("asphalt")
          .lineSide(lineSide)
          .lineSide2(lineSide2)
          .lineTop(lineTop);
      final ResourceLocation modelId = road.uploadModel("_with_angle_line", textures, blockStateModelGenerator, MishangucTextureKeys.BASE, MishangucTextureKeys.LINE_SIDE, MishangucTextureKeys.LINE_SIDE2, MishangucTextureKeys.LINE_TOP);
      final ResourceLocation mirroredModelId = road.uploadModel("_with_angle_line_mirrored", "_mirrored", textures, blockStateModelGenerator, MishangucTextureKeys.BASE, MishangucTextureKeys.LINE_SIDE, MishangucTextureKeys.LINE_SIDE2, MishangucTextureKeys.LINE_TOP);
      final BlockStateVariantMap.DoubleProperty<HorizontalCornerDirection, Direction.Axis> map = BlockStateVariantMap.create(FACING, AXIS);
      for (Direction direction : Direction.Plane.HORIZONTAL) {
        // direction：正中线所朝的方向
        final Direction offsetDirection1 = direction.getClockWise();
        final Direction offsetDirection2 = direction.getCounterClockWise();
        map.register(
            HorizontalCornerDirection.fromDirections(direction, offsetDirection1),
            direction.getAxis(),
            BlockStateVariant.create().put(VariantSettings.MODEL, modelId).put(MishangUtils.DIRECTION_Y_VARIANT, direction));
        map.register(
            HorizontalCornerDirection.fromDirections(direction, offsetDirection2),
            direction.getAxis(),
            BlockStateVariant.create().put(VariantSettings.MODEL, mirroredModelId)
                .put(MishangUtils.DIRECTION_Y_VARIANT, direction));
      }
      blockStateModelGenerator.blockStateCollector.accept(road.composeState(VariantsBlockStateSupplier.create(road).coordinate(map)));
    }

    @Override
    public RecipeBuilder getPaintingRecipe(Block base, Block self) {
      if (isBevel()) {
        throw new UnsupportedOperationException("Recipes for bevel line with one part offset is not supported!");
      }
      return ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, self, 3)
          .pattern("  *")
          .pattern("*XX")
          .pattern(" X ")
          .define('*', lineColor.getIngredient())
          .define('X', base)
          .unlockedBy("has_paint", FabricRecipeProvider.has(lineColor.getIngredient()))
          .unlockedBy(FabricRecipeProvider.getHasName(base), FabricRecipeProvider.has(base));
    }
  }
}
