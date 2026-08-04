package pers.solid.mishang.uc.data.stubs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModelIds {
    private ModelIds() {}

    public static ResourceLocation getBlockModelId(Block block) {
        ResourceLocation id = block.builtInRegistryHolder().key().location();
        return new ResourceLocation(id.getNamespace(), "block/" + id.getPath());
    }

    public static ResourceLocation getBlockSubModelId(Block block, String suffix) {
        ResourceLocation id = block.builtInRegistryHolder().key().location();
        return new ResourceLocation(id.getNamespace(), "block/" + id.getPath() + suffix);
    }

    public static ResourceLocation getItemModelId(Item item) {
        ResourceLocation id = item.builtInRegistryHolder().key().location();
        return new ResourceLocation(id.getNamespace(), "item/" + id.getPath());
    }

    public static ResourceLocation getItemSubModelId(Item item, String suffix) {
        ResourceLocation id = item.builtInRegistryHolder().key().location();
        return new ResourceLocation(id.getNamespace(), "item/" + id.getPath() + suffix);
    }
}
