package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SlabBlock;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.MishangucProperties;
import pers.solid.mishang.uc.blocks.RoadBlocks;
import pers.solid.mishang.uc.data.FasterTextureMap;
import pers.solid.mishang.uc.data.MishangucTextureKeys;
import pers.solid.mishang.uc.util.*;

import java.util.List;
import com.mojang.math.Axis;

/**
 * 类似于 {@link RoadWithJointLine}，不过较短的那一条线是被偏移的。
 */
public interface RoadWithJointLineWithOffsetSide extends Road {
  /**
   * 道路方块中，偏移半线与正中直线围成的面积范围较小的那个直角。<br>
   * 不同于{@link RoadWithJointLine#FACING}，那个是正对的水平方向，而这个是斜角水平方向。
   */
  EnumProperty<HorizontalCornerDirection> FACING = MishangucProperties.HORIZONTAL_CORNER_FACING;
  /**
   * 道路方块中，正中直线所在的轴。
   */
  Property<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

  @Override
  LineColor getLineColor(BlockState blockState, Direction direction);

  @Override
  default void appendRoadProperties(StateDefinition.Builder<Block, BlockState> builder) {
    Road.super.appendRoadProperties(builder);
    builder.add(FACING, AXIS);
  }

  @Override
  default RoadConnectionState getConnectionStateOf(BlockState state, Direction direction) {
    return RoadConnectionState.of(
        state.getValue(FACING).hasDirection(direction) || state.getValue(AXIS).test(direction),
        getLineColor(state, direction),
        EightHorizontalDirection.of(direction.getOpposite()),
        getLineType(state, direction),
        state.getValue(AXIS).test(direction) ? null : new LineOffset(state.getValue(FACING).getDirectionInAxis(state.getValue(AXIS)), offsetLevel()));
  }

  @Override
  default BlockState mirrorRoad(BlockState state, Mirror mirror) {
    return state.setValue(FACING, state.getValue(FACING).mirror(mirror));
  }

  @Override
  default BlockState rotateRoad(BlockState state, Rotation rotation) {
    final Direction.Axis axis = state.getValue(AXIS);
    return state
        .setValue(FACING, state.getValue(FACING).rotate(rotation))
        .setValue(AXIS, MishangUtils.rotateAxis(rotation, axis));
  }

  @Override
  default BlockState withPlacementState(BlockState state, BlockPlaceContext ctx) {
    final HorizontalCornerDirection facing = HorizontalCornerDirection.fromRotation(ctx.getRotation());
    return state
        .setValue(
            FACING,
            ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown() ? facing.getOpposite() : facing)
        .setValue(AXIS, ctx.getHorizontalDirection().getAxis());
  }

  @Override
  default void appendRoadTooltip(
      ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag options) {
    Road.super.appendRoadTooltip(stack, world, tooltip, options);
    tooltip.add(TextBridge.translatable("block.mishanguc.tooltip.road_with_joint_line_with_offset_side.1").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("block.mishanguc.tooltip.road_with_joint_line_with_offset_side.2").withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("block.mishanguc.tooltip.road_with_joint_line_with_offset_side.3").withStyle(ChatFormatting.GRAY));
  }

  int offsetLevel();

  class Impl extends AbstractRoadBlock implements RoadWithJointLineWithOffsetSide {
    private final LineColor lineColorSide;
    private final LineType lineTypeSide;
    protected final String lineSide;
    protected final String lineSide2;
    protected final String lineTop;
    private final int offsetLevel;

    /**
     * 由不带偏移的 T 字形道路映射到带有偏移的 T 字形道路的映射。这里的偏移，是指的只有半边的那条线路的偏移。
     */
    public static final BiMap<RoadWithJointLine.Impl, RoadWithJointLineWithOffsetSide.Impl> OFFSET_ROADS = HashBiMap.create();

    public Impl(Properties settings, RoadWithJointLine.Impl block, String lineTop, int offsetLevel) {
      this(settings, block.lineColor, block.lineColorSide, block.lineType, block.lineTypeSide, lineTop, offsetLevel);
      OFFSET_ROADS.put(block, this);
    }

    public Impl(Properties settings, LineColor lineColor, LineColor lineColorSide, LineType lineType, LineType lineTypeSide, String lineTop, int offsetLevel) {
      super(settings, lineColor, lineType);
      this.lineColorSide = lineColorSide;
      this.lineTypeSide = lineTypeSide;
      this.lineTop = lineTop;
      this.offsetLevel = offsetLevel;
      lineSide = MishangUtils.composeStraightLineTexture(lineColor, lineType);
      lineSide2 = lineColorSide.getSerializedName() + "_offset_straight_line";
    }

    @Override
    public int offsetLevel() {
      return offsetLevel;
    }

    @Override
    protected <B extends Block & Road> void registerBaseOrSlabModels(B road, BlockStateModelGenerator blockStateModelGenerator) {
      final FasterTextureMap textures = new FasterTextureMap().base("asphalt").lineSide(lineSide).lineSide2(lineSide2).lineTop(lineTop);
      final ResourceLocation modelId = road.uploadModel("_with_joint_line", textures, blockStateModelGenerator, MishangucTextureKeys.BASE, MishangucTextureKeys.LINE_SIDE, MishangucTextureKeys.LINE_SIDE2, MishangucTextureKeys.LINE_TOP);
      final ResourceLocation mirroredModelId = road.uploadModel("_with_joint_line_mirrored", "_mirrored", textures, blockStateModelGenerator, MishangucTextureKeys.BASE, MishangucTextureKeys.LINE_SIDE, MishangucTextureKeys.LINE_SIDE2, MishangucTextureKeys.LINE_TOP);
      final BlockStateVariantMap.DoubleProperty<HorizontalCornerDirection, Direction.Axis> map = BlockStateVariantMap.create(FACING, AXIS);
      // 一侧的短线所朝向的方向。
      for (Direction direction : Direction.Plane.HORIZONTAL) {
        final @NotNull Direction offsetDirection1 = direction.getClockWise();
        // direction 的右偏方向
        final @NotNull HorizontalCornerDirection facing1 = HorizontalCornerDirection.fromDirections(direction, offsetDirection1);
        final @NotNull Direction offsetDirection2 = direction.getCounterClockWise();
        // direction 的左偏方向
        final @NotNull HorizontalCornerDirection facing2 = HorizontalCornerDirection.fromDirections(direction, offsetDirection2);
        map
            .register(facing1, offsetDirection1.getAxis(),
                BlockStateVariant.create().put(VariantSettings.MODEL, modelId).put(MishangUtils.DIRECTION_Y_VARIANT, direction))
            .register(facing2, offsetDirection2.getAxis(),
                BlockStateVariant.create().put(VariantSettings.MODEL, mirroredModelId)
                    .put(MishangUtils.DIRECTION_Y_VARIANT, direction));
      }
      blockStateModelGenerator.blockStateCollector.accept(road.composeState(VariantsBlockStateSupplier.create(road).coordinate(map)));
    }

    @Override
    public void appendDescriptionTooltip(List<Component> tooltip, TooltipFlag options) {
      tooltip.add(TextBridge.translatable("lineType.jointWithOffsetSide.composed.1", lineColor.getName(), lineType.getName()).withStyle(ChatFormatting.BLUE));
      tooltip.add(TextBridge.translatable("lineType.jointWithOffsetSide.composed.2", lineColorSide.getName(), lineTypeSide.getName()).withStyle(ChatFormatting.BLUE));
    }

    @Override
    public LineColor getLineColor(BlockState state, Direction direction) {
      if (state.getValue(FACING).hasDirection(direction) && !state.getValue(AXIS).test(direction)) {
        return lineColorSide;
      }
      return super.getLineColor(state, direction);
    }

    @Override
    public LineType getLineType(BlockState state, Direction direction) {
      if (state.getValue(FACING).hasDirection(direction) && !state.getValue(AXIS).test(direction)) {
        return lineTypeSide;
      }
      return super.getLineType(state, direction);
    }

    @Override
    public RecipeBuilder getPaintingRecipe(Block base, Block self) {
      if (lineTypeSide != LineType.NORMAL) {
        throw new UnsupportedOperationException(String.format("Recipe for the block [lineTypeSide=%s] is not supported", lineTypeSide.getSerializedName()));
      }
      Block base2 = RoadBlocks.getRoadBlockWithLine(lineColor, lineType);
      if (base instanceof SlabBlock) {
        base2 = ((AbstractRoadBlock) base2).getRoadSlab();
      }
      final ShapedRecipeBuilder recipe = ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, self, 3)
          .pattern("a  ")
          .pattern("XXX")
          .define('a', lineColorSide.getIngredient())
          .define('X', base2)
          .unlockedBy("has_" + lineColorSide.getSerializedName() + "_paint", FabricRecipeProvider.has(lineColorSide.getIngredient()))
          .unlockedBy(FabricRecipeProvider.getHasName(base2), FabricRecipeProvider.has(base2));
      if (lineColorSide != lineColor) {
        recipe.unlockedBy("has_" + lineColor.getSerializedName() + "_paint", FabricRecipeProvider.has(lineColor.getIngredient()));
      }
      return recipe;
    }
  }
}
