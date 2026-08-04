package pers.solid.mishang.uc.blockentity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.mishang.uc.render.HungSignBlockEntityRenderer;
import pers.solid.mishang.uc.text.TextContext;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @see pers.solid.mishang.uc.block.HungSignBlock
 * @see HungSignBlockEntityRenderer
 */
public class HungSignBlockEntity extends BlockEntityWithText {
  public static final TextContext DEFAULT_TEXT_CONTEXT =
      Util.make(new TextContext(), textContext1 -> textContext1.size = 6);
  /**
   * 该方块正在被编辑的方向。同时存在于客户端与服务器。<br>
   * 若未被编辑则为 {@code null}。<br>
   * The direction being edited of the block. Exists on both minecraft and server sides.<br>
   * {@code null} if not edited.
   */
  public @Nullable Direction editedSide;

  public @Unmodifiable Map<@NotNull Direction, @Unmodifiable @NotNull List<@NotNull TextContext>>
      texts = ImmutableMap.of();
  /**
   * 编辑该方块的玩家。若为非 <code>null</code>，则其他玩家不可编辑。<br>
   * The player editing the block. non-<code>null</code> means other players cannot edit.
   */
  @Nullable
  private Player editor;

  /**
   * 涂蜡的侧面。为了节省内容，如果为空集，则直接使用不可变的 {@link Set#of()}，其他情况则为 {@link HashSet}。
   */
  public @Unmodifiable Set<@NotNull Direction> waxed = Collections.emptySet();
  /**
   * 发光的侧面。为了节省内容，如果为空集，则直接使用不可变的 {@link Set#of()}，其他情况则为 {@link HashSet}。
   */
  public @Unmodifiable Set<@NotNull Direction> glowing = Collections.emptySet();

  public HungSignBlockEntity(BlockPos pos, BlockState state) {
    super(MishangucBlockEntities.HUNG_SIGN_BLOCK_ENTITY, pos, state);
  }

  protected HungSignBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  @Override
  public void load(CompoundTag nbt) {
    super.load(nbt);
    ImmutableMap.Builder<Direction, List<TextContext>> builder = new ImmutableMap.Builder<>();
    for (Direction direction : Direction.Plane.HORIZONTAL) {
      final Tag element = nbt.get(direction.getSerializedName());
      if (element instanceof ListTag) {
        ImmutableList.Builder<TextContext> listBuilder = new ImmutableList.Builder<>();
        for (Tag nbtElement : ((ListTag) element)) {
          TextContext textContext = TextContext.fromNbt(nbtElement);
          listBuilder.add(textContext);
        }
        final ImmutableList<TextContext> build = listBuilder.build();
        if (!build.isEmpty()) builder.put(direction, build);
      } else if (element != null) {
        builder.put(
            direction, ImmutableList.of(TextContext.fromNbt(element, createDefaultTextContext())));
      }
    }
    texts = builder.build();
    if (nbt.contains("waxed", Tag.TAG_LIST)) {
      final ListTag list = nbt.getList("waxed", Tag.TAG_STRING);
      if (list.isEmpty()) {
        waxed = Set.of();
      } else {
        waxed = list.stream().map(nbtElement -> Direction.byName(nbtElement.getAsString())).filter(Objects::nonNull).collect(Collectors.toCollection(() -> new HashSet<>(2)));
      }
    } else {
      waxed = Set.of();
    }
    if (nbt.contains("glowing", Tag.TAG_LIST)) {
      final ListTag list = nbt.getList("glowing", Tag.TAG_STRING);
      if (list.isEmpty()) {
        glowing = Set.of();
      } else {
        glowing = list.stream().map(nbtElement -> Direction.byName(nbtElement.getAsString())).filter(Objects::nonNull).collect(Collectors.toCollection(() -> new HashSet<>(2)));
      }
    } else {
      glowing = Set.of();
    }
  }

  @Override
  public void saveAdditional(CompoundTag nbt) {
    super.saveAdditional(nbt);
    for (Direction direction : Direction.Plane.HORIZONTAL) {
      final List<@NotNull TextContext> textContexts = texts.get(direction);
      if (textContexts == null || textContexts.isEmpty()) {
        continue;
      }
      final ListTag nbtList = new ListTag();
      for (TextContext textContext : textContexts) {
        nbtList.add(textContext.createNbt());
      }
      nbt.put(direction.getSerializedName(), nbtList);
    }
    nbt.put("waxed", waxed.stream().map(Direction::getSerializedName).map(StringTag::valueOf).collect(Collectors.toCollection(ListTag::new)));
    nbt.put("glowing", glowing.stream().map(Direction::getSerializedName).map(StringTag::valueOf).collect(Collectors.toCollection(ListTag::new)));
  }

  //  @Override
  public void applyRotation(Rotation rotation) {
    //    super.applyRotation(rotation);
    final ImmutableMap.Builder<Direction, List<TextContext>> builder = new ImmutableMap.Builder<>();
    texts.forEach((direction, list) -> builder.put(rotation.rotate(direction), list));
    texts = builder.build();
  }

  //  @Override
  public void applyMirror(Mirror mirror) {
    //    super.applyMirror(mirror);
    final ImmutableMap.Builder<Direction, List<TextContext>> builder = new ImmutableMap.Builder<>();
    texts.forEach((direction, list) -> builder.put(mirror.mirror(direction), list));
    texts = builder.build();
  }

  @Override
  public float getHeight() {
    return 8;
  }

  @Override
  public TextContext createDefaultTextContext() {
    return DEFAULT_TEXT_CONTEXT.clone();
  }

  @Override
  public @Nullable Player getEditor() {
    return editor;
  }

  @Override
  public void setEditor(@Nullable Player editor) {
    this.editor = editor;
  }

}
