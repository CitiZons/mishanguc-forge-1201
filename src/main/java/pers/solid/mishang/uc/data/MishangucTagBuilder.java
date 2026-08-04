package pers.solid.mishang.uc.data;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

public class MishangucTagBuilder<T> extends TagsProvider.TagAppender<T> {
  private final TagKey<T> tagKey;
  private final Function<T, ResourceKey<T>> valueToKey;

  protected MishangucTagBuilder(TagKey<T> tagKey, TagBuilder builder, Function<T, ResourceKey<T>> valueToKey) {
    super(builder, "mishanguc");
    this.tagKey = tagKey;
    this.valueToKey = valueToKey;
  }

  public MishangucTagBuilder<T> add(T value) {
    add(valueToKey.apply(value));
    return this;
  }

  @SafeVarargs
  public final MishangucTagBuilder<T> add(T... values) {
    Stream.of(values).map(this.valueToKey).forEach(this::add);
    return this;
  }

  @SafeVarargs
  public final MishangucTagBuilder<T> addKeys(ResourceKey<T>... keys) {
    super.add(keys);
    return this;
  }

  @Override
  public MishangucTagBuilder<T> addOptional(ResourceLocation id) {
    super.addOptional(id);
    return this;
  }

  @Override
  public MishangucTagBuilder<T> addTag(TagKey<T> identifiedTag) {
    super.addTag(identifiedTag);
    return this;
  }

  @SafeVarargs
  public final MishangucTagBuilder<T> addTag(TagKey<T>... tags) {
    for (TagKey<T> tag : tags) {
      super.addTag(tag);
    }
    return this;
  }

  public MishangucTagBuilder<T> addTag(MishangucTagBuilder<T> builder) {
    return addTag(builder.tagKey);
  }

  @SafeVarargs
  public final MishangucTagBuilder<T> addTag(MishangucTagBuilder<T>... builders) {
    Arrays.stream(builders).map(b -> b.tagKey).forEach(this::addTag);
    return this;
  }

  @Override
  public MishangucTagBuilder<T> addOptionalTag(ResourceLocation id) {
    super.addOptionalTag(id);
    return this;
  }
}
