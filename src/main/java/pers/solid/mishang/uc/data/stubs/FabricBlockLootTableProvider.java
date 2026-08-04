package pers.solid.mishang.uc.data.stubs;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Stub for Fabric's FabricBlockLootTableProvider
 * (net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider).
 * To be replaced with proper Forge data gen implementation later.
 *
 * In Fabric, FabricBlockLootTableProvider extends BlockLootTableGenerator and provides
 * a lootTables map to collect loot tables.
 */
public abstract class FabricBlockLootTableProvider extends BlockLootSubProvider {

    /**
     * Map from loot table ResourceLocation to its builder.
     * Subclasses add entries here in their generate() method.
     */
    public final Map<ResourceLocation, LootTable.Builder> lootTables = new HashMap<>();

    protected FabricBlockLootTableProvider(FabricDataOutput dataOutput) {
        super(java.util.Set.of(), net.minecraft.world.flag.FeatureFlags.REGISTRY.allFlags());
    }

    /**
     * Override to generate loot tables and add them to {@link #lootTables}.
     */
    public abstract void generate();

    /** Public helper returning a single-item loot table builder (Forge equivalent of Yarn drops). */
    public LootTable.Builder drops(Block block) {
        return createSingleItemTable(block);
    }

    public LootTable.Builder drops(ItemLike item) {
        return createSingleItemTable(item);
    }

    public LootTable.Builder drops(Block block, ItemLike item) {
        return createSingleItemTable(item);
    }

    public LootTable.Builder drops(Block block, NumberProvider count) {
        return createSingleItemTable(block, count);
    }

    @Override
    public LootTable.Builder createSlabItemTable(Block block) {
        return super.createSlabItemTable(block);
    }

    @Override
    public LootTable.Builder createLeavesDrops(Block leaves, Block sapling, float... chances) {
        return super.createLeavesDrops(leaves, sapling, chances);
    }

    @Override
    public LootTable.Builder createOakLeavesDrops(Block leaves, Block sapling, float... chances) {
        return super.createOakLeavesDrops(leaves, sapling, chances);
    }

    public LootTable.Builder mangroveLeavesDrops(Block leaves) {
        return createLeavesDrops(leaves, Blocks.MANGROVE_PROPAGULE, NORMAL_LEAVES_SAPLING_CHANCES);
    }

    public static LootTable.Builder createSilkTouchOnlyTable(ItemLike item) {
        return BlockLootSubProvider.createSilkTouchOnlyTable(item);
    }
}
