package pers.solid.mishang.uc.data.stubs;

import com.google.gson.JsonElement;
import net.minecraft.world.level.block.Block;

/**
 * Stub for Fabric's BlockStateSupplier (net.minecraft.data.models.BlockStateSupplier in Fabric mappings).
 * To be replaced with proper Forge data gen implementation later.
 */
public interface BlockStateSupplier {
    Block getBlock();

    JsonElement get();

    /**
     * Alias for get() - Fabric's API uses getValue() in some places.
     */
    default JsonElement getValue() {
        return get();
    }
}
