package pers.solid.mishang.uc.data.stubs;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Stub for Fabric's Model (net.minecraft.data.models.Model in Fabric mappings).
 * To be replaced with proper Forge data gen implementation later.
 */
public class Model {
    private final Optional<ResourceLocation> parent;
    private final Optional<String> variant;
    private final TextureKey[] requiredTextures;

    public Model(Optional<ResourceLocation> parent, Optional<String> variant, TextureKey... requiredTextures) {
        this.parent = parent;
        this.variant = variant;
        this.requiredTextures = requiredTextures;
    }

    /**
     * Upload model for a block — returns the model ResourceLocation.
     */
    public ResourceLocation upload(Block block, TextureMap textures, BiConsumer<ResourceLocation, JsonObject> modelCollector) {
        ResourceLocation id = getModelId(block);
        modelCollector.accept(id, createJson(id, textures));
        return id;
    }

    /**
     * Upload model for an explicit id — returns the model ResourceLocation.
     */
    public ResourceLocation upload(ResourceLocation id, TextureMap textures, BiConsumer<ResourceLocation, JsonObject> modelCollector) {
        modelCollector.accept(id, createJson(id, textures));
        return id;
    }

    /**
     * Upload with custom json writer function.
     */
    public ResourceLocation upload(ResourceLocation id, TextureMap textures,
                                   BiConsumer<ResourceLocation, JsonObject> modelCollector,
                                   BiFunction<ResourceLocation, TextureMap, JsonObject> jsonFactory) {
        JsonObject json = jsonFactory.apply(id, textures);
        modelCollector.accept(id, json);
        return id;
    }

    public JsonObject createJson(ResourceLocation id, TextureMap textures) {
        JsonObject obj = new JsonObject();
        parent.ifPresent(p -> obj.addProperty("parent", p.toString()));
        return obj;
    }

    private ResourceLocation getModelId(Block block) {
        ResourceLocation blockId = block.builtInRegistryHolder().key().location();
        String suffix = variant.map(v -> v.startsWith("_") ? v : "_" + v).orElse("");
        return new ResourceLocation(blockId.getNamespace(), "block/" + blockId.getPath() + suffix);
    }

    public Optional<ResourceLocation> getParent() {
        return parent;
    }

    public Optional<String> getVariant() {
        return variant;
    }

    public TextureKey[] getRequiredTextures() {
        return requiredTextures;
    }
}
