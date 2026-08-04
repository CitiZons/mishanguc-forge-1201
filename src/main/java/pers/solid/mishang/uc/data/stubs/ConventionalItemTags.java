package pers.solid.mishang.uc.data.stubs;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Stub for Fabric's ConventionalItemTags (net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags).
 * Maps Fabric conventional tag names to Forge/c namespace equivalents.
 * To be replaced with proper Forge tag implementation later.
 */
public final class ConventionalItemTags {
    private ConventionalItemTags() {}

    // Dye tags
    public static final TagKey<Item> BLACK_DYES = tag("dyes/black");
    public static final TagKey<Item> BLUE_DYES = tag("dyes/blue");
    public static final TagKey<Item> BROWN_DYES = tag("dyes/brown");
    public static final TagKey<Item> CYAN_DYES = tag("dyes/cyan");
    public static final TagKey<Item> GRAY_DYES = tag("dyes/gray");
    public static final TagKey<Item> GREEN_DYES = tag("dyes/green");
    public static final TagKey<Item> LIGHT_BLUE_DYES = tag("dyes/light_blue");
    public static final TagKey<Item> LIGHT_GRAY_DYES = tag("dyes/light_gray");
    public static final TagKey<Item> LIME_DYES = tag("dyes/lime");
    public static final TagKey<Item> MAGENTA_DYES = tag("dyes/magenta");
    public static final TagKey<Item> ORANGE_DYES = tag("dyes/orange");
    public static final TagKey<Item> PINK_DYES = tag("dyes/pink");
    public static final TagKey<Item> PURPLE_DYES = tag("dyes/purple");
    public static final TagKey<Item> RED_DYES = tag("dyes/red");
    public static final TagKey<Item> WHITE_DYES = tag("dyes/white");
    public static final TagKey<Item> YELLOW_DYES = tag("dyes/yellow");

    // Material tags
    public static final TagKey<Item> IRON_INGOTS = tag("ingots/iron");
    public static final TagKey<Item> GOLD_INGOTS = tag("ingots/gold");
    public static final TagKey<Item> EMERALDS = tag("gems/emerald");
    public static final TagKey<Item> DIAMONDS = tag("gems/diamond");
    public static final TagKey<Item> NETHERITE_INGOTS = tag("ingots/netherite");
    public static final TagKey<Item> LAPIS = tag("gems/lapis");

    // Glass tags
    public static final TagKey<Item> GLASS_BLOCKS = tag("glass_blocks");
    public static final TagKey<Item> GLASS_PANES = tag("glass_panes");

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("c", path));
    }
}
