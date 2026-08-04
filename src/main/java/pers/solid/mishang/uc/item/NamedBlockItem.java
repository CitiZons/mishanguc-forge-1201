package pers.solid.mishang.uc.item;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.block.ColoredBlock;
import pers.solid.mishang.uc.block.ColoredGlassHandrailBlock;
import pers.solid.mishang.uc.blockentity.ColoredBlockEntity;
import pers.solid.mishang.uc.mixin.ItemUsageContextInvoker;
import pers.solid.mishang.uc.util.TextBridge;

/**
 * <p>类似于 {@link BlockItem}，但是名称会调用 {@link Block#getName()}。</p>
 * <p>必须注意：由于 {@link Block#getName()} 仅限客户端，因此本类的方块必须确保覆盖该方法时，没有注解为 {@code @}{@link Environment}{@code (Dist.CLIENT)}！！</p>
 */
public class NamedBlockItem extends BlockItem {

  public NamedBlockItem(Block block, Properties settings) {
    super(block, settings);
  }

  @Override
  public Component getDescription() {
    return getBlock().getName();
  }

  @Override
  public Component getName(ItemStack stack) {
    final Block block = getBlock();
    if (getBlock() instanceof ColoredBlock) {
      final CompoundTag nbt = stack.getTagElement("BlockEntityTag");
      if (nbt != null && nbt.contains("color", Tag.TAG_ANY_NUMERIC)) {
        final int color = nbt.getInt("color");
        return TextBridge.translatable("block.mishanguc.colored_block.color", block.getName(), MishangUtils.describeColor(color));
      } else if (getBlock() instanceof ColoredGlassHandrailBlock) {
        return TextBridge.translatable("block.mishanguc.colored_block.auto_color_decoration", block.getName());
      } else {
        return TextBridge.translatable("block.mishanguc.colored_block.auto_color", block.getName());
      }
    }
    return block.getName();
  }

  public static int getDependentColor(BlockPlaceContext context) {
    final Level world = context.getLevel();
    final int dependentColor;
    final BlockPos dependingPos = ((ItemUsageContextInvoker) context).invokeGetHitResult().getBlockPos();
    if (world.getBlockEntity(dependingPos) instanceof ColoredBlockEntity dependingColoredBlockEntity) {
      dependentColor = dependingColoredBlockEntity.getColor();
    } else {
      dependentColor = world.getBlockState(dependingPos).getMapColor(world, dependingPos).col;
    }
    return dependentColor;
  }

  @Override
  protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
    final ItemStack stack = context.getItemInHand();
    if (getBlock() instanceof ColoredBlock) {
      final Integer color;
      final CompoundTag nbt = stack.getTagElement("BlockEntityTag");
      if (nbt != null && nbt.contains("color", Tag.TAG_ANY_NUMERIC)) {
        color = nbt.getInt("color");
      } else {
        color = null;
      }
      final Level world = context.getLevel();
      int dependentColor = -1;
      if (color == null) {
        dependentColor = getDependentColor(context);
      }
      final boolean place = super.placeBlock(context, state);
      final BlockEntity placedEntity = world.getBlockEntity(context.getClickedPos());
      if (placedEntity instanceof final ColoredBlockEntity placedColoredBlockEntity) {
        if (color == null) {
          placedColoredBlockEntity.setColor(dependentColor);
        } else {
          placedColoredBlockEntity.setColor(color);
        }
      }
      return place;
    } else {
      return super.placeBlock(context, state);
    }
  }
}
