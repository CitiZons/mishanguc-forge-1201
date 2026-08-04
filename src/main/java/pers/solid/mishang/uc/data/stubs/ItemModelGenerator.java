package pers.solid.mishang.uc.data.stubs;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.BiConsumer;

public class ItemModelGenerator {
    public final BiConsumer<ResourceLocation, JsonObject> writer;

    public ItemModelGenerator(BiConsumer<ResourceLocation, JsonObject> writer) {
        this.writer = writer;
    }

    public void register(Item item, Model model) {
        ResourceLocation id = ModelIds.getItemModelId(item);
        writer.accept(id, new JsonObject());
    }

    public void register(Item item, String suffix, Model model) {
        ResourceLocation id = ModelIds.getItemSubModelId(item, suffix);
        writer.accept(id, new JsonObject());
    }
}
