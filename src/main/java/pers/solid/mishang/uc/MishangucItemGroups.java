package pers.solid.mishang.uc;

import pers.solid.mishang.uc.MishangUtils;

import com.google.common.base.Preconditions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;
import org.apache.commons.lang3.ObjectUtils;
import pers.solid.mishang.uc.blocks.*;
import pers.solid.mishang.uc.item.ExplosionToolItem;
import pers.solid.mishang.uc.item.FastBuildingToolItem;
import pers.solid.mishang.uc.item.MishangucItems;
import pers.solid.mishang.uc.util.ColorfulBlockRegistry;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MishangucItemGroups {
  public static CreativeModeTab ROADS;
  public static CreativeModeTab LIGHTS;
  public static CreativeModeTab SIGNS;
  public static CreativeModeTab TOOLS;
  public static CreativeModeTab DECORATIONS;
  public static CreativeModeTab COLORED_BLOCKS;

  public static final List<DyeColor> FANCY_COLORS = List.of(
      DyeColor.WHITE,
      DyeColor.LIGHT_GRAY,
      DyeColor.GRAY,
      DyeColor.BLACK,
      DyeColor.BROWN,
      DyeColor.RED,
      DyeColor.ORANGE,
      DyeColor.YELLOW,
      DyeColor.LIME,
      DyeColor.GREEN,
      DyeColor.CYAN,
      DyeColor.LIGHT_BLUE,
      DyeColor.BLUE,
      DyeColor.PURPLE,
      DyeColor.MAGENTA,
      DyeColor.PINK
  );

  /**
   * Register creative mode tabs via Forge's RegisterEvent.
   * Call this from the mod event bus listener for RegisterEvent.
   */
  public static void registerTabs(RegisterEvent event) {
    event.register(Registries.CREATIVE_MODE_TAB, helper -> {
      ROADS = CreativeModeTab.builder()
          .icon(() -> new ItemStack(RoadBlocks.ROAD_WITH_WHITE_DOUBLE_LINE))
          .title(Component.translatable("itemGroup.mishanguc.roads"))
          .displayItems((params, output) -> {
            MishangUtils.instanceStream(RoadBlocks.class, Block.class).forEach(addEntries(output));
            RoadSlabBlocks.SLABS.forEach(addEntries(output));
            MishangUtils.instanceStream(RoadMarkBlocks.class, Block.class).forEach(addEntries(output));
          })
          .build();
      helper.register(Mishanguc.id("roads"), ROADS);

      LIGHTS = CreativeModeTab.builder()
          .icon(() -> new ItemStack(LightBlocks.WHITE_LARGE_WALL_LIGHT))
          .title(Component.translatable("itemGroup.mishanguc.lights"))
          .displayItems((params, output) -> MishangUtils.instanceStream(LightBlocks.class, Block.class).forEach(addEntries(output)))
          .build();
      helper.register(Mishanguc.id("lights"), LIGHTS);

      SIGNS = CreativeModeTab.builder()
          .icon(() -> new ItemStack(StandingSignBlocks.ACACIA_STANDING_SIGN))
          .title(Component.translatable("itemGroup.mishanguc.signs"))
          .displayItems((params, output) -> {
            MishangUtils.instanceStream(WallSignBlocks.class, Block.class).forEach(addEntries(output));
            MishangUtils.instanceStream(HungSignBlocks.class, Block.class).forEach(addEntries(output));
            MishangUtils.instanceStream(StandingSignBlocks.class, Block.class).forEach(addEntries(output));
          })
          .build();
      helper.register(Mishanguc.id("signs"), SIGNS);

      TOOLS = CreativeModeTab.builder()
          .icon(() -> new ItemStack(MishangucItems.ROTATING_TOOL))
          .title(Component.translatable("itemGroup.mishanguc.tools"))
          .displayItems((params, output) -> MishangUtils.instanceStream(MishangucItems.class, ItemLike.class).forEach(item -> {
            if (item instanceof final ExplosionToolItem explosionToolItem) {
              explosionToolItem.appendToEntries(output);
            } else if (item instanceof final FastBuildingToolItem fastBuildingToolItem) {
              fastBuildingToolItem.appendToEntries(output);
            } else {
              output.accept(new ItemStack(item));
            }
          }))
          .build();
      helper.register(Mishanguc.id("tools"), TOOLS);

      DECORATIONS = CreativeModeTab.builder()
          .icon(() -> new ItemStack(HandrailBlocks.SIMPLE_ORANGE_CONCRETE_HANDRAIL))
          .title(Component.translatable("itemGroup.mishanguc.decorations"))
          .displayItems((params, output) -> MishangUtils.instanceStream(HandrailBlocks.class, Block.class).forEach(addEntries(output)))
          .build();
      helper.register(Mishanguc.id("decorations"), DECORATIONS);

      COLORED_BLOCKS = CreativeModeTab.builder()
          .icon(() -> new ItemStack(ColoredBlocks.COLORED_WOOL))
          .title(Component.translatable("itemGroup.mishanguc.colored_blocks"))
          .displayItems((params, output) -> MishangUtils.instanceStream(ColoredBlocks.class, Block.class).forEach(addEntries(output)))
          .build();
      helper.register(Mishanguc.id("colored_blocks"), COLORED_BLOCKS);
    });
  }

  public static void init() {
    Preconditions.checkState(ObjectUtils.allNotNull(ROADS, LIGHTS, SIGNS, TOOLS, DECORATIONS, COLORED_BLOCKS));
  }

  private static <T extends Block> Consumer<T> addEntries(CreativeModeTab.Output output) {
    return t -> {
      if (ColorfulBlockRegistry.WHITE_TO_COLORFUL.containsKey(t)) {
        final Map<DyeColor, ? extends Block> map = ColorfulBlockRegistry.WHITE_TO_COLORFUL.get(t);
        for (DyeColor color : FANCY_COLORS) {
          if (map.containsKey(color)) {
            output.accept(new ItemStack(map.get(color)));
          }
        }
      } else if (!ColorfulBlockRegistry.COLORFUL_BLOCKS.contains(t)) {
        output.accept(new ItemStack(t));
      }
    };
  }
}
