package pers.solid.mishang.uc.data.stubs;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.CompletableFuture;

/**
 * Stub for Fabric's FabricTagProvider (net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider).
 * To be replaced with proper Forge data gen implementation later.
 */
public abstract class FabricTagProvider<T> {

    protected FabricTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        // Stub constructor
    }

    /**
     * Override to add tags during data generation.
     */
    protected abstract void configure(HolderLookup.Provider wrapperLookup);

    /**
     * Get or create a tag builder for the given tag key.
     * Stub returns an empty TagBuilder.
     */
    @SuppressWarnings("deprecation")
    public TagBuilder getTagBuilder(TagKey<T> tag) {
        return TagBuilder.create();
    }

    // ---- Inner classes ----

    /**
     * Stub for FabricTagProvider.BlockTagProvider.
     */
    public abstract static class BlockTagProvider extends FabricTagProvider<Block> {

        protected BlockTagProvider(FabricDataOutput output,
                                   CompletableFuture<HolderLookup.Provider> registriesFuture) {
            super(output, registriesFuture);
        }
    }

    /**
     * Stub for FabricTagProvider.ItemTagProvider.
     */
    public abstract static class ItemTagProvider extends FabricTagProvider<Item> {

        protected ItemTagProvider(FabricDataOutput output,
                                   CompletableFuture<HolderLookup.Provider> completableFuture,
                                   BlockTagProvider blockTagProvider) {
            super(output, completableFuture);
        }

        /**
         * Copy a block tag to an item tag.
         * Stub — no-op.
         */
        public void copy(TagKey<Block> blockTag, TagKey<Item> itemTag) {
            // Stub — no-op
        }
    }
}
