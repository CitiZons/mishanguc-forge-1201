package pers.solid.mishang.uc.blockentity;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import pers.solid.mishang.uc.MishangUtils;

public class ColoredStandingSignBlockEntity extends StandingSignBlockEntity implements ColoredBlockEntity {
  public int color = 0;

  public ColoredStandingSignBlockEntity(BlockPos pos, BlockState state) {
    super(MishangucBlockEntities.COLORED_STANDING_SIGN_BLOCK_ENTITY, pos, state);
  }

  @Override
  public void load(CompoundTag nbt) {
    super.load(nbt);
    color = MishangUtils.readColorFromNbtElement(nbt.get("color"));
    if (level != null && level.isClientSide) {
      level.sendBlockUpdated(getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }
  }

  @Override
  public void saveAdditional(CompoundTag nbt) {
    super.saveAdditional(nbt);
    nbt.putInt("color", color);
  }

  @Override
  public int getColor() {
    return color;
  }

  @Override
  public void setColor(int color) {
    this.color = color;
  }
}
