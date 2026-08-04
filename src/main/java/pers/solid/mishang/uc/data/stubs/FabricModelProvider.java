package pers.solid.mishang.uc.data.stubs;

// ItemModelGenerator is in this package

/**
 * Stub for Fabric's FabricModelProvider (net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider).
 * To be replaced with proper Forge data gen implementation later.
 *
 * In Fabric, FabricModelProvider is an abstract class that provides generateBlockStateModels
 * and generateItemModels as abstract methods.
 */
public abstract class FabricModelProvider extends ModelProvider {

    public FabricModelProvider(FabricDataOutput output) {
        // Stub constructor
    }

    /**
     * Override to register block state and model files for blocks.
     */
    public abstract void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator);

    /**
     * Override to register item models.
     */
    public abstract void generateItemModels(ItemModelGenerator itemModelGenerator);
}
