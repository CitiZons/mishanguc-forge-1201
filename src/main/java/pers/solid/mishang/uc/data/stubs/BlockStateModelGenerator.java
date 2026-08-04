package pers.solid.mishang.uc.data.stubs;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Stub for Fabric's BlockStateModelGenerator.
 * TODO: Replace with Forge BlockStateProvider.
 */
public class BlockStateModelGenerator {
    public final BiConsumer<ResourceLocation, JsonObject> modelCollector;
    public final Consumer<BlockStateSupplier> blockStateCollector;

    public BlockStateModelGenerator(
            BiConsumer<ResourceLocation, JsonObject> modelCollector,
            Consumer<BlockStateSupplier> blockStateCollector) {
        this.modelCollector = modelCollector;
        this.blockStateCollector = blockStateCollector;
    }

    public void registerParentedItemModel(Block block, ResourceLocation parentModelId) {}

    public static VariantsBlockStateSupplier createSingletonBlockState(Block block, ResourceLocation modelId) {
        return new VariantsBlockStateSupplier();
    }

    public static BlockStateVariantMap createSouthDefaultHorizontalRotationStates() {
        return new BlockStateVariantMap();
    }

    public static BlockStateVariantMap createNorthDefaultHorizontalRotationStates() {
        return new BlockStateVariantMap();
    }

    public static BlockStateSupplier createBlockStateWithTwoModelAndRandomInversion(
            Block block, ResourceLocation modelId, ResourceLocation mirroredModelId) {
        return new VariantsBlockStateSupplier();
    }

    public static BlockStateSupplier createAxisRotatedBlockState(
            Block block, ResourceLocation modelId, ResourceLocation horizontalModelId) {
        return new VariantsBlockStateSupplier();
    }

    public static BlockStateSupplier createSlabBlockState(
            Block block, ResourceLocation bottomModelId, ResourceLocation topModelId, ResourceLocation fullModelId) {
        return new VariantsBlockStateSupplier();
    }

    public static BlockStateSupplier createStairsBlockState(
            Block block, ResourceLocation innerModelId, ResourceLocation modelId, ResourceLocation outerModelId) {
        return new VariantsBlockStateSupplier();
    }

    public static BlockStateSupplier createBlockStateWithRandomHorizontalRotations(
            Block block, ResourceLocation modelId) {
        return new VariantsBlockStateSupplier();
    }
}
