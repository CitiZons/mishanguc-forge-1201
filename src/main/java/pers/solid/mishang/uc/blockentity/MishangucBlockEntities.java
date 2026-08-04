package pers.solid.mishang.uc.blockentity;

import com.google.common.collect.Streams;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.RegisterEvent;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.Mishanguc;
import pers.solid.mishang.uc.block.*;
import pers.solid.mishang.uc.blocks.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class MishangucBlockEntities {

  /** Block entity types built during static init, registered later via {@link #registerAll}. */
  private static final Map<ResourceLocation, BlockEntityType<?>> TO_REGISTER = new LinkedHashMap<>();

  public static final BlockEntityType<SimpleColoredBlockEntity> SIMPLE_COLORED_BLOCK_ENTITY = register(
      "simple_colored_block_entity",
      SimpleColoredBlockEntity::new,
      Streams.concat(
          MishangUtils.instanceStream(ColoredBlocks.class, Block.class),
          MishangUtils.instanceStream(HungSignBlocks.class, ColoredHungSignBarBlock.class),
          MishangUtils.instanceStream(HandrailBlocks.class, ColoredGlassHandrailBlock.class).flatMap(block -> Arrays.stream(block.selfAndVariants()))));
  public static final BlockEntityType<HungSignBlockEntity> HUNG_SIGN_BLOCK_ENTITY = register(
      "hung_sign_block_entity",
      HungSignBlockEntity::new,
      MishangUtils.instanceStream(HungSignBlocks.class, HungSignBlock.class)
          .filter(block -> !(block instanceof ColoredBlock)));

  public static final BlockEntityType<ColoredHungSignBlockEntity> COLORED_HUNG_SIGN_BLOCK_ENTITY = register(
      "colored_hung_sign_block_entity",
      ColoredHungSignBlockEntity::new,
      MishangUtils.instanceStream(HungSignBlocks.class, HungSignBlock.class).filter(block -> block instanceof ColoredBlock));

  public static final BlockEntityType<WallSignBlockEntity> WALL_SIGN_BLOCK_ENTITY = register(
      "wall_sign_block_entity",
      WallSignBlockEntity::new,
      MishangUtils.instanceStream(WallSignBlocks.class, Block.class)
          .filter(block -> !(block instanceof FullWallSignBlock || block instanceof ColoredBlock)));

  public static final BlockEntityType<FullWallSignBlockEntity> FULL_WALL_SIGN_BLOCK_ENTITY = register(
      "full_wall_sign_block_entity",
      FullWallSignBlockEntity::new,
      MishangUtils.instanceStream(WallSignBlocks.class, FullWallSignBlock.class)
          .filter(block -> !(block instanceof ColoredBlock)));

  public static final BlockEntityType<ColoredWallSignBlockEntity> COLORED_WALL_SIGN_BLOCK_ENTITY = register(
      "colored_wall_sign_block_entity",
      ColoredWallSignBlockEntity::new,
      MishangUtils.instanceStream(WallSignBlocks.class, WallSignBlock.class)
          .filter(block -> block instanceof ColoredBlock));

  public static final BlockEntityType<StandingSignBlockEntity> STANDING_SIGN_BLOCK_ENTITY = register(
      "standing_sign_block_entity",
      StandingSignBlockEntity::new,
      MishangUtils.instanceStream(StandingSignBlocks.class, StandingSignBlock.class)
          .filter(block -> !(block instanceof ColoredBlock)));

  public static final BlockEntityType<ColoredStandingSignBlockEntity> COLORED_STANDING_SIGN_BLOCK_ENTITY = register(
      "colored_standing_sign_block_entity",
      ColoredStandingSignBlockEntity::new,
      MishangUtils.instanceStream(StandingSignBlocks.class, StandingSignBlock.class)
          .filter(block -> block instanceof ColoredBlock));

  /** Registers all block entity types during the {@code RegisterEvent} for the BLOCK_ENTITY_TYPE registry. */
  public static void registerAll(RegisterEvent.RegisterHelper<BlockEntityType<?>> helper) {
    TO_REGISTER.forEach(helper::register);
  }

  private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType.BlockEntitySupplier<T> factory, Block... blocks) {
    final BlockEntityType<T> type = BlockEntityType.Builder.of(factory, blocks).build(null);
    TO_REGISTER.put(Mishanguc.id(name), type);
    return type;
  }

  private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType.BlockEntitySupplier<T> factory, Stream<? extends Block> blockStream) {
    return register(name, factory, blockStream.toArray(Block[]::new));
  }
}
