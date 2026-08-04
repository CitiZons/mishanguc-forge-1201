package pers.solid.mishang.uc.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import pers.solid.mishang.uc.data.stubs.*;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
import pers.solid.mishang.uc.data.stubs.VariantSettings;
import pers.solid.mishang.uc.data.stubs.Model;
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import pers.solid.mishang.uc.MishangUtils;

import java.util.Map;
import com.mojang.math.Axis;

/**
 * 类似于墙上的灯方块，但是是条状的，因此具有多一个属性。
 */
public class StripWallLightBlock extends WallLightBlock implements LightConnectable {
  protected static final EnumProperty<StripType> STRIP_TYPE =
      EnumProperty.create("strip_type", StripType.class);
  private static final Map<Direction, VoxelShape> SHAPE_PER_DIRECTION_WHEN_HORIZONTAL =
      MishangUtils.createDirectionToShape(0, 0, 4, 16, 2, 12);
  private static final Map<Direction, VoxelShape> SHAPE_PER_DIRECTION_WHEN_VERTICAL =
      MishangUtils.createDirectionToShape(4, 0, 0, 12, 2, 16);

  public StripWallLightBlock(String lightColor, Properties settings) {
    super(lightColor, settings, false);
  }

  @Override
  public boolean isConnectedIn(BlockState blockState, Direction facing, Direction direction) {
    final StripType stripType = blockState.getValue(STRIP_TYPE);
    if (facing != blockState.getValue(FACING) || direction.getAxis() == facing.getAxis()) {
      return false;
    }
    return switch (stripType) {
      case VERTICAL -> facing.getAxis() == Direction.Axis.Y
          ? direction.getAxis() == Direction.Axis.Z
          : direction.getAxis() == Direction.Axis.Y;
      case HORIZONTAL -> facing.getAxis() == Direction.Axis.Y
          ? direction.getAxis() == Direction.Axis.X
          : direction.getAxis() != Direction.Axis.Y;
    };
  }

  @Override
  public BlockState rotate(BlockState state, Rotation rotation) {
    final BlockState rotate = super.rotate(state, rotation);
    if (rotate.getValue(FACING).getAxis().isVertical() && (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90)) {
      return rotate.setValue(STRIP_TYPE, rotate.getValue(STRIP_TYPE).another());
    }
    return rotate;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(STRIP_TYPE);
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext ctx) {
    final BlockState placementState = super.getStateForPlacement(ctx);
    if (placementState == null) {
      return null;
    }
    final Player player = ctx.getPlayer();
    return placementState.setValue(
        STRIP_TYPE,
        ctx.getClickedFace().getAxis() == Direction.Axis.Y
            ? (ctx.getHorizontalDirection().getAxis() == Direction.Axis.X
            ? StripType.HORIZONTAL
            : StripType.VERTICAL)
            : (player != null && player.isShiftKeyDown() ? StripType.VERTICAL : StripType.HORIZONTAL));
  }

  @Override
  public VoxelShape getShape(
      BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    return (state.getValue(STRIP_TYPE) == StripType.VERTICAL
        ? SHAPE_PER_DIRECTION_WHEN_VERTICAL
        : SHAPE_PER_DIRECTION_WHEN_HORIZONTAL)
        .get(state.getValue(FACING));
  }

  public enum StripType implements StringRepresentable {
    /**
     * 水平的，对于天花板上或地上的表示为东西方向。
     */
    HORIZONTAL,
    /**
     * 垂直的，对于天花板上或地上的表示为南北方向。
     */
    VERTICAL;

    @Override
    public String getSerializedName() {
      return switch (this) {
        case HORIZONTAL -> "horizontal";
        case VERTICAL -> "vertical";
      };
    }

    public StripType another() {
      return switch (this) {
        case HORIZONTAL -> VERTICAL;
        case VERTICAL -> HORIZONTAL;
      };
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
    return stateFrom.is(this) && ((LightConnectable) stateFrom.getBlock()).isConnectedIn(stateFrom, state.getValue(FACING), direction.getOpposite()) || super.skipRendering(state, stateFrom, direction);
  }

  @Override
  public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    final BlockStateVariantMap.DoubleProperty<Direction, StripType> map = BlockStateVariantMap.create(FACING, STRIP_TYPE);

    final TextureMap textureMap = getTextureMap();
    final ResourceLocation id = getModelType().upload(this, textureMap, blockStateModelGenerator.modelCollector);
    final ResourceLocation idVertical = getModelType("_vertical").upload(this, textureMap, blockStateModelGenerator.modelCollector);

    map.register(Direction.UP, StripType.HORIZONTAL, BlockStateVariant.create().put(VariantSettings.MODEL, id));
    map.register(Direction.UP, StripType.VERTICAL, BlockStateVariant.create().put(VariantSettings.MODEL, idVertical));
    map.register(Direction.DOWN, StripType.HORIZONTAL, BlockStateVariant.create().put(VariantSettings.MODEL, id).put(VariantSettings.X, VariantSettings.Rotation.R180));
    map.register(Direction.DOWN, StripType.VERTICAL, BlockStateVariant.create().put(VariantSettings.MODEL, idVertical).put(VariantSettings.X, VariantSettings.Rotation.R180));
    for (Direction direction : Direction.Plane.HORIZONTAL) {
      map.register(direction, StripType.HORIZONTAL, BlockStateVariant.create().put(VariantSettings.MODEL, id).put(VariantSettings.X, VariantSettings.Rotation.R270).put(MishangUtils.DIRECTION_Y_VARIANT, direction));
      map.register(direction, StripType.VERTICAL, BlockStateVariant.create().put(VariantSettings.MODEL, idVertical).put(VariantSettings.X, VariantSettings.Rotation.R270).put(MishangUtils.DIRECTION_Y_VARIANT, direction));
    }
    blockStateModelGenerator.blockStateCollector.accept(VariantsBlockStateSupplier.create(this, BlockStateVariant.create().put(VariantSettings.UVLOCK, true)).coordinate(map));
  }
}
