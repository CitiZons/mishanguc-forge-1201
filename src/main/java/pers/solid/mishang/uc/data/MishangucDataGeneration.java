package pers.solid.mishang.uc.data;

import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.core.HolderLookup;

@Mod.EventBusSubscriber(modid = "mishanguc", bus = Mod.EventBusSubscriber.Bus.MOD)
public class MishangucDataGeneration {

  @SubscribeEvent
  public static void gatherData(GatherDataEvent event) {
    // TODO: Convert Fabric data generator providers to Forge equivalents.
    // The Fabric FabricDataGenerator.Pack concept maps to Forge's DataGenerator + PackOutput.
    // Provider registrations are commented out until the providers themselves are converted.
    //
    // final DataGenerator generator = event.getGenerator();
    // final PackOutput output = generator.getPackOutput();
    // final CompletableFuture<HolderLookup.Provider> registriesFuture = event.getLookupProvider();
    //
    // generator.addProvider(event.includeServer(), new MishangucBlockLootTableProvider(output));
    // generator.addProvider(event.includeServer(), new MishangucRecipeProvider(output));
    // generator.addProvider(event.includeClient(), new MishangucModelProvider(output));
    // final MishangucBlockTagProvider blockTagProvider = new MishangucBlockTagProvider(output, registriesFuture, event.getExistingFileHelper());
    // generator.addProvider(event.includeServer(), blockTagProvider);
    // generator.addProvider(event.includeServer(), blockTagProvider.affiliate);
  }
}
