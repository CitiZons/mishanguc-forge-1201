package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.item.TooltipFlag;
import pers.solid.mishang.uc.data.stubs.*;
import pers.solid.mishang.uc.data.stubs.TextureMap;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.MishangucProperties;
import pers.solid.mishang.uc.blocks.RoadBlocks;
import pers.solid.mishang.uc.data.FasterTextureMap;
import pers.solid.mishang.uc.data.MishangucTextureKeys;
import pers.solid.mishang.uc.util.*;

import java.util.List;
import java.util.function.Supplier;
import com.mojang.math.Axis;

public interface RoadWithStraightAndAngleLine extends RoadWithAngleLine, RoadWithStraightLine {
  BooleanProperty BEVEL_TOP = MishangucProperties.BEVEL_TOP;

  @Override
  default void appendRoadProperties(StateDefinition.Builder<Block, BlockState> builder) {
    RoadWithAngleLine.super.appendRoadProperties(builder);
    RoadWithStraightLine.super.appendRoadProperties(builder);
  }

  @Override
  default RoadConnectionState getConnectionStateOf(BlockState state, Direction direction) {
    return RoadConnectionState.or(
        RoadWithStraightLine.super.getConnectionStateOf(state, direction),
        RoadWithAngleLine.super.getConnectionStateOf(state, direction));
  }

  @Override
  default BlockState mirrorRoad(BlockState state, Mirror mirror) {
    return RoadWithAngleLine.super.mirrorRoad(state, mirror);
  }

  @Override
  default BlockState rotateRoad(BlockState state, Rotation rotation) {
    return RoadWithStraightLine.super.rotateRoad(
        RoadWithAngleLine.super.rotateRoad(state, rotation), rotation);
  }

  @Override
  default BlockState withPlacementState(BlockState state, BlockPlaceContext ctx) {
    return RoadWithStraightLine.super.withPlacementState(
        RoadWithAngleLine.super.withPlacementState(state, ctx), ctx);
  }

  @Override
  default void appendRoadTooltip(
      ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag options) {
    RoadWithAngleLine.super.appendRoadTooltip(stack, world, tooltip, options);
    RoadWithStraightLine.super.appendRoadTooltip(stack, world, tooltip, options);
  }

  class Impl extends AbstractRoadBlock implements RoadWithStraightAndAngleLine {
    /**
     * 用于构造函数，道路是否拥有 {@link #BEVEL_TOP} 属性。在构造函数调用之前就应该被计算。
     */
    private static boolean hasBevelTopProperty;
    private final LineColor lineColorSide;
    private final LineType lineTypeSide;

    public Impl(Properties settings, LineColor lineColor, LineColor lineColorSide, LineType lineType, LineType lineTypeSide) {
      super(settings, lineColor, ((Supplier<LineType>) () -> {
        hasBevelTopProperty = lineColor != lineColorSide;
        return lineType;
      }).get());
      this.lineColorSide = lineColorSide;
      this.lineTypeSide = lineTypeSide;
      if (hasBevelTopProperty) {
        registerDefaultState(defaultBlockState().setValue(BEVEL_TOP, false));
      }
    }

    public Impl(Properties settings, LineColor lineColor, LineType lineType) {
      this(settings, lineColor, lineColor, lineType, lineType);
    }

    @Override
    public boolean isBevel() {
      return true;
    }

    @Override
    public void appendRoadProperties(StateDefinition.Builder<Block, BlockState> builder) {
      RoadWithStraightAndAngleLine.super.appendRoadProperties(builder);
      if (hasBevelTopProperty) {
        builder.add(BEVEL_TOP);
      }
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
      final BlockState placementState = super.getStateForPlacement(ctx);
      if (placementState == null) return null;
      final Direction direction = placementState.getValue(FACING).getDirectionInAxis(placementState.getValue(AXIS));
      final BlockPos blockPos = ctx.getClickedPos();
      final BlockPos neighborPos = blockPos.relative(direction);
      final Level world = ctx.getLevel();
      return updateShape(placementState, direction, world.getBlockState(neighborPos), world, blockPos, neighborPos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
      BlockState stateForNeighborUpdate = super.updateShape(state, direction, neighborState, world, pos, neighborPos);
      if (stateForNeighborUpdate.hasProperty(BEVEL_TOP) && stateForNeighborUpdate.getValue(AXIS).test(direction) && stateForNeighborUpdate.getValue(FACING).hasDirection(direction)) {
        // 如果连接的那个方块在连接部分的道路标线与当前道路的斜线部分颜色一致，那么 bevel_top = true。
        final Block neighborBlock = neighborState.getBlock();
        final boolean bevelTop = neighborBlock instanceof Road road && road.getLineColor(neighborState, direction.getOpposite()) == lineColorSide;
        if (bevelTop) {
          return stateForNeighborUpdate.setValue(BEVEL_TOP, true);
        } else {
          final BlockPos up = neighborPos.above();
          final BlockState upState = world.getBlockState(up);
          if (upState.getBlock() instanceof Road road && road.getLineColor(upState, direction.getOpposite()) == lineColorSide) {
            return stateForNeighborUpdate.setValue(BEVEL_TOP, true);
          }
          final BlockPos down = neighborPos.below();
          final BlockState downState = world.getBlockState(down);
          if (downState.getBlock() instanceof Road road && road.getLineColor(downState, direction.getOpposite()) == lineColorSide) {
            return stateForNeighborUpdate.setValue(BEVEL_TOP, true);
          }
        }
        return stateForNeighborUpdate.setValue(BEVEL_TOP, false);
      }
      return stateForNeighborUpdate;
    }

    @Override
    protected <B extends Block & Road> void registerBaseOrSlabModels(B road, BlockStateModelGenerator blockStateModelGenerator) {
      final String lineTopStraight = MishangUtils.composeStraightLineTexture(lineColor, lineType);
      final String lineTopAngle = MishangUtils.composeAngleLineTexture(lineColorSide, LineType.NORMAL, true);
      final String lineSide = lineTopStraight;
      final String lineSide2 = MishangUtils.composeStraightLineTexture(lineColorSide, lineTypeSide);
      final FasterTextureMap textures = new FasterTextureMap()
          .base("asphalt")
          .lineTop(lineTopAngle)
          .lineTop2(lineTopStraight)
          .lineSide(lineSide)
          .lineSide2(lineSide2);
      final ResourceLocation modelId = road.uploadModel("_with_straight_and_angle_line", textures, blockStateModelGenerator, MishangucTextureKeys.BASE, MishangucTextureKeys.LINE_TOP, MishangucTextureKeys.LINE_TOP2, MishangucTextureKeys.LINE_SIDE, MishangucTextureKeys.LINE_SIDE2);
      final ResourceLocation mirroredModelId = road.uploadModel("_with_straight_and_angle_line_mirrored", "_mirrored", textures, blockStateModelGenerator, MishangucTextureKeys.BASE, MishangucTextureKeys.LINE_TOP, MishangucTextureKeys.LINE_TOP2, MishangucTextureKeys.LINE_SIDE, MishangucTextureKeys.LINE_SIDE2);

      final ResourceLocation beveledTopModelId, beveledTopMirroredModelId;
      if (getStateDefinition().getProperties().contains(BEVEL_TOP)) {
        TextureMap textures2 = new FasterTextureMap()
            .base("asphalt")
            .lineTop(lineTopStraight)
            .lineTop2(lineTopAngle)
            .lineSide(lineSide)
            .lineSide2(lineSide2)
            .varP(MishangucTextureKeys.LINE_SIDE3, lineSide2);

        beveledTopModelId = road.uploadModel("_with_straight_and_angle_line", "_bevel_top", textures2, blockStateModelGenerator, MishangucTextureKeys.BASE, MishangucTextureKeys.LINE_TOP, MishangucTextureKeys.LINE_TOP2, MishangucTextureKeys.LINE_SIDE, MishangucTextureKeys.LINE_SIDE2, MishangucTextureKeys.LINE_SIDE3);
        beveledTopMirroredModelId = road.uploadModel("_with_straight_and_angle_line_mirrored", "_bevel_top_mirrored", textures2, blockStateModelGenerator, MishangucTextureKeys.BASE, MishangucTextureKeys.LINE_TOP, MishangucTextureKeys.LINE_TOP2, MishangucTextureKeys.LINE_SIDE, MishangucTextureKeys.LINE_SIDE2, MishangucTextureKeys.LINE_SIDE3);
      } else {
        beveledTopModelId = beveledTopMirroredModelId = null;
      }

      final boolean hasBevelTop = lineColor != lineColorSide;
      final BlockStateVariantMap.DoubleProperty<Direction.Axis, HorizontalCornerDirection> map1 = hasBevelTop ? null : BlockStateVariantMap.create(AXIS, FACING);
      final BlockStateVariantMap.TripleProperty<Direction.Axis, HorizontalCornerDirection, Boolean> map2 = hasBevelTop ? BlockStateVariantMap.create(AXIS, FACING, BEVEL_TOP) : null;
      for (Direction direction : Direction.Plane.HORIZONTAL) {
        final int rotation = (int) direction.toYRot();
        final Direction.Axis axis = direction.getAxis();
        final @NotNull HorizontalCornerDirection facing1 = HorizontalCornerDirection.fromDirections(direction, direction.getClockWise());
        final @NotNull HorizontalCornerDirection facing2 = HorizontalCornerDirection.fromDirections(direction, direction.getCounterClockWise());
        if (hasBevelTop) {
          map2.register(axis, facing1, false,
              BlockStateVariant.create().put(VariantSettings.MODEL, modelId).put(MishangUtils.INT_Y_VARIANT, rotation));
          map2.register(axis, facing1, true,
              BlockStateVariant.create().put(VariantSettings.MODEL, beveledTopModelId).put(MishangUtils.INT_Y_VARIANT, rotation));
          map2.register(axis, facing2, false,
              BlockStateVariant.create().put(VariantSettings.MODEL, mirroredModelId).put(MishangUtils.INT_Y_VARIANT, rotation - 90));
          map2.register(axis, facing2, true,
              BlockStateVariant.create().put(VariantSettings.MODEL, beveledTopMirroredModelId).put(MishangUtils.INT_Y_VARIANT, rotation - 90));
        } else {
          map1.register(
              axis, facing1,
              BlockStateVariant.create().put(VariantSettings.MODEL, modelId).put(MishangUtils.INT_Y_VARIANT, rotation));
          map1.register(
              axis, facing2,
              BlockStateVariant.create().put(VariantSettings.MODEL, mirroredModelId).put(MishangUtils.INT_Y_VARIANT, rotation - 90));
        }
      }
      blockStateModelGenerator.blockStateCollector.accept(road.composeState(VariantsBlockStateSupplier.create(road).coordinate(hasBevelTop ? map2 : map1)));
    }

    @Override
    public LineColor getLineColor(BlockState state, Direction direction) {
      if (state.getValue(FACING).hasDirection(direction) && (state.hasProperty(BEVEL_TOP) && state.getValue(BEVEL_TOP) || !state.getValue(AXIS).test(direction))) {
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
    public void appendDescriptionTooltip(List<Component> tooltip, TooltipFlag options) {
      if (lineColor == lineColorSide && lineType == lineTypeSide) {
        tooltip.add(TextBridge.translatable("lineType.straightAndAngle.same", lineColor.getName(), lineType.getName()).withStyle(ChatFormatting.BLUE));
      } else {
        tooltip.add(TextBridge.translatable("lineType.straightAndAngle.straight", lineColor.getName(), lineType.getName()).withStyle(ChatFormatting.BLUE));
        tooltip.add(TextBridge.translatable("lineType.straightAndAngle.bevel", lineColorSide.getName(), lineTypeSide.getName()).withStyle(ChatFormatting.BLUE));
      }
    }

    @Override
    public RecipeBuilder getPaintingRecipe(Block base, Block self) {
      if (lineTypeSide != LineType.NORMAL) {
        throw new UnsupportedOperationException();
      }
      Block base2 = RoadBlocks.getRoadBlockWithLine(lineColor, lineType);
      if (base instanceof SlabBlock) {
        base2 = ((AbstractRoadBlock) base2).getRoadSlab();
      }
      return ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, self, 3)
          .pattern(" *X")
          .pattern("*X ")
          .pattern("X  ")
          .define('*', lineColorSide.getIngredient())
          .define('X', base2)
          .unlockedBy("has_paint", FabricRecipeProvider.has(lineColorSide.getIngredient()))
          .unlockedBy(FabricRecipeProvider.getHasName(base2), FabricRecipeProvider.has(base2));
    }
  }
}
