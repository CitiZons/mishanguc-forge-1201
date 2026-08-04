package pers.solid.mishang.uc.data.stubs;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Stub for Fabric's ConventionalBlockTags (net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags).
 * Maps Fabric conventional tag names to Forge/c namespace equivalents.
 * To be replaced with proper Forge tag implementation later.
 */
public final class ConventionalBlockTags {
    private ConventionalBlockTags() {}

    // Glass tags
    public static final TagKey<Block> GLASS_BLOCKS = tag("glass_blocks");
    public static final TagKey<Block> GLASS_PANES = tag("glass_panes");

    private static TagKey<Block> tag(String path) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation("c", path));
    }
}
