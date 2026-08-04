package pers.solid.mishang.uc.data.stubs;

import net.minecraft.world.level.block.Block;

/**
 * Stub for Fabric's MultipartBlockStateSupplier.
 */
public class MultipartBlockStateSupplier implements BlockStateSupplier {
    public static MultipartBlockStateSupplier create(Block block) {
        return new MultipartBlockStateSupplier();
    }

    public MultipartBlockStateSupplier with(BlockStateVariant variant) {
        return this;
    }

    public MultipartBlockStateSupplier with(When when, BlockStateVariant... variants) {
        return this;
    }

    public MultipartBlockStateSupplier setValue(When when, BlockStateVariant variant) {
        return this;
    }

    public MultipartBlockStateSupplier setValue(BlockStateVariant variant) {
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
