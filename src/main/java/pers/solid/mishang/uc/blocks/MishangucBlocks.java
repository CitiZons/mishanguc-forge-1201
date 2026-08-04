package pers.solid.mishang.uc.blocks;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DyeColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.ApiStatus;
import pers.solid.mishang.uc.Mishanguc;
import pers.solid.mishang.uc.annotations.CustomId;
import pers.solid.mishang.uc.annotations.Cutout;
import pers.solid.mishang.uc.annotations.Translucent;
import pers.solid.mishang.uc.block.HandrailBlock;
import pers.solid.mishang.uc.block.HungSignBlock;
import pers.solid.mishang.uc.block.StandingSignBlock;
import pers.solid.mishang.uc.block.WallSignBlock;
import pers.solid.mishang.uc.item.HungSignBlockItem;
import pers.solid.mishang.uc.item.NamedBlockItem;
import pers.solid.mishang.uc.item.StandingSignBlockItem;
import pers.solid.mishang.uc.item.WallSignBlockItem;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 迷上城建模组的所有方块。
 */
// TODO: Consider using DeferredRegister for proper Forge registration
public class MishangucBlocks {

  /**
   * 绝大多数柏油路方块共用的方块设置。
   */
  protected static final BlockBehaviour.Properties ROAD_SETTINGS =
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.5F);
  /**
   * 具有白色标线的道路方块使用的方块设置。
   */
  protected static final BlockBehaviour.Properties WHITE_ROAD_SETTINGS = BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.5F);
  /**
   * 具有黄色标线的道路方块使用的方块设置。
   */
  protected static final BlockBehaviour.Properties YELLOW_ROAD_SETTINGS = BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.5F);

  /**
   * 绝大多数白色光方块共用的方块设置。
   */
  protected static final BlockBehaviour.Properties WHITE_LIGHT_SETTINGS =
      BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).lightLevel(state -> 15).strength(0.2f);
  /**
   * 墙上的白色灯等方块等用到的方块设置。与{@link #WHITE_LIGHT_SETTINGS}相比，该方块设置具有{@code noCollision}属性。
   */
  protected static final BlockBehaviour.Properties WHITE_WALL_LIGHT_SETTINGS =
      BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).lightLevel(state -> 15).strength(0.2f).noCollission();
  /**
   * 绝大多数黄色光方块共用的方块设置。
   */
  protected static final BlockBehaviour.Properties YELLOW_LIGHT_SETTINGS = BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).lightLevel(state -> 15).strength(0.2f);
  /**
   * 墙上的黄色灯等方块等用到的方块设置。与{@link #YELLOW_LIGHT_SETTINGS}相比，该方块设置具有{@code noCollision}属性。
   */
  protected static final BlockBehaviour.Properties YELLOW_WALL_LIGHT_SETTINGS =
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).lightLevel(state -> 15).strength(0.2f).noCollission();
  /**
   * 绝大多数青色光方块共用的方块设置。
   */
  protected static final BlockBehaviour.Properties CYAN_LIGHT_SETTINGS = BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).lightLevel(state -> 15).strength(0.2f);
  /**
   * 墙上的青色灯等方块等用到的方块设置。与{@link #YELLOW_LIGHT_SETTINGS}相比，该方块设置具有{@code noCollision}属性。
   */
  protected static final BlockBehaviour.Properties CYAN_WALL_LIGHT_SETTINGS =
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).lightLevel(state -> 15).strength(0.2f).noCollission();
  @ApiStatus.AvailableSince("1.1.0")
  protected static final BlockBehaviour.Properties ORANGE_LIGHT_SETTINGS = BlockBehaviour.Properties.of().mapColor(DyeColor.ORANGE).lightLevel(state -> 15).strength(0.2f);
  @ApiStatus.AvailableSince("1.1.0")
  protected static final BlockBehaviour.Properties ORANGE_WALL_LIGHT_SETTINGS = BlockBehaviour.Properties.of().mapColor(DyeColor.ORANGE).lightLevel(state -> 15).strength(0.2f).noCollission();
  @ApiStatus.AvailableSince("1.1.0")
  protected static final BlockBehaviour.Properties GREEN_LIGHT_SETTINGS = BlockBehaviour.Properties.of().mapColor(DyeColor.GREEN).lightLevel(state -> 15).strength(0.2f);
  @ApiStatus.AvailableSince("1.1.0")
  protected static final BlockBehaviour.Properties GREEN_WALL_LIGHT_SETTINGS = BlockBehaviour.Properties.of().mapColor(DyeColor.GREEN).lightLevel(state -> 15).strength(0.2f).noCollission();
  @ApiStatus.AvailableSince("1.1.0")
  protected static final BlockBehaviour.Properties PINK_LIGHT_SETTINGS = BlockBehaviour.Properties.of().mapColor(DyeColor.PINK).lightLevel(state -> 15).strength(0.2f);
  @ApiStatus.AvailableSince("1.1.0")
  protected static final BlockBehaviour.Properties PINK_WALL_LIGHT_SETTINGS = BlockBehaviour.Properties.of().mapColor(DyeColor.PINK).lightLevel(state -> 15).strength(0.2f).noCollission();
  @ApiStatus.Internal
  public static ObjectArrayList<Block> translucentBlocks = new ObjectArrayList<>();
  @ApiStatus.Internal
  public static ObjectArrayList<Block> cutoutBlocks = new ObjectArrayList<>();

  /**
   * 自动注册一个类中的所有静态常量字段的方块，同时创建并注册对应的物品。
   *
   * @see CustomId
   */
  private static String[] nsPath(Field field) {
    if (field.isAnnotationPresent(CustomId.class)) {
      final CustomId annotation = field.getAnnotation(CustomId.class);
      return new String[]{annotation.nameSpace(), annotation.path()};
    }
    return new String[]{"mishanguc", field.getName().toLowerCase()};
  }

  private static boolean isBlockField(Field field) {
    final int modifier = field.getModifiers();
    return Modifier.isFinal(modifier)
        && Modifier.isStatic(modifier)
        && Block.class.isAssignableFrom(field.getType());
  }

  /** Registers all blocks during the {@code RegisterEvent} for the BLOCK registry. */
  private static <T> void registerBlocksIn(Class<T> cls, RegisterEvent.RegisterHelper<Block> helper) {
    for (Field field : cls.getFields()) {
      if (!isBlockField(field)) continue;
      try {
        final Block value = (Block) field.get(null);
        final String[] np = nsPath(field);
        final String path = np[1];
        helper.register(new ResourceLocation(np[0], path), value);
        if (field.isAnnotationPresent(Cutout.class)) {
          cutoutBlocks.add(value);
        } else if (field.isAnnotationPresent(Translucent.class)) {
          translucentBlocks.add(value);
          if (value instanceof HandrailBlock) {
            translucentBlocks.add(((HandrailBlock) value).central());
            translucentBlocks.add(((HandrailBlock) value).corner());
            translucentBlocks.add(((HandrailBlock) value).outer());
            translucentBlocks.add(((HandrailBlock) value).stair());
          }
        }
        if (value instanceof HandrailBlock handrailBlock) {
          // 如果该方块为 HandrailBlock，则一并注册其 central 等方块，因为这些方块并没有作为字段存在。
          helper.register(Mishanguc.id(path + "_central"), handrailBlock.central());
          helper.register(Mishanguc.id(path + "_corner"), handrailBlock.corner());
          helper.register(Mishanguc.id(path + "_stair"), handrailBlock.stair());
          helper.register(Mishanguc.id(path + "_outer"), handrailBlock.outer());
        }
      } catch (IllegalAccessException e) {
        Mishanguc.MISHANG_LOGGER.error("Error when registering blocks:", e);
      }
    }
  }

  /** Registers the {@link BlockItem} for each block during the {@code RegisterEvent} for the ITEM registry. */
  private static <T> void registerItemsIn(Class<T> cls, RegisterEvent.RegisterHelper<Item> helper) {
    for (Field field : cls.getFields()) {
      if (!isBlockField(field)) continue;
      try {
        final Block value = (Block) field.get(null);
        final String path = nsPath(field)[1];
        final Item.Properties settings = new Item.Properties();
        if (path.contains("netherite")) {
          settings.fireResistant();
        }
        final BlockItem item =
            value instanceof HungSignBlock
                ? new HungSignBlockItem(value, settings)
                : value instanceof WallSignBlock
                ? new WallSignBlockItem(value, settings)
                : value instanceof StandingSignBlock
                ? new StandingSignBlockItem(value, settings)
                : new NamedBlockItem(value, settings);
        helper.register(Mishanguc.id(path), item);
      } catch (IllegalAccessException e) {
        Mishanguc.MISHANG_LOGGER.error("Error when registering block items:", e);
      }
    }
  }

  public static void registerBlocks(RegisterEvent.RegisterHelper<Block> helper) {
    registerBlocksIn(RoadBlocks.class, helper);
    RoadSlabBlocks.registerSlabBlocks(helper);
    registerBlocksIn(RoadMarkBlocks.class, helper);
    registerBlocksIn(LightBlocks.class, helper);
    registerBlocksIn(WallSignBlocks.class, helper);
    registerBlocksIn(HungSignBlocks.class, helper);
    registerBlocksIn(StandingSignBlocks.class, helper);
    registerBlocksIn(HandrailBlocks.class, helper);
    registerBlocksIn(ColoredBlocks.class, helper);
  }

  public static void registerItems(RegisterEvent.RegisterHelper<Item> helper) {
    registerItemsIn(RoadBlocks.class, helper);
    RoadSlabBlocks.registerSlabItems(helper);
    registerItemsIn(RoadMarkBlocks.class, helper);
    registerItemsIn(LightBlocks.class, helper);
    registerItemsIn(WallSignBlocks.class, helper);
    registerItemsIn(HungSignBlocks.class, helper);
    registerItemsIn(StandingSignBlocks.class, helper);
    registerItemsIn(HandrailBlocks.class, helper);
    registerItemsIn(ColoredBlocks.class, helper);
  }
}
