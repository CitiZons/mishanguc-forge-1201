package pers.solid.mishang.uc.blockentity;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.BlockPos;
import pers.solid.mishang.uc.MishangUtils;

public class SimpleColoredBlockEntity extends BlockEntity implements ColoredBlockEntity {
  public int color = 0;

  public SimpleColoredBlockEntity(BlockPos pos, BlockState state) {
    super(MishangucBlockEntities.SIMPLE_COLORED_BLOCK_ENTITY, pos, state);
  }

  @Override
  public int getColor() {
    return color;
  }

  @Override
  public void setColor(int color) {
    this.color = color;
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
  protected void saveAdditional(CompoundTag nbt) {
    super.saveAdditional(nbt);
    nbt.putInt("color", color);
  }

  @Override
  public Packet<ClientGamePacketListener> getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }

  @Override
  public CompoundTag getUpdateTag() {
    return saveWithoutMetadata();
  }
}
