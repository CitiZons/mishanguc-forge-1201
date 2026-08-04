package pers.solid.mishang.uc.data.stubs;

import net.minecraft.world.level.block.Block;

/**
 * Stub for Fabric's VariantsBlockStateSupplier.
 * TODO: Replace with Forge data gen.
 */
public class VariantsBlockStateSupplier implements BlockStateSupplier {
    public static VariantsBlockStateSupplier create(Block block, BlockStateVariant... variants) {
        return new VariantsBlockStateSupplier();
    }

    public VariantsBlockStateSupplier coordinate(BlockStateVariantMap map) {
        return this;
    }

    @Override
    public Block getBlock() {
        return null;
    }

    @Override
    public com.google.gson.JsonElement get() {
        return new com.google.gson.JsonObject();
    }
}
