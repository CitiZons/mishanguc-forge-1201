package pers.solid.mishang.uc.blockentity;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.mishang.uc.render.WallSignBlockEntityRenderer;
import pers.solid.mishang.uc.text.TextContext;

import java.util.List;

/**
 * @see pers.solid.mishang.uc.block.WallSignBlock
 * @see WallSignBlockEntityRenderer
 */
public class WallSignBlockEntity extends BlockEntityWithText {
  public static final TextContext DEFAULT_TEXT_CONTEXT = Util.make(new TextContext(), textContext -> textContext.size = 6);
  /**
   * 正在编辑该告示牌的玩家。若为 <code>null</code>，则表示该告示牌为空闲模式。
   */
  public @Nullable Player editor;

  public @Unmodifiable List<TextContext> textContexts = ImmutableList.of();
  /**
   * 告示牌的文本是否正在发光，不影响文本的颜色和描边，只影响文本显示时的所使用的亮度。
   */
  public boolean glowing;
  /**
   * 告示牌是否已经被涂蜡。
   */
  public boolean waxed;

  public WallSignBlockEntity(BlockPos pos, BlockState state) {
    super(MishangucBlockEntities.WALL_SIGN_BLOCK_ENTITY, pos, state);
  }

  protected WallSignBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  @Override
  public void load(CompoundTag nbt) {
    super.load(nbt);
    final @Nullable Tag nbtText = nbt.get("text");
    if (nbtText instanceof StringTag || nbt.contains("textJson", Tag.TAG_STRING)) {
      // 如果 text 是个字符串，则读取整个 nbt 作为 TextContext。
      // 例如，整个 nbt 可以是 {text: "abc", color: "red", size: 5}。
      textContexts = ImmutableList.of(TextContext.fromNbt(nbt, createDefaultTextContext()));
    } else if (nbtText instanceof CompoundTag) {
      // 如果 text 是个复合标签，则读取这个复合标签。
      // 例如，整个 nbt 可以是 {text: {text: "abc", color: "red", size: 5}}。
      textContexts = ImmutableList.of(TextContext.fromNbt(nbtText, createDefaultTextContext()));
    } else if (nbtText instanceof ListTag) {
      ImmutableList.Builder<TextContext> builder = new ImmutableList.Builder<>();
      for (Tag nbtElement : ((ListTag) nbtText)) {
        builder.add(TextContext.fromNbt(nbtElement, createDefaultTextContext()));
      }
      textContexts = builder.build();
    }

    glowing = nbt.getBoolean("glowing");
    waxed = nbt.getBoolean("waxed");
  }

  @Override
  public void saveAdditional(CompoundTag nbt) {
    super.saveAdditional(nbt);
    if (textContexts.size() == 1) {
      final CompoundTag nbtCompound = new CompoundTag();
      textContexts.get(0).saveAdditional(nbtCompound);
      nbt.put("text", nbtCompound);
    } else {
      final ListTag nbtList = new ListTag();
      for (TextContext textContext : textContexts) {
        nbtList.add(textContext.createNbt());
      }
      nbt.put("text", nbtList);
    }
    nbt.putBoolean("glowing", glowing);
    nbt.putBoolean("waxed", waxed);
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
