package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.*;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
import pers.solid.mishang.uc.data.stubs.VariantSettings;
import pers.solid.mishang.uc.data.stubs.Model;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.data.FasterTextureMap;
import pers.solid.mishang.uc.data.MishangucTextureKeys;
import pers.solid.mishang.uc.util.*;

import java.util.List;
import com.mojang.math.Axis;

public interface RoadWithStraightLine extends Road {
  EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

  @Override
  default void appendRoadProperties(StateDefinition.Builder<Block, BlockState> builder) {
    Road.super.appendRoadProperties(builder);
    builder.add(AXIS);
  }

  @Override
  default RoadConnectionState getConnectionStateOf(BlockState state, Direction direction) {
    Direction.Axis axis = state.getValue(AXIS);
    return RoadConnectionState.of(
        direction.getAxis() == axis,
        getLineColor(state, direction),
        EightHorizontalDirection.of(direction),
        getLineType(state, direction), null);
  }

  @Override
  default BlockState rotateRoad(BlockState state, Rotation rotation) {
    Direction.Axis axis = state.getValue(AXIS);
    Direction.Axis rotatedAxis = switch (rotation) {
      case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> axis == Direction.Axis.X
          ? Direction.Axis.Z
          : axis == Direction.Axis.Z ? Direction.Axis.X : axis;
      default -> axis;
    };
    return state.setValue(AXIS, rotatedAxis);
  }

  @Override
  default BlockState withPlacementState(BlockState state, BlockPlaceContext ctx) {
    final Player player = ctx.getPlayer();
    final Direction playerFacing = ctx.getHorizontalDirection();
    return state.setValue(
        AXIS,
        (player != null && player.isShiftKeyDown() ? playerFacing.getClockWise() : playerFacing)
            .getAxis());
  }

  @Override
  default void appendRoadTooltip(
      ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag options) {
    Road.super.appendRoadTooltip(stack, world, tooltip, options);
    tooltip.add(
        TextBridge.translatable("block.mishanguc.tooltip.road_with_straight_line.1")
            .withStyle(ChatFormatting.GRAY));
    tooltip.add(
        TextBridge.translatable("block.mishanguc.tooltip.road_with_straight_line.2")
            .withStyle(ChatFormatting.GRAY));
  }

  class Impl extends AbstractRoadBlock implements RoadWithStraightLine {
    private final String lineTexture;

    public Impl(Properties settings, LineColor lineColor, LineType lineType, String lineTexture) {
      super(settings, lineColor, lineType);
      this.lineTexture = lineTexture;
    }

    @Override
    public void appendDescriptionTooltip(List<Component> tooltip, TooltipFlag options) {
      tooltip.add(TextBridge.translatable("lineType.straight.composed", lineColor.getName(), lineType.getName()).withStyle(ChatFormatting.BLUE));
    }

    @Override
    protected <B extends Block & Road> void registerBaseOrSlabModels(B road, BlockStateModelGenerator blockStateModelGenerator) {
      final TextureMap textures = new FasterTextureMap().base("asphalt").lineSide(lineTexture).lineTop(lineTexture);
      final ResourceLocation modelId = road.uploadModel("_with_straight_line", textures, blockStateModelGenerator, MishangucTextureKeys.BASE, MishangucTextureKeys.LINE_SIDE, MishangucTextureKeys.LINE_TOP);
      blockStateModelGenerator.blockStateCollector.accept(road.composeState(VariantsBlockStateSupplier.create(road, BlockStateVariant.create().put(VariantSettings.MODEL, modelId))
          .coordinate(BlockStateVariantMap.create(AXIS)
              .register(Direction.Axis.X, ImmutableList.of(BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R90), BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R270)))
              .register(Direction.Axis.Z, ImmutableList.of(BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R0), BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R180))))));
    }

    @Override
    public RecipeBuilder getPaintingRecipe(Block base, Block self) {
      final String[] patterns = switch (lineType) {
        case NORMAL -> new String[]{
            " * ",
            "XXX",
            " * "
        };
        case DOUBLE -> new String[]{
            "* *",
            "XXX",
            "* *"
        };
        case THICK -> new String[]{
            "***",
            "XXX",
            "***"
        };
      };
      return ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, self, 3)
          .pattern(patterns[0])
          .pattern(patterns[1])
          .pattern(patterns[2])
          .define('*', lineColor.getIngredient())
          .define('X', base)
          .unlockedBy("has_paint", FabricRecipeProvider.has(lineColor.getIngredient()))
          .unlockedBy(FabricRecipeProvider.getHasName(base), FabricRecipeProvider.has(base));
    }
  }
}
