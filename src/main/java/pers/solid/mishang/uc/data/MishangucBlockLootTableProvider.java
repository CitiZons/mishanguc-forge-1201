package pers.solid.mishang.uc.data;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.resources.ResourceLocation;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.block.MishangucBlock;
import pers.solid.mishang.uc.data.stubs.FabricBlockLootTableProvider;
import pers.solid.mishang.uc.data.stubs.FabricDataOutput;

public class MishangucBlockLootTableProvider extends FabricBlockLootTableProvider {
  protected MishangucBlockLootTableProvider(FabricDataOutput dataOutput) {
    super(dataOutput);
  }

  @Override
  public void generate() {
    for (Block block : MishangUtils.blocks()) {
      if (block instanceof MishangucBlock r) {
        final ResourceLocation lootTableId = block.getLootTable();
        if (BuiltInLootTables.EMPTY.equals(lootTableId)) {
          continue;
        }
        final LootTable.Builder lootTable = r.getLootTable(this);
        lootTables.put(lootTableId, lootTable);
      } else {
        throw new IllegalStateException();
      }
    }
  }
}
