package pers.solid.mishang.uc.text;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.mixin.TextRendererAccessor;
import pers.solid.mishang.uc.util.TextBridge;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * 长方形，可以指定其宽度和高度。
 *
 * @param width  长方形宽度，若为 8 则与文本大小的高度（注意不是文本宽度）相同。
 * @param height 长方形的高度，若为 8 则与文本大小的高度相同。
 * @since 0.2.1 将此类改成了记录。
 */
public record RectSpecialDrawable(float width, float height, @NotNull TextContext textContext) implements SpecialDrawable {

  @OnlyIn(Dist.CLIENT)
  @Override
  public void drawExtra(Font font, PoseStack matrixStack, MultiBufferSource vertexConsumers, int light, float x, float y) {
    int color = textContext.color;
    final float red = (float) (color >> 16 & 0xFF) / 255.0f;
    final float green = (float) (color >> 8 & 0xFF) / 255.0f;
    final float blue = (float) (color & 0xFF) / 255.0f;
    final float alpha = ((color & 0xFC000000) == 0) ? 1 : (float) (color >> 24 & 0xFF) / 255.0f;
    BakedGlyph glyphRenderer = ((TextRendererAccessor) font).invokeGetFontSet(Style.DEFAULT_FONT).whiteGlyph();
    final Matrix4f matrix4f = matrixStack.last().pose();
    final RenderType layer = glyphRenderer.renderType(textContext.outlineColorType != OutlineColorType.NONE ? Font.DisplayMode.POLYGON_OFFSET : textContext.seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL);
    final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(layer);
    final boolean shadow = textContext.outlineColorType == OutlineColorType.NONE && textContext.shadow;
    if (shadow) {
      // 绘制阴影
      BakedGlyph.Effect shadowRectangle = new BakedGlyph.Effect(x + 1, (height + y) + 1, (width + x) + 1, y + 1, 0, red * 0.25f, green * 0.25f, blue * 0.25f, alpha);
      glyphRenderer.renderEffect(shadowRectangle, matrix4f, vertexConsumer, light);
    }
    if (textContext.outlineColorType != OutlineColorType.NONE) {
      // 绘制轮廓
      int outlineColor = textContext.outlineColorType == OutlineColorType.AUTO ? MishangUtils.toSignOutlineColor(color) : textContext.outlineColor;
      final float outlineAlpha = ((outlineColor & 0xfc000000) == 0) ? 1 : (outlineColor >> 24 & 0xFF) / 255f;
      BakedGlyph.Effect outlineRectangle = new BakedGlyph.Effect(x - 1, (height + y) + 1, (width + x) + 1, y - 1, 0, (outlineColor >> 16 & 255) / 255f, (outlineColor >> 8 & 255) / 255f, (outlineColor & 255) / 255f, outlineAlpha);
      glyphRenderer.renderEffect(outlineRectangle, matrix4f, vertexConsumers.getBuffer(glyphRenderer.renderType(Font.DisplayMode.NORMAL)), light);
    }

    final VertexConsumer vertexConsumer2 = vertexConsumers.getBuffer(layer);
    BakedGlyph.Effect rectangle = new BakedGlyph.Effect(x, (height + y), (width + x), y, shadow ? 0.03f : textContext.outlineColorType != OutlineColorType.NONE ? 0.02f : 0, red, green, blue, alpha);
    glyphRenderer.renderEffect(rectangle, matrix4f, vertexConsumer2, light);
  }

  @Override
  public @NotNull String getId() {
    return "rect";
  }

  @Override
  public @NotNull SpecialDrawableType<RectSpecialDrawable> getType() {
    return SpecialDrawableTypes.RECT;
  }

  public static @NotNull RectSpecialDrawable fromNbt(@NotNull TextContext textContext, @NotNull CompoundTag nbt) {
    return new RectSpecialDrawable(nbt.getFloat("width"), nbt.getFloat("height"), textContext);
  }

  public static @NotNull RectSpecialDrawable fromStringArgs(@NotNull TextContext textContext, @NotNull String args) throws CommandSyntaxException {
    final String[] split = args.split(" ");
    if (split.length < 2) {
      throw new CommandSyntaxException(null, TextBridge.translatable("special_drawable.rect.too_few", 2, split.length));
    } else if (split.length > 2) {
      throw new CommandSyntaxException(null, TextBridge.translatable("special_drawable.rect.too_many", 2, split.length));
    }
    final float width;
    try {
      width = Float.parseFloat(split[0]);
    } catch (NumberFormatException e) {
      throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidFloat().create(split[0]);
    }
    final float height;
    try {
      height = Float.parseFloat(split[1]);
    } catch (NumberFormatException e) {
      throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidFloat().create(split[1]);
    }
    return new RectSpecialDrawable(width, height, textContext);
  }

  @Override
  public void saveAdditional(CompoundTag nbt) {
    SpecialDrawable.super.saveAdditional(nbt);
    nbt.putFloat("width", width);
    nbt.putFloat("height", height);
  }

  @Override
  public String asStringArgs() {
    return String.format("%s %s",
        width % 1 == 0 ? Integer.toString((int) width) : Float.toString(width),
        height % 1 == 0 ? Integer.toString((int) height) : Float.toString(height));
  }

  @Override
  public float width() {
    return width / 8;
  }

  @Override
  public float height() {
    return height / 8;
  }

  @Override
  public RectSpecialDrawable clone() {
    try {
      return (RectSpecialDrawable) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public SpecialDrawable cloneWithNewTextContext(@NotNull TextContext textContext) {
    return new RectSpecialDrawable(width, height, textContext);
  }

  @Override
  public @NotNull MutableComponent asStyledText() {
    return TextBridge.empty()
        .append(TextBridge.literal("■").withStyle(style -> style.withColor(textContext.color)))
        .append(" (" + width + "×" + height + ")");
  }
}
