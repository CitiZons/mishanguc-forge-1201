package pers.solid.mishang.uc.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import pers.solid.mishang.uc.util.RoadConnectionState;

import java.util.EnumMap;

public class RoadSlabBlockWithAutoLine extends SmartRoadSlabBlock<RoadBlockWithAutoLine>
    implements RoadWithAutoLine {

  public RoadSlabBlockWithAutoLine(RoadBlockWithAutoLine baseBlock) {
    super(baseBlock);
  }

  @Override
  public @NotNull BlockState makeState(
      EnumMap<Direction, RoadConnectionState> connectionStateMap, BlockState defaultState) {
    final BlockState baseState = baseBlock.makeState(connectionStateMap, defaultState);
    AbstractRoadBlock block = (AbstractRoadBlock) baseState.getBlock();
    BlockState state = block.getRoadSlab().defaultBlockState();
    for (Property<?> property : baseState.getProperties()) {
      if (state.hasProperty(property)) {
        state = sendProperty(baseState, state, property);
      }
    }
    return state
        .setValue(WATERLOGGED, defaultState.getValue(WATERLOGGED))
        .setValue(TYPE, defaultState.getValue(TYPE));
  }

  @Override
  public void neighborChanged(
      BlockState state, Level world, BlockPos pos, Block block, BlockPos sourcePos, boolean notify) {
    super.neighborChanged(state, world, pos, block, sourcePos, notify);
    neighborRoadUpdate(state, world, pos, block, sourcePos, notify);
  }

  private <T extends Comparable<T>> BlockState sendProperty(
      BlockState fromState, BlockState toState, Property<T> property) {
    return toState.setValue(property, fromState.getValue(property));
  }
}
