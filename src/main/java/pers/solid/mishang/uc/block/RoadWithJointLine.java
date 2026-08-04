package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.blocks.RoadBlocks;
import pers.solid.mishang.uc.data.FasterTextureMap;
import pers.solid.mishang.uc.data.MishangucTextureKeys;
import pers.solid.mishang.uc.util.*;

import java.util.List;

public interface RoadWithJointLine extends Road {
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
            state.getValue(FACING) != direction.getOpposite(),
            getLineColor(state, direction),
            EightHorizontalDirection.of(direction),
            getLineType(state, direction), null));
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
    final Direction rotation = ctx.getHorizontalDirection();
    return Road.super
        .withPlacementState(state, ctx)
        .setValue(
            FACING,
            ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()
                ? rotation.getOpposite()
                : rotation);
  }

  @Override
  default void appendRoadTooltip(
      ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag options) {
    Road.super.appendRoadTooltip(stack, world, tooltip, options);
    tooltip.add(
        TextBridge.translatable("block.mishanguc.tooltip.road_with_joint_line.1")
            .withStyle(ChatFormatting.GRAY));
    tooltip.add(
        TextBridge.translatable("block.mishanguc.tooltip.road_with_joint_line.2")
            .withStyle(ChatFormatting.GRAY));
  }

  class Impl extends AbstractRoadBlock implements RoadWithJointLine {
    public final LineColor lineColorSide;
    public final LineType lineTypeSide;
    private final String lineTop;
    protected final String lineSide;
    protected final String lineSide2;

    public Impl(
        Properties settings,
        LineColor lineColor,
        LineColor lineColorSide,
        LineType lineType,
        LineType lineTypeSide, String lineTop) {
      super(settings, lineColor, lineType);
      this.lineColorSide = lineColorSide;
      this.lineTypeSide = lineTypeSide;
      this.lineTop = lineTop;
      lineSide = MishangUtils.composeStraightLineTexture(this.lineColor, this.lineType);
      lineSide2 = MishangUtils.composeStraightLineTexture(this.lineColorSide, this.lineTypeSide);
    }

    @Override
    public LineColor getLineColor(BlockState blockState, Direction direction) {
      final Direction facing = blockState.getValue(FACING);
      if (facing == direction) {
        return lineColorSide;
      } else if (facing == direction.getOpposite()) {
        return LineColor.NONE;
      } else {
        return lineColor;
      }
    }

    @Override
    public void appendDescriptionTooltip(List<Component> tooltip, TooltipFlag options) {
      tooltip.add(TextBridge.translatable("lineType.joint.composed", lineColor.getName(), lineType.getName(), lineColorSide.getName(), lineTypeSide.getName()).withStyle(ChatFormatting.BLUE));
    }

    @Override
    public LineType getLineType(BlockState blockState, Direction direction) {
      final Direction facing = blockState.getValue(FACING);
      if (facing == direction) {
        return lineTypeSide;
      } else if (facing == direction.getOpposite()) {
        return LineType.NORMAL;
      } else {
        return lineType;
      }
    }

    @Override
    protected <B extends Block & Road> void registerBaseOrSlabModels(B road, BlockStateModelGenerator blockStateModelGenerator) {
      final FasterTextureMap textures = new FasterTextureMap().base("asphalt").lineSide(lineSide).lineSide2(lineSide2).lineTop(lineTop);
      final ResourceLocation modelId = road.uploadModel("_with_joint_line", textures, blockStateModelGenerator, MishangucTextureKeys.BASE, MishangucTextureKeys.LINE_SIDE, MishangucTextureKeys.LINE_TOP, MishangucTextureKeys.LINE_SIDE2);
      blockStateModelGenerator.blockStateCollector.accept(road.composeState(VariantsBlockStateSupplier.create(road, BlockStateVariant.create().put(VariantSettings.MODEL, modelId)).coordinate(BlockStateVariantMap.create(FACING).register(direction -> BlockStateVariant.create().put(MishangUtils.DIRECTION_Y_VARIANT, direction)))));
    }

    @Override
    public RecipeBuilder getPaintingRecipe(Block base, Block self) {
      final String pattern1 = switch (lineTypeSide) {
        case NORMAL -> " a ";
        case DOUBLE -> "a a";
        case THICK -> "aaa";
      };
      Block base2 = RoadBlocks.getRoadBlockWithLine(lineColor, lineType);
      if (base instanceof SlabBlock) {
        base2 = ((AbstractRoadBlock) base2).getRoadSlab();
      }
      final ShapedRecipeBuilder recipe = ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, self, 3)
          .pattern(pattern1)
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
