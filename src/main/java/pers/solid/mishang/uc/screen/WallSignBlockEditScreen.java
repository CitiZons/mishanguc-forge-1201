package pers.solid.mishang.uc.screen;

import com.google.common.collect.ImmutableList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.mishang.uc.blockentity.WallSignBlockEntity;
import pers.solid.mishang.uc.text.TextContext;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class WallSignBlockEditScreen extends AbstractSignBlockEditScreen<WallSignBlockEntity> {
  /**
   * 修改之前的文本内容。当编辑时取消编辑了，则使用修改之前的文本内容。
   */
  private final @Unmodifiable List<TextContext> backedUpTextContexts;

  public WallSignBlockEditScreen(WallSignBlockEntity entity, BlockPos blockPos) {
    super(
        entity,
        blockPos,
        entity.textContexts);
    this.backedUpTextContexts = entity.textContexts;
    entity.textContexts = textFieldListWidget.getTextContexts();
  }

  @Override
  public void removed() {
    super.removed();
    if (changed) {
      // 固化 entity.textContexts
      entity.textContexts = ImmutableList.copyOf(textFieldListWidget.getTextContexts());
    } else {
      entity.textContexts = backedUpTextContexts;
    }
  }
}
