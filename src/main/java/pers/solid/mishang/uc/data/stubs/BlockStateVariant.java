package pers.solid.mishang.uc.data.stubs;

import net.minecraft.resources.ResourceLocation;

/**
 * Stub for Fabric's BlockStateVariant.
 * TODO: Replace with Forge data gen.
 */
public class BlockStateVariant {
    public static BlockStateVariant create() {
        return new BlockStateVariant();
    }

    public BlockStateVariant put(VariantSettings.Key<?> key, Object value) {
        return this;
    }
}
