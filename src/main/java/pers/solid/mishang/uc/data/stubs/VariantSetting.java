package pers.solid.mishang.uc.data.stubs;

import com.google.gson.JsonElement;

import java.util.function.Function;

public class VariantSetting<T> extends VariantSettings.Key<T> {
    private final Function<T, JsonElement> valueWriter;

    public VariantSetting(String key, Function<T, JsonElement> valueWriter) {
        super(key);
        this.valueWriter = valueWriter;
    }

    public JsonElement write(T value) { return valueWriter.apply(value); }
}
