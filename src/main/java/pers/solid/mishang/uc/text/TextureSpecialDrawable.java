package pers.solid.mishang.uc.text;

import com.google.common.annotations.Beta;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.nio.file.Path;
import java.util.Optional;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * 表示一个纹理的特殊文本内容，用于渲染其纹理，一般来说这个纹理的宽度和高度是和文本的大小相同的。
 *
 * @param identifier  纹理在游戏资源中的路径，如 {@code "textures/block/stone.png"}。
 * @param textContext 该对象对应的文本。在 {@link TextContext#draw} 中渲染时，会根据其大小来决定这个纹理渲染的大小，偏移等参数也是同理。
 */
@Beta
public record TextureSpecialDrawable(@NotNull ResourceLocation identifier, @NotNull TextContext textContext) implements SpecialDrawable {
  public TextureSpecialDrawable {
    validateIdentifier(identifier);
  }

  /**
   * 检验路径是否有效，如果无效，抛出异常。注意：不检查资源是否存在。可以存储指定不存在资源的对象，但路径不能是无效的。
   */
  public static void validateIdentifier(ResourceLocation identifier) throws IllegalArgumentException {
    FileUtil.decomposePath(identifier.getPath()).error().ifPresent(error -> {throw new IllegalArgumentException(error.message());});
    var ignore = Path.of(identifier.getNamespace());
  }

  public static boolean isValidIdentifier(ResourceLocation identifier) {
    try {
      validateIdentifier(identifier);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void drawExtra(Font font, PoseStack matrixStack, MultiBufferSource vertexConsumers, int light, float x, float y) {
    final Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(identifier);
    final RenderType layer;
    if (resource.isEmpty()) {
      layer = RenderType.text(MissingTextureAtlasSprite.getLocation());
    } else {
      layer = RenderType.text(identifier);
    }
    final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(layer);
    final Matrix4f matrix4f = matrixStack.last().pose();

      vertexConsumer.vertex(matrix4f, 0, 8, -0).color(255, 255, 255, 255).uv(0.0f, 1.0f).uv2(light).endVertex();
      vertexConsumer.vertex(matrix4f, 8, 8, -0).color(255, 255, 255, 255).uv(1.0f, 1.0f).uv2(light).endVertex();
      vertexConsumer.vertex(matrix4f, 8, 0, -0).color(255, 255, 255, 255).uv(1.0f, 0.0f).uv2(light).endVertex();
      vertexConsumer.vertex(matrix4f, 0, 0, -0).color(255, 255, 255, 255).uv(0.0f, 0.0f).uv2(light).endVertex();
  }

  @Override
  public @NotNull String getId() {
    return "texture";
  }

  @Override
  public @NotNull SpecialDrawableType<TextureSpecialDrawable> getType() {
    return SpecialDrawableTypes.TEXTURE;
  }

  @Override
  public float width() {
    return 1;
  }

  @Override
  public float height() {
    return 1;
  }

  @Override
  public TextureSpecialDrawable cloneWithNewTextContext(@NotNull TextContext textContext) {
    return new TextureSpecialDrawable(identifier, textContext);
  }

  @Override
  public void saveAdditional(CompoundTag nbt) {
    SpecialDrawable.super.saveAdditional(nbt);
    nbt.putString("texture", identifier.toString());
  }

  @Override
  public String asStringArgs() {
    return identifier.toString();
  }
}
