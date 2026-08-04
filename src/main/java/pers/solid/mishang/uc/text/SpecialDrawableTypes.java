package pers.solid.mishang.uc.text;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ResourceLocationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.Mishanguc;
import pers.solid.mishang.uc.util.TextBridge;

public final class SpecialDrawableTypes {
  private SpecialDrawableTypes() {
  }

  public static final SpecialDrawableType<SpecialDrawable> INVALID = register("invalid", (textContext, nbtCompound) -> SpecialDrawable.INVALID, (textContext, s) -> SpecialDrawable.INVALID);

  public static final SpecialDrawableType<RectSpecialDrawable> RECT = register("rect", RectSpecialDrawable::fromNbt, RectSpecialDrawable::fromStringArgs);

  public static final SpecialDrawableType<PatternSpecialDrawable> PATTERN = register("pattern", PatternSpecialDrawable::fromNbt, (textContext, args) -> {
    final PatternSpecialDrawable pattern = PatternSpecialDrawable.fromName(textContext, args);
    if (pattern == null) {
      throw new CommandSyntaxException(null, TextBridge.translatable("special_drawable.pattern.invalid_name", args));
    } else {
      return pattern;
    }
  });

  public static final SpecialDrawableType<TextureSpecialDrawable> TEXTURE = register("texture", (textContext, nbt) -> {
    final ResourceLocation texture = ResourceLocation.tryParse(nbt.getString("texture"));
    return texture != null && TextureSpecialDrawable.isValidIdentifier(texture) ? new TextureSpecialDrawable(texture, textContext) : null;
  }, (textContext, args) -> {
    final ResourceLocation identifier;
    try {
      identifier = new ResourceLocation(args);
    } catch (ResourceLocationException e) {
      throw new CommandSyntaxException(null, TextBridge.translatable("argument.id.invalid"));
    }
    if (FMLEnvironment.dist == Dist.CLIENT) {
      try {
        TextureSpecialDrawable.validateIdentifier(identifier);
      } catch (IllegalArgumentException e) {
        throw new CommandSyntaxException(null, TextBridge.literal(e.getMessage()));
      }
      return new TextureSpecialDrawable(identifier, textContext);
    }
    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
  });

  private static <T extends SpecialDrawableType<? extends SpecialDrawable>> T register(String namePath, T specialDrawable) {
    return Registry.register(SpecialDrawableType.REGISTRY, Mishanguc.id(namePath), specialDrawable);
  }

  private static <S extends SpecialDrawable> SpecialDrawableType<S> register(String namePath, FromNbt<S> fromNbt, FromStringArgs<S> fromStringArgs) {
    return register(namePath, new Simple<>(fromNbt, fromStringArgs));
  }

  private record Simple<S extends SpecialDrawable>(FromNbt<S> fromNbt, FromStringArgs<S> fromStringArgs) implements SpecialDrawableType<S> {

    @Override
    public @Nullable S fromNbt(@NotNull TextContext textContext, @NotNull CompoundTag nbt) {
      return fromNbt.fromNbt(textContext, nbt);
    }

    @Override
    public @NotNull S fromStringArgs(@NotNull TextContext textContext, @NotNull String args) throws CommandSyntaxException {
      return fromStringArgs.fromStringArgs(textContext, args);
    }
  }

  @SuppressWarnings("EmptyMethod")
  public static void init() {
  }

  @FunctionalInterface
  public interface FromNbt<S extends SpecialDrawable> {
    @Nullable S fromNbt(@NotNull TextContext textContext, @NotNull CompoundTag nbt);
  }

  @FunctionalInterface
  public interface FromStringArgs<S> {
    @NotNull S fromStringArgs(@NotNull TextContext textContext, @NotNull String args) throws CommandSyntaxException;
  }
}
