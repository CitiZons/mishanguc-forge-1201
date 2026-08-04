package pers.solid.mishang.uc.text;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.mixin.TextRendererAccessor;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.VertexConsumer;

public record PatternSpecialDrawable(TextContext textContext, RectanglePattern rectanglePattern) implements SpecialDrawable {

  @Contract(pure = true)
  public boolean isEmpty() {
    return rectanglePattern == RectanglePatterns.EMPTY;
  }

  @OnlyIn(Dist.CLIENT)
  @Override
  public void drawExtra(Font font, PoseStack matrixStack, MultiBufferSource vertexConsumers, int light, float x, float y) {
    int color = textContext.color;
    final float red = (float) (color >> 16 & 0xFF) / 255.0f;
    final float green = (float) (color >> 8 & 0xFF) / 255.0f;
    final float blue = (float) (color & 0xFF) / 255.0f;
    final float alpha = ((color & 0xFC000000) == 0) ? 1 : (float) (color >> 24 & 0xFF) / 255.0f;
    //noinspection resource
    BakedGlyph glyphRenderer = ((TextRendererAccessor) font).invokeGetFontSet(Style.DEFAULT_FONT).whiteGlyph();
    final float sizeMultiplier = 1;
    final RenderType layer = glyphRenderer.renderType(textContext.outlineColorType != OutlineColorType.NONE ? Font.DisplayMode.POLYGON_OFFSET : textContext.seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL);

    // 文本是否存在阴影。
    final boolean shadow = textContext.outlineColorType == OutlineColorType.NONE && textContext.shadow;
    // 用于文本渲染的矩阵。当存在阴影时，文本渲染需要适当调整。
    final List<BakedGlyph.Effect> rectanglesToDraw = new ArrayList<>();
    final List<BakedGlyph.Effect> outlineRectangles = textContext.outlineColorType == OutlineColorType.NONE ? null : new ArrayList<>();
    for (float[] rectangle : rectanglePattern.rectangles()) {
      final float minX = (rectangle[0] + x) * sizeMultiplier;
      final float minY = (rectangle[3] + y) * sizeMultiplier;
      final float maxX = (rectangle[2] + x) * sizeMultiplier;
      final float maxY = (rectangle[1] + y) * sizeMultiplier;
      if (shadow) {
        float g = red * 0.25f;
        float h = green * 0.25f;
        float l = blue * 0.25f;
        rectanglesToDraw.add(
            new BakedGlyph.Effect(minX + 1, minY + 1, maxX + 1, maxY + 1, 0, g, h, l, alpha)
        );
      }
      if (outlineRectangles != null) {
        int outlineColor = textContext.outlineColorType == OutlineColorType.AUTO ? MishangUtils.toSignOutlineColor(color) : textContext.outlineColor;
        float outlineR = (outlineColor >> 16 & 255) / 255f;
        float outlineG = (outlineColor >> 8 & 255) / 255f;
        float outlineB = (outlineColor & 255) / 255f;
        final float outlineAlpha = ((outlineColor & 0xFC000000) == 0) ? 1 : (outlineColor >> 24 & 0xFF) / 255f;
        outlineRectangles.add(
            new BakedGlyph.Effect(minX - 1, minY + 1, maxX + 1, maxY - 1, 0, outlineR, outlineG, outlineB, outlineAlpha)
        );

      }
      rectanglesToDraw.add(
          new BakedGlyph.Effect(minX, minY, maxX, maxY, shadow ? 0.03f : textContext.outlineColorType != OutlineColorType.NONE ? 0.02f : 0, red, green, blue, alpha)
      );
    }

    final Matrix4f matrix4f = matrixStack.last().pose();
    final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(layer);
    for (BakedGlyph.Effect rectangle : rectanglesToDraw) {
      glyphRenderer.renderEffect(rectangle, matrix4f, vertexConsumer, light);
    }
    if (outlineRectangles != null) {
      final VertexConsumer vertexConsumerOutline = vertexConsumers.getBuffer(glyphRenderer.renderType(Font.DisplayMode.NORMAL));
      for (BakedGlyph.Effect outlineRectangle : outlineRectangles) {
        glyphRenderer.renderEffect(outlineRectangle, matrix4f, vertexConsumerOutline, light);
      }
    }
  }

  @Override
  public float height() {
    return 7 / 8f;
  }

  @Override
  public float width() {
    return 7 / 8f;
  }

  @Override
  public @NotNull String getId() {
    return "pattern";
  }

  @Override
  public @NotNull SpecialDrawableType<PatternSpecialDrawable> getType() {
    return SpecialDrawableTypes.PATTERN;
  }

  @Override
  public SpecialDrawable cloneWithNewTextContext(@NotNull TextContext textContext) {
    return new PatternSpecialDrawable(textContext, rectanglePattern);
  }

  @Override
  public String asStringArgs() {
    return rectanglePattern.name();
  }

  @Contract(value = "_,_ -> new", pure = true)
  public static @Nullable PatternSpecialDrawable fromNbt(TextContext textContext, CompoundTag nbt) {
    final String shapeName = nbt.getString("shapeName");
    return fromName(textContext, shapeName);
  }

  @Override
  public void saveAdditional(CompoundTag nbt) {
    SpecialDrawable.super.saveAdditional(nbt);
    nbt.putString("shapeName", rectanglePattern.name());
  }

  public static @Nullable PatternSpecialDrawable fromName(TextContext textContext, String shapeName) {
    final RectanglePattern pattern = RectanglePatterns.get(shapeName);
    if (pattern == null) return null;
    return new PatternSpecialDrawable(textContext, pattern);
  }

  @Override
  public @NotNull MutableComponent asStyledText() {
    return SpecialDrawable.super.asStyledText().withStyle(style -> style.withColor(textContext.color));
  }
}
