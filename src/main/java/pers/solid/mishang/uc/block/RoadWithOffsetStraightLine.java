package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.*;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
import pers.solid.mishang.uc.data.stubs.BlockStateSupplier;
import pers.solid.mishang.uc.data.stubs.VariantSettings;
import pers.solid.mishang.uc.data.stubs.Model;
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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.data.FasterTextureMap;
import pers.solid.mishang.uc.data.MishangucTextureKeys;
import pers.solid.mishang.uc.util.*;

import java.util.List;

/**
 * 类似于 {@link RoadWithStraightLine}，不过道路的直线是偏移的，而非正中的。
 */
public interface RoadWithOffsetStraightLine extends Road {
  /**
   * 道路偏移直线所偏移的反方向。例如道路有一条南北方向的向西偏移的直线，则该道路朝向东。
   */
  DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

  @Override
  default void appendRoadProperties(StateDefinition.Builder<Block, BlockState> builder) {
    Road.super.appendRoadProperties(builder);
    builder.add(FACING);
  }

  @Override
  default RoadConnectionState getConnectionStateOf(BlockState state, Direction direction) {
    return RoadConnectionState.or(
        Road.super.getConnectionStateOf(state, direction),
        RoadConnectionState.of(
            direction.getAxis() != state.getValue(FACING).getAxis(),
            getLineColor(state, direction),
            EightHorizontalDirection.of(direction),
            getLineType(state, direction),
            new LineOffset(state.getValue(FACING).getOpposite(), offsetLevel())));
  }

  @Override
  default BlockState mirrorRoad(BlockState state, Mirror mirror) {
    return Road.super.mirrorRoad(state, mirror).setValue(FACING, mirror.mirror(state.getValue(FACING)));
  }

  @Override
  default BlockState rotateRoad(BlockState state, Rotation rotation) {
    return Road.super.rotateRoad(state, rotation).setValue(FACING, rotation.rotate(state.getValue(FACING)));
  }

  @Override
  default BlockState withPlacementState(BlockState state, BlockPlaceContext ctx) {
    return Road.super
        .withPlacementState(state, ctx)
        .setValue(
            FACING,
            ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()
                ? ctx.getHorizontalDirection().getCounterClockWise()
                : ctx.getHorizontalDirection().getClockWise());
  }

  @Override
  default void appendRoadTooltip(
      ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag options) {
    Road.super.appendRoadTooltip(stack, world, tooltip, options);
    final int offsetLevel = offsetLevel();
    if (offsetLevel == 114514) {
      tooltip.add(TextBridge.translatable("block.mishanguc.tooltip.road_with_white_and_yellow_double_line.1").withStyle(ChatFormatting.GRAY));
      tooltip.add(TextBridge.translatable("block.mishanguc.tooltip.road_with_white_and_yellow_double_line.2").withStyle(ChatFormatting.GRAY));
      tooltip.add(TextBridge.translatable("block.mishanguc.tooltip.road_with_white_and_yellow_double_line.3").withStyle(ChatFormatting.GRAY));
    } else {
      tooltip.add(
          TextBridge.translatable("block.mishanguc.tooltip.road_with_offset_straight_line")
              .withStyle(ChatFormatting.GRAY));
    }
  }

  default @NotNull BlockStateSupplier createBlockStates(Block block, ResourceLocation modelId) {
    return VariantsBlockStateSupplier.create(block, BlockStateVariant.create().put(VariantSettings.MODEL, modelId).put(VariantSettings.UVLOCK, false)).coordinate(BlockStateVariantMap.create(FACING).register(direction -> BlockStateVariant.create().put(MishangUtils.DIRECTION_Y_VARIANT, direction.getClockWise())));
  }

  @Contract(pure = true)
  int offsetLevel();

  class Impl extends AbstractRoadBlock implements RoadWithOffsetStraightLine {
    private final String lineTexture;
    private final int offsetLevel;

    public Impl(Properties settings, LineColor lineColor, LineType lineType, String lineTexture, int offsetLevel) {
      super(settings, lineColor, lineType);
      this.lineTexture = lineTexture;
      this.offsetLevel = offsetLevel;
    }

    @Override
    public void appendDescriptionTooltip(List<Component> tooltip, TooltipFlag options) {
      if (offsetLevel == 0) {
        tooltip.add(TextBridge.translatable("tbd")
            .withStyle(ChatFormatting.BLUE));
      } else {
        tooltip.add(TextBridge.translatable("lineType.offsetStraight.composed", lineColor.getName(), lineType.getName()).withStyle(ChatFormatting.BLUE));
      }
    }

    @Override
    protected <B extends Block & Road> void registerBaseOrSlabModels(B road, BlockStateModelGenerator blockStateModelGenerator) {
      final FasterTextureMap textures = new FasterTextureMap().base("asphalt").lineSide(lineTexture).lineTop(lineTexture);
      final ResourceLocation modelId = road.uploadModel("_with_straight_line", textures, blockStateModelGenerator, MishangucTextureKeys.BASE, MishangucTextureKeys.LINE_SIDE, MishangucTextureKeys.LINE_TOP);
      blockStateModelGenerator.blockStateCollector.accept(road.composeState(createBlockStates(road, modelId)));
    }

    @Override
    public RecipeBuilder getPaintingRecipe(Block base, Block self) {
      if (offsetLevel == 114514) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, self, 3)
            .pattern("w y")
            .pattern("XXX")
            .pattern("w y")
            .define('w', LineColor.WHITE.getIngredient())
            .define('y', LineColor.YELLOW.getIngredient())
            .define('X', base)
            .unlockedBy("has_white_paint", FabricRecipeProvider.has(LineColor.WHITE.getIngredient()))
            .unlockedBy("has_yellow_paint", FabricRecipeProvider.has(LineColor.YELLOW.getIngredient()))
            .unlockedBy(FabricRecipeProvider.getHasName(base), FabricRecipeProvider.has(base));
      } else {
        final String[] patterns = switch (offsetLevel) {
          case 2 -> new String[]{
              "*  ",
              "XXX",
              "*  "
          };
          case 1 -> new String[]{
              "*  ",
              "XXX",
              " * "
          };
          default -> throw new IllegalStateException("Unexpected value: " + offsetLevel);
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

    @Override
    public int offsetLevel() {
      return offsetLevel;
    }
  }
}
