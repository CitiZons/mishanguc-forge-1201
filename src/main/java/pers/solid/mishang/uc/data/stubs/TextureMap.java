package pers.solid.mishang.uc.data.stubs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * Stub for Fabric's TextureMap (net.minecraft.data.models.TextureMap in Fabric mappings).
 * To be replaced with proper Forge data gen implementation later.
 */
public class TextureMap implements Cloneable {
    private final Map<TextureKey, ResourceLocation> entries = new HashMap<>();

    public TextureMap put(TextureKey key, ResourceLocation id) {
        if (key != null && id != null) {
            entries.put(key, id);
        }
        return this;
    }

    public ResourceLocation getTexture(TextureKey key) {
        return entries.get(key);
    }

    // Static factory methods

    public static TextureMap all(ResourceLocation id) {
        return new TextureMap().put(TextureKey.ALL, id);
    }

    public static TextureMap all(Block block) {
        return all(getId(block));
    }

    public static TextureMap all(ItemLike item) {
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.asItem());
        return all(itemId != null ? new ResourceLocation(itemId.getNamespace(), "item/" + itemId.getPath())
                                  : ResourceLocation.tryParse("item/" + item.asItem()));
    }

    public static TextureMap of(TextureKey key, ResourceLocation id) {
        return new TextureMap().put(key, id);
    }

    public static TextureMap layer0(ItemLike item) {
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.asItem());
        ResourceLocation id = itemId != null
                ? new ResourceLocation(itemId.getNamespace(), "item/" + itemId.getPath())
                : ResourceLocation.tryParse("item/" + item.asItem());
        return new TextureMap().put(TextureKey.LAYER0, id);
    }

    public static TextureMap layer0(ResourceLocation id) {
        return new TextureMap().put(TextureKey.LAYER0, id);
    }

    /**
     * Creates a TextureMap with TextureKey.TEXTURE mapped to the given ResourceLocation.
     * This is a Fabric helper equivalent.
     */
    public static TextureMap texture(ResourceLocation id) {
        return new TextureMap().put(TextureKey.TEXTURE, id);
    }

    public static TextureMap sideEnd(ResourceLocation side, ResourceLocation end) {
        return new TextureMap().put(TextureKey.SIDE, side).put(TextureKey.END, end);
    }

    public static TextureMap sideEnd(Block block) {
        return sideEnd(getSubId(block, "_side"), getSubId(block, "_top"));
    }

    private static ResourceLocation getSubId(Block block, String suffix) {
        ResourceLocation id = getId(block);
        return new ResourceLocation(id.getNamespace(), id.getPath() + suffix);
    }

    public static ResourceLocation getId(Block block) {
        ResourceLocation blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        if (blockId != null) {
            return new ResourceLocation(blockId.getNamespace(), "block/" + blockId.getPath());
        }
        return ResourceLocation.tryParse("block/" + block);
    }

    public Map<TextureKey, ResourceLocation> getEntries() {
        return entries;
    }

    @Override
    public TextureMap clone() {
        try {
            TextureMap clone = (TextureMap) super.clone();
            clone.entries.putAll(this.entries);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
