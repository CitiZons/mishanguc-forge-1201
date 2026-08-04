package pers.solid.mishang.uc;

import net.minecraft.world.level.block.state.pattern.BlockInWorld;

import pers.solid.mishang.uc.MishangUtils;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.solid.mishang.uc.block.ColoredBlock;
import pers.solid.mishang.uc.block.GlassHandrailBlock;
import pers.solid.mishang.uc.block.HandrailBlock;
import pers.solid.mishang.uc.block.Road;
import pers.solid.mishang.uc.blockentity.BlockEntityWithText;
import pers.solid.mishang.uc.data.stubs.ServerPlayNetworking;
import pers.solid.mishang.uc.item.HotbarScrollInteraction;
import pers.solid.mishang.uc.item.SlabToolItem;
import pers.solid.mishang.uc.networking.MishangucNetwork;
import pers.solid.mishang.uc.blockentity.ColoredBlockEntity;
import pers.solid.mishang.uc.blockentity.MishangucBlockEntities;
import pers.solid.mishang.uc.blocks.*;
import pers.solid.mishang.uc.item.*;
import pers.solid.mishang.uc.text.SpecialDrawableTypes;
import pers.solid.mishang.uc.util.ColorfulBlockRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.InteractionHand;

@Mod("mishanguc")
public class Mishanguc {
  public static final Logger MISHANG_LOGGER = LoggerFactory.getLogger("Mishang Urban Construction");

  // TODO: These custom events were Fabric EventFactory-based. They need to be replaced with
  // proper Forge event handling. For now, the attack block logic is handled via
  // PlayerInteractEvent.LeftClickBlock @SubscribeEvent methods below.
  // BEGIN_ATTACK_BLOCK_EVENT and PROGRESS_ATTACK_BLOCK_EVENT are removed.

  private static final @NotNull ResourceLocation EXAMPLE_ID = new ResourceLocation("mishanguc", "");

  /**
   * 创建使用本模组命名空间（{@code mishanguc}）的 ID。此方法可以提高不同版本之间的兼容性，同时在部分版本中避免命名空间的冗余校验。
   *
   * @return 使用本模组命名空间（{@code mishanguc}）的 ID。
   */
  public static @NotNull ResourceLocation id(@NotNull String path) {
    return EXAMPLE_ID.withPath(path);
  }

  /**
   * Set of wooden blocks that should be registered as fuel. Used by the FurnaceFuelBurnTimeEvent handler.
   */
  private static Set<Block> WOODEN_FUEL_BLOCKS;
  private static Set<Block> WOODEN_HANDRAIL_FUEL_BLOCKS;

  public Mishanguc() {
    final net.minecraftforge.eventbus.api.IEventBus modEventBus =
        net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();

    // 初始化游戏规则与特殊渲染类型（非 Forge 注册表，可在此直接初始化）
    final GameRules.Key<EnumRule<MishangucRules.ToolAccess>> ignore = MishangucRules.CARRYING_TOOL_ACCESS;
    SpecialDrawableTypes.init();

    // 通过 Forge 的 RegisterEvent 注册方块、物品、方块实体、创造模式物品栏。
    modEventBus.addListener(this::onRegister);

    // Register this instance for Forge game events
    MinecraftForge.EVENT_BUS.register(this);

    // Register FMLCommonSetupEvent on the mod event bus
    modEventBus.addListener(this::onCommonSetup);

    // 玩家踩在道路方块上时，予以加速。
    ColumnBuildingTool.registerTempMemoryEvents();

    registerCommands();
  }

  /**
   * Forge 在加载阶段会为每个注册表分别触发一次 {@link RegisterEvent}。方块和物品属于不同的注册表，
   * 必须分别在各自的事件中注册，否则会出现 "intrusive holders were not registered" 的错误。
   */
  private void onRegister(RegisterEvent event) {
    // 使用 RegisterHelper 注册，才能正确绑定方块/物品的 intrusive holder。
    event.register(ForgeRegistries.Keys.BLOCKS, MishangucBlocks::registerBlocks);
    event.register(ForgeRegistries.Keys.ITEMS, helper -> {
      MishangucBlocks.registerItems(helper);
      MishangucItems.registerItems(helper);
    });
    event.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, MishangucBlockEntities::registerAll);
    // 创造模式物品栏（内部会自行判断注册表是否为 CREATIVE_MODE_TABS）
    MishangucItemGroups.registerTabs(event);
  }

  private void onCommonSetup(FMLCommonSetupEvent event) {
    // 注册网络频道与服务端接收器。
    MishangucNetwork.register();
    registerNetworkingReceiver();
    event.enqueueWork(() -> {
      // 这些方法会引用（从而加载）方块类，必须在方块注册完成后执行，否则会在注册表冻结状态下创建方块。
      registerColoredBlocks();
      registerColorfulBlocks();
      registerFlammableBlocks();
    });
  }

  /**
   * 注册服务端的网络接收器（对应 Fabric 的 {@code ServerPlayNetworking.registerGlobalReceiver}）。
   */
  private static void registerNetworkingReceiver() {
    ServerPlayNetworking.registerGlobalReceiver(
        new ResourceLocation("mishanguc", "edit_sign_finish"), BlockEntityWithText.PACKET_HANDLER);
    ServerPlayNetworking.registerGlobalReceiver(
        new ResourceLocation("mishanguc", "item_scroll"), (server, player, handler, buf, responseSender) -> {
          final int selectedSlot = buf.readInt();
          final double scrollAmount = buf.readDouble();
          server.execute(() -> {
            final ItemStack stack = player.getInventory().getItem(selectedSlot);
            if (stack.getItem() instanceof HotbarScrollInteraction interaction) {
              interaction.onScroll(selectedSlot, scrollAmount, player, stack);
            }
          });
        });
    ServerPlayNetworking.registerGlobalReceiver(
        new ResourceLocation("mishanguc", "slab_tool"), SlabToolItem.Handler.INSTANCE);
  }

  private static void registerCommands() {
  }

  /**
   * Register flammable blocks using FireBlock.setFlammable (Forge equivalent of FlammableBlockRegistry).
   * Fuel registration is handled by the FurnaceFuelBurnTimeEvent handler below.
   */
  private static void registerFlammableBlocks() {
    final FireBlock fireBlock = (FireBlock) Blocks.FIRE;

    final Block[] woodenBlocks = {
        HungSignBlocks.OAK_HUNG_SIGN,
        HungSignBlocks.SPRUCE_HUNG_SIGN,
        HungSignBlocks.BIRCH_HUNG_SIGN,
        HungSignBlocks.JUNGLE_HUNG_SIGN,
        HungSignBlocks.ACACIA_HUNG_SIGN,
        HungSignBlocks.CHERRY_HUNG_SIGN,
        HungSignBlocks.DARK_OAK_HUNG_SIGN,
        HungSignBlocks.MANGROVE_HUNG_SIGN,
        HungSignBlocks.OAK_WOOD_HUNG_SIGN,
        HungSignBlocks.SPRUCE_WOOD_HUNG_SIGN,
        HungSignBlocks.BIRCH_WOOD_HUNG_SIGN,
        HungSignBlocks.JUNGLE_WOOD_HUNG_SIGN,
        HungSignBlocks.ACACIA_WOOD_HUNG_SIGN,
        HungSignBlocks.CHERRY_WOOD_HUNG_SIGN,
        HungSignBlocks.DARK_OAK_WOOD_HUNG_SIGN,
        HungSignBlocks.MANGROVE_WOOD_HUNG_SIGN,
        HungSignBlocks.STRIPPED_OAK_WOOD_HUNG_SIGN,
        HungSignBlocks.STRIPPED_SPRUCE_WOOD_HUNG_SIGN,
        HungSignBlocks.STRIPPED_BIRCH_WOOD_HUNG_SIGN,
        HungSignBlocks.STRIPPED_JUNGLE_WOOD_HUNG_SIGN,
        HungSignBlocks.STRIPPED_ACACIA_WOOD_HUNG_SIGN,
        HungSignBlocks.STRIPPED_CHERRY_WOOD_HUNG_SIGN,
        HungSignBlocks.STRIPPED_DARK_OAK_WOOD_HUNG_SIGN,
        HungSignBlocks.STRIPPED_MANGROVE_WOOD_HUNG_SIGN,
        HungSignBlocks.BAMBOO_HUNG_SIGN,
        HungSignBlocks.BAMBOO_PLANK_HUNG_SIGN,
        HungSignBlocks.BAMBOO_MOSAIC_HUNG_SIGN,
        HungSignBlocks.OAK_HUNG_SIGN_BAR,
        HungSignBlocks.SPRUCE_HUNG_SIGN_BAR,
        HungSignBlocks.BIRCH_HUNG_SIGN_BAR,
        HungSignBlocks.JUNGLE_HUNG_SIGN_BAR,
        HungSignBlocks.ACACIA_HUNG_SIGN_BAR,
        HungSignBlocks.CHERRY_HUNG_SIGN_BAR,
        HungSignBlocks.DARK_OAK_HUNG_SIGN_BAR,
        HungSignBlocks.MANGROVE_HUNG_SIGN_BAR,
        HungSignBlocks.BAMBOO_HUNG_SIGN_BAR,
        HungSignBlocks.STRIPPED_OAK_HUNG_SIGN_BAR,
        HungSignBlocks.STRIPPED_SPRUCE_HUNG_SIGN_BAR,
        HungSignBlocks.STRIPPED_BIRCH_HUNG_SIGN_BAR,
        HungSignBlocks.STRIPPED_JUNGLE_HUNG_SIGN_BAR,
        HungSignBlocks.STRIPPED_ACACIA_HUNG_SIGN_BAR,
        HungSignBlocks.STRIPPED_CHERRY_HUNG_SIGN_BAR,
        HungSignBlocks.STRIPPED_DARK_OAK_HUNG_SIGN_BAR,
        HungSignBlocks.STRIPPED_MANGROVE_HUNG_SIGN_BAR,
        WallSignBlocks.OAK_WOOD_WALL_SIGN,
        WallSignBlocks.SPRUCE_WOOD_WALL_SIGN,
        WallSignBlocks.BIRCH_WOOD_WALL_SIGN,
        WallSignBlocks.JUNGLE_WOOD_WALL_SIGN,
        WallSignBlocks.ACACIA_WOOD_WALL_SIGN,
        WallSignBlocks.CHERRY_WOOD_WALL_SIGN,
        WallSignBlocks.DARK_OAK_WOOD_WALL_SIGN,
        WallSignBlocks.MANGROVE_WOOD_WALL_SIGN,
        WallSignBlocks.STRIPPED_OAK_WOOD_WALL_SIGN,
        WallSignBlocks.STRIPPED_SPRUCE_WOOD_WALL_SIGN,
        WallSignBlocks.STRIPPED_BIRCH_WOOD_WALL_SIGN,
        WallSignBlocks.STRIPPED_JUNGLE_WOOD_WALL_SIGN,
        WallSignBlocks.STRIPPED_ACACIA_WOOD_WALL_SIGN,
        WallSignBlocks.STRIPPED_CHERRY_WOOD_WALL_SIGN,
        WallSignBlocks.STRIPPED_DARK_OAK_WOOD_WALL_SIGN,
        WallSignBlocks.STRIPPED_MANGROVE_WOOD_WALL_SIGN,
        WallSignBlocks.OAK_WALL_SIGN,
        WallSignBlocks.SPRUCE_WALL_SIGN,
        WallSignBlocks.BIRCH_WALL_SIGN,
        WallSignBlocks.JUNGLE_WALL_SIGN,
        WallSignBlocks.ACACIA_WALL_SIGN,
        WallSignBlocks.CHERRY_WALL_SIGN,
        WallSignBlocks.DARK_OAK_WALL_SIGN,
        WallSignBlocks.MANGROVE_WALL_SIGN,
        WallSignBlocks.OAK_WOOD_WALL_SIGN,
        WallSignBlocks.SPRUCE_WOOD_WALL_SIGN,
        WallSignBlocks.BIRCH_WOOD_WALL_SIGN,
        WallSignBlocks.JUNGLE_WOOD_WALL_SIGN,
        WallSignBlocks.ACACIA_WOOD_WALL_SIGN,
        WallSignBlocks.CHERRY_WOOD_WALL_SIGN,
        WallSignBlocks.DARK_OAK_WOOD_WALL_SIGN,
        WallSignBlocks.MANGROVE_WOOD_WALL_SIGN,
        WallSignBlocks.STRIPPED_OAK_WOOD_WALL_SIGN,
        WallSignBlocks.STRIPPED_SPRUCE_WOOD_WALL_SIGN,
        WallSignBlocks.STRIPPED_BIRCH_WOOD_WALL_SIGN,
        WallSignBlocks.STRIPPED_JUNGLE_WOOD_WALL_SIGN,
        WallSignBlocks.STRIPPED_ACACIA_WOOD_WALL_SIGN,
        WallSignBlocks.STRIPPED_CHERRY_WOOD_WALL_SIGN,
        WallSignBlocks.STRIPPED_DARK_OAK_WOOD_WALL_SIGN,
        WallSignBlocks.STRIPPED_MANGROVE_WOOD_WALL_SIGN,
        WallSignBlocks.BAMBOO_WALL_SIGN,
        WallSignBlocks.BAMBOO_PLANK_WALL_SIGN,
        WallSignBlocks.BAMBOO_MOSAIC_WALL_SIGN,
        WallSignBlocks.COLORED_WOODEN_WALL_SIGN,
        StandingSignBlocks.OAK_STANDING_SIGN,
        StandingSignBlocks.SPRUCE_STANDING_SIGN,
        StandingSignBlocks.BIRCH_STANDING_SIGN,
        StandingSignBlocks.ACACIA_STANDING_SIGN,
        StandingSignBlocks.CHERRY_STANDING_SIGN,
        StandingSignBlocks.JUNGLE_STANDING_SIGN,
        StandingSignBlocks.DARK_OAK_STANDING_SIGN,
        StandingSignBlocks.MANGROVE_STANDING_SIGN,
        StandingSignBlocks.OAK_WOOD_STANDING_SIGN,
        StandingSignBlocks.SPRUCE_WOOD_STANDING_SIGN,
        StandingSignBlocks.BIRCH_WOOD_STANDING_SIGN,
        StandingSignBlocks.ACACIA_WOOD_STANDING_SIGN,
        StandingSignBlocks.CHERRY_WOOD_STANDING_SIGN,
        StandingSignBlocks.JUNGLE_WOOD_STANDING_SIGN,
        StandingSignBlocks.DARK_OAK_WOOD_STANDING_SIGN,
        StandingSignBlocks.MANGROVE_WOOD_STANDING_SIGN,
        StandingSignBlocks.STRIPPED_OAK_WOOD_STANDING_SIGN,
        StandingSignBlocks.STRIPPED_SPRUCE_WOOD_STANDING_SIGN,
        StandingSignBlocks.STRIPPED_BIRCH_WOOD_STANDING_SIGN,
        StandingSignBlocks.STRIPPED_ACACIA_WOOD_STANDING_SIGN,
        StandingSignBlocks.STRIPPED_CHERRY_WOOD_STANDING_SIGN,
        StandingSignBlocks.STRIPPED_JUNGLE_WOOD_STANDING_SIGN,
        StandingSignBlocks.STRIPPED_DARK_OAK_WOOD_STANDING_SIGN,
        StandingSignBlocks.STRIPPED_MANGROVE_WOOD_STANDING_SIGN,
        StandingSignBlocks.BAMBOO_STANDING_SIGN,
        StandingSignBlocks.BAMBOO_PLANK_STANDING_SIGN,
        StandingSignBlocks.BAMBOO_MOSAIC_STANDING_SIGN
    };

    // Build the set for fuel registration
    ImmutableSet.Builder<Block> woodenFuelBuilder = ImmutableSet.builder();

    for (Block block : woodenBlocks) {
      fireBlock.setFlammable(block, 5, 20);
      woodenFuelBuilder.add(block);
    }
    WOODEN_FUEL_BLOCKS = woodenFuelBuilder.build();

    final Collection<HandrailBlock> woodenHandrails = ImmutableSet.of(
        HandrailBlocks.SIMPLE_OAK_HANDRAIL,
        HandrailBlocks.SIMPLE_SPRUCE_HANDRAIL,
        HandrailBlocks.SIMPLE_BIRCH_HANDRAIL,
        HandrailBlocks.SIMPLE_JUNGLE_HANDRAIL,
        HandrailBlocks.SIMPLE_ACACIA_HANDRAIL,
        HandrailBlocks.SIMPLE_CHERRY_HANDRAIL,
        HandrailBlocks.SIMPLE_DARK_OAK_HANDRAIL,
        HandrailBlocks.SIMPLE_MANGROVE_HANDRAIL,
        HandrailBlocks.SIMPLE_OAK_PLANK_HANDRAIL,
        HandrailBlocks.SIMPLE_SPRUCE_PLANK_HANDRAIL,
        HandrailBlocks.SIMPLE_BIRCH_PLANK_HANDRAIL,
        HandrailBlocks.SIMPLE_JUNGLE_PLANK_HANDRAIL,
        HandrailBlocks.SIMPLE_ACACIA_PLANK_HANDRAIL,
        HandrailBlocks.SIMPLE_CHERRY_PLANK_HANDRAIL,
        HandrailBlocks.SIMPLE_DARK_OAK_PLANK_HANDRAIL,
        HandrailBlocks.SIMPLE_MANGROVE_PLANK_HANDRAIL,
        HandrailBlocks.SIMPLE_BAMBOO_HANDRAIL,
        HandrailBlocks.SIMPLE_BAMBOO_PLANK_HANDRAIL,
        HandrailBlocks.SIMPLE_BAMBOO_MOSAIC_HANDRAIL,
        HandrailBlocks.GLASS_OAK_HANDRAIL,
        HandrailBlocks.GLASS_SPRUCE_HANDRAIL,
        HandrailBlocks.GLASS_BIRCH_HANDRAIL,
        HandrailBlocks.GLASS_JUNGLE_HANDRAIL,
        HandrailBlocks.GLASS_ACACIA_HANDRAIL,
        HandrailBlocks.GLASS_CHERRY_HANDRAIL,
        HandrailBlocks.GLASS_DARK_OAK_HANDRAIL,
        HandrailBlocks.GLASS_MANGROVE_HANDRAIL,
        HandrailBlocks.GLASS_BAMBOO_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_OAK_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_SPRUCE_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_BIRCH_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_JUNGLE_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_ACACIA_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_CHERRY_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_DARK_OAK_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_MANGROVE_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_BAMBOO_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_STRIPPED_OAK_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_STRIPPED_SPRUCE_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_STRIPPED_BIRCH_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_STRIPPED_JUNGLE_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_STRIPPED_ACACIA_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_STRIPPED_CHERRY_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_STRIPPED_DARK_OAK_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_STRIPPED_MANGROVE_HANDRAIL,
        HandrailBlocks.COLORED_DECORATED_STRIPPED_BAMBOO_HANDRAIL
    );

    ImmutableSet.Builder<Block> handrailFuelBuilder = ImmutableSet.builder();
    for (HandrailBlock handrail : woodenHandrails) {
      fireBlock.setFlammable(handrail, 5, 20);
      fireBlock.setFlammable(handrail.central(), 5, 20);
      fireBlock.setFlammable(handrail.corner(), 5, 20);
      fireBlock.setFlammable(handrail.outer(), 5, 20);
      fireBlock.setFlammable(handrail.stair(), 5, 20);
      handrailFuelBuilder.add(handrail);
      handrailFuelBuilder.add(handrail.central());
      handrailFuelBuilder.add(handrail.corner());
      handrailFuelBuilder.add(handrail.outer());
      handrailFuelBuilder.add(handrail.stair());
    }
    WOODEN_HANDRAIL_FUEL_BLOCKS = handrailFuelBuilder.build();

    fireBlock.setFlammable(ColoredBlocks.COLORED_PLANKS, 5, 20);
    fireBlock.setFlammable(ColoredBlocks.COLORED_PLANK_STAIRS, 5, 20);
    fireBlock.setFlammable(ColoredBlocks.COLORED_PLANK_SLAB, 5, 20);
    fireBlock.setFlammable(ColoredBlocks.COLORED_WOOL, 30, 60);
  }

  /**
   * Forge equivalent of FuelRegistry. Handles fuel burn time for wooden blocks.
   */
  @SubscribeEvent
  public void onFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
    final Item item = event.getItemStack().getItem();
    final Block block = Block.byItem(item);

    if (WOODEN_FUEL_BLOCKS != null && WOODEN_FUEL_BLOCKS.contains(block)) {
      event.setBurnTime(100);
      return;
    }
    if (WOODEN_HANDRAIL_FUEL_BLOCKS != null && WOODEN_HANDRAIL_FUEL_BLOCKS.contains(block)) {
      event.setBurnTime(100);
      return;
    }
    if (block == ColoredBlocks.COLORED_PLANKS) {
      event.setBurnTime(300);
    } else if (block == ColoredBlocks.COLORED_PLANK_STAIRS) {
      event.setBurnTime(300);
    } else if (block == ColoredBlocks.COLORED_PLANK_SLAB) {
      event.setBurnTime(150);
    } else if (block == ColoredBlocks.COLORED_WOOL) {
      event.setBurnTime(100);
    }
  }

  // TODO: Networking registration needs Forge SimpleChannel conversion.
  // ServerPlayNetworking.registerGlobalReceiver calls should be replaced with
  // Forge networking (SimpleChannel/NetworkChannel).
  // For now, the player login sync is handled via @SubscribeEvent below.

  @SubscribeEvent
  public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      final GameRules gameRules = serverPlayer.getServer().getGameRules();
      MishangucRules.sync(gameRules.getRule(MishangucRules.FORCE_PLACING_TOOL_ACCESS), (short) 0, serverPlayer);
      MishangucRules.sync(gameRules.getRule(MishangucRules.CARRYING_TOOL_ACCESS), (short) 1, serverPlayer);
    }
  }

  // ---- Forge event handlers replacing Fabric callbacks ----

  /**
   * Replaces AttackBlockCallback.EVENT.register (server-side attack block).
   * Also handles BEGIN_ATTACK_BLOCK_EVENT logic for minecraft-side.
   */
  @SubscribeEvent
  public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
    final var player = event.getEntity();
    final var world = event.getLevel();
    final var hand = event.getHand();
    final var pos = event.getPos();
    final var direction = event.getFace();

    if (player.isSpectator()) return;

    final ItemStack stack = player.getItemInHand(hand);
    final Item item = stack.getItem();

    if (item instanceof final BlockToolItem blockToolItem) {
      InteractionResult result = blockToolItem.beginAttackBlock(stack, player, world, hand, pos, direction, blockToolItem.includesFluid(stack, player.isShiftKeyDown()));
      if (result != InteractionResult.PASS) {
        event.setCanceled(true);
      }
    }
  }

  /**
   * Replaces UseBlockCallback.EVENT.register (use block / right-click block).
   */
  @SubscribeEvent
  public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
    final var player = event.getEntity();
    final var world = event.getLevel();
    final var hand = event.getHand();
    final var hitResult = event.getHitVec();

    if (player.isSpectator()) return;

    final ItemStack stackInHand = player.getItemInHand(hand);
    final Item item = stackInHand.getItem();

    // BlockToolItem use on block
    if (!player.getAbilities().mayBuild && !stackInHand.hasAdventureModePlaceTagForBlock(BuiltInRegistries.BLOCK, new BlockInWorld(world, hitResult.getBlockPos(), false))) {
      // pass
    } else if (item instanceof final BlockToolItem blockToolItem) {
      InteractionResult result = blockToolItem.useOnBlock(stackInHand, player, world, hitResult, hand, blockToolItem.includesFluid(stackInHand, player.isShiftKeyDown()));
      if (result != InteractionResult.PASS) {
        event.setCanceled(true);
        return;
      }
    }

    // Water cauldron road cleaning
    final BlockPos blockPos = hitResult.getBlockPos();
    final BlockState blockState = world.getBlockState(blockPos);
    if (blockState.is(Blocks.WATER_CAULDRON)) {
      InteractionResult result = Road.CLEAN_ROAD_BLOCK.interact(blockState, world, blockPos, player, hand, stackInHand);
      if (result != InteractionResult.PASS) {
        event.setCanceled(true);
        return;
      }
    }

    // Colored block entity dyeing
    if (hitResult.getType() == HitResult.Type.BLOCK) {
      final BlockEntity blockEntity = world.getBlockEntity(blockPos);
      if (blockEntity instanceof ColoredBlockEntity coloredBlockEntity) {
        for (Map.Entry<DyeColor, TagKey<Item>> entry : MishangUtils.DYE_ITEM_TAGS.get().entrySet()) {
          final ItemStack stack = player.getItemInHand(hand);
          if (stack.is(entry.getValue())) {
            coloredBlockEntity.setColor(entry.getKey().getFireworkColor());
            blockEntity.setChanged();
            world.sendBlockUpdated(blockPos, blockEntity.getBlockState(), blockEntity.getBlockState(), Block.UPDATE_CLIENTS);
            if (!player.getAbilities().instabuild) {
              stack.shrink(1);
            }
            world.playSound(null, blockPos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            event.setCanceled(true);
            return;
          }
        }
      }
    }
  }

  /**
   * Replaces AttackEntityCallback.EVENT.register.
   */
  @SubscribeEvent
  public void onAttackEntity(AttackEntityEvent event) {
    final var player = event.getEntity();
    final var world = player.level();
    if (player.isSpectator()) return;

    // AttackEntityEvent does not provide hand or hitResult directly;
    // we use main hand as Fabric's AttackEntityCallback did.
    final var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
    final var entity = event.getTarget();
    final ItemStack stackInHand = player.getItemInHand(hand);
    final Item item = stackInHand.getItem();
    if (item instanceof final InteractsWithEntity interactsWithEntity) {
      InteractionResult result = interactsWithEntity.attackEntityCallback(player, world, hand, entity, null);
      if (result != InteractionResult.PASS) {
        event.setCanceled(true);
      }
    }
  }

  /**
   * Replaces UseEntityCallback.EVENT.register.
   */
  @SubscribeEvent
  public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
    final var player = event.getEntity();
    final var world = event.getLevel();
    final var hand = event.getHand();
    final var entity = event.getTarget();

    if (player.isSpectator()) return;

    final ItemStack stackInHand = player.getItemInHand(hand);
    final Item item = stackInHand.getItem();
    if (item instanceof final InteractsWithEntity interactsWithEntity) {
      InteractionResult result = interactsWithEntity.useEntityCallback(player, world, hand, entity, null);
      if (result != InteractionResult.PASS) {
        event.setCanceled(true);
      }
    }
  }

  private static void registerColoredBlocks() {
    final Object2ObjectMap<Block, Block> blockMap = ColoredBlock.BASE_TO_COLORED;
    final Object2ObjectMap<TagKey<Block>, Block> tagMap = ColoredBlock.BASE_TAG_TO_COLORED;
    tagMap.put(BlockTags.WOOL, ColoredBlocks.COLORED_WOOL);
    tagMap.put(BlockTags.TERRACOTTA, ColoredBlocks.COLORED_TERRACOTTA);
    blockMap.put(Blocks.WHITE_CONCRETE, ColoredBlocks.COLORED_CONCRETE);
    blockMap.put(Blocks.ORANGE_CONCRETE, ColoredBlocks.COLORED_CONCRETE);
    blockMap.put(Blocks.MAGENTA_CONCRETE, ColoredBlocks.COLORED_CONCRETE);
    blockMap.put(Blocks.LIGHT_BLUE_CONCRETE, ColoredBlocks.COLORED_CONCRETE);
    blockMap.put(Blocks.YELLOW_CONCRETE, ColoredBlocks.COLORED_CONCRETE);
    blockMap.put(Blocks.LIME_CONCRETE, ColoredBlocks.COLORED_CONCRETE);
    blockMap.put(Blocks.PINK_CONCRETE, ColoredBlocks.COLORED_CONCRETE);
    blockMap.put(Blocks.GRAY_CONCRETE, ColoredBlocks.COLORED_CONCRETE);
    blockMap.put(Blocks.LIGHT_GRAY_CONCRETE, ColoredBlocks.COLORED_CONCRETE);
    blockMap.put(Blocks.CYAN_CONCRETE, ColoredBlocks.COLORED_CONCRETE);
    blockMap.put(Blocks.PURPLE_CONCRETE, ColoredBlocks.COLORED_CONCRETE);
    blockMap.put(Blocks.BLUE_CONCRETE, ColoredBlocks.COLORED_CONCRETE);
    blockMap.put(Blocks.BROWN_CONCRETE, ColoredBlocks.COLORED_CONCRETE);
    blockMap.put(Blocks.GREEN_CONCRETE, ColoredBlocks.COLORED_CONCRETE);
    blockMap.put(Blocks.RED_CONCRETE, ColoredBlocks.COLORED_CONCRETE);
    blockMap.put(Blocks.BLACK_CONCRETE, ColoredBlocks.COLORED_CONCRETE);

    // 下面的木制方块不含任何下界方块和竹制方块
    List.of(Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.JUNGLE_PLANKS, Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.MANGROVE_PLANKS, Blocks.CHERRY_PLANKS).forEach(block -> blockMap.put(block, ColoredBlocks.COLORED_PLANKS));
    List.of(Blocks.OAK_STAIRS, Blocks.SPRUCE_STAIRS, Blocks.BIRCH_STAIRS, Blocks.JUNGLE_STAIRS, Blocks.ACACIA_STAIRS, Blocks.DARK_OAK_STAIRS, Blocks.MANGROVE_STAIRS, Blocks.CHERRY_STAIRS).forEach(block -> blockMap.put(block, ColoredBlocks.COLORED_PLANK_STAIRS));
    List.of(Blocks.OAK_SLAB, Blocks.SPRUCE_SLAB, Blocks.BIRCH_SLAB, Blocks.JUNGLE_SLAB, Blocks.ACACIA_SLAB, Blocks.DARK_OAK_SLAB, Blocks.MANGROVE_SLAB, Blocks.CHERRY_SLAB).forEach(block -> blockMap.put(block, ColoredBlocks.COLORED_PLANK_SLAB));

    blockMap.put(Blocks.DIRT, ColoredBlocks.COLORED_DIRT);
    blockMap.put(Blocks.COBBLESTONE, ColoredBlocks.COLORED_COBBLESTONE);
    blockMap.put(Blocks.COBBLESTONE_STAIRS, ColoredBlocks.COLORED_COBBLESTONE_STAIRS);
    blockMap.put(Blocks.COBBLESTONE_SLAB, ColoredBlocks.COLORED_COBBLESTONE_SLAB);
    blockMap.put(Blocks.ANDESITE, ColoredBlocks.COLORED_ANDESITE);
    blockMap.put(Blocks.ANDESITE_STAIRS, ColoredBlocks.COLORED_ANDESITE_STAIRS);
    blockMap.put(Blocks.ANDESITE_SLAB, ColoredBlocks.COLORED_ANDESITE_SLAB);
    blockMap.put(Blocks.DIORITE, ColoredBlocks.COLORED_DIORITE);
    blockMap.put(Blocks.DIORITE_STAIRS, ColoredBlocks.COLORED_DIORITE_STAIRS);
    blockMap.put(Blocks.DIORITE_SLAB, ColoredBlocks.COLORED_DIORITE_SLAB);
    blockMap.put(Blocks.CALCITE, ColoredBlocks.COLORED_CALCITE);
    blockMap.put(Blocks.TUFF, ColoredBlocks.COLORED_TUFF);
    blockMap.put(Blocks.IRON_BLOCK, ColoredBlocks.COLORED_IRON_BLOCK);
    blockMap.put(Blocks.STONE, ColoredBlocks.COLORED_STONE);
    blockMap.put(Blocks.STONE_STAIRS, ColoredBlocks.COLORED_STONE_STAIRS);
    blockMap.put(Blocks.STONE_SLAB, ColoredBlocks.COLORED_STONE_SLAB);
    blockMap.put(Blocks.STONE_BRICKS, ColoredBlocks.COLORED_STONE_BRICKS);
    blockMap.put(Blocks.STONE_BRICK_STAIRS, ColoredBlocks.COLORED_STONE_BRICK_STAIRS);
    blockMap.put(Blocks.STONE_BRICK_SLAB, ColoredBlocks.COLORED_STONE_BRICK_SLAB);
    blockMap.put(Blocks.QUARTZ_BLOCK, ColoredBlocks.COLORED_QUARTZ_BLOCK);
    blockMap.put(Blocks.QUARTZ_STAIRS, ColoredBlocks.COLORED_QUARTZ_STAIRS);
    blockMap.put(Blocks.QUARTZ_SLAB, ColoredBlocks.COLORED_QUARTZ_SLAB);
    blockMap.put(Blocks.CHISELED_QUARTZ_BLOCK, ColoredBlocks.COLORED_CHISELED_QUARTZ_BLOCK);
    blockMap.put(Blocks.QUARTZ_BRICKS, ColoredBlocks.COLORED_QUARTZ_BRICKS);
    blockMap.put(Blocks.SMOOTH_QUARTZ, ColoredBlocks.COLORED_SMOOTH_QUARTZ);
    blockMap.put(Blocks.SMOOTH_QUARTZ_STAIRS, ColoredBlocks.COLORED_SMOOTH_QUARTZ_STAIRS);
    blockMap.put(Blocks.SMOOTH_QUARTZ_SLAB, ColoredBlocks.COLORED_SMOOTH_QUARTZ_SLAB);
    blockMap.put(Blocks.QUARTZ_PILLAR, ColoredBlocks.COLORED_QUARTZ_PILLAR);
    blockMap.put(Blocks.PURPUR_BLOCK, ColoredBlocks.COLORED_PURPUR_BLOCK);
    blockMap.put(Blocks.PURPUR_PILLAR, ColoredBlocks.COLORED_PURPUR_PILLAR);
    blockMap.put(Blocks.END_STONE, ColoredBlocks.COLORED_END_STONE);
    blockMap.put(Blocks.END_STONE_BRICKS, ColoredBlocks.COLORED_END_STONE_BRICKS);
    blockMap.put(Blocks.END_STONE_BRICK_STAIRS, ColoredBlocks.COLORED_END_STONE_BRICK_STAIRS);
    blockMap.put(Blocks.END_STONE_BRICK_SLAB, ColoredBlocks.COLORED_END_STONE_BRICK_SLAB);
    blockMap.put(Blocks.NETHER_PORTAL, ColoredBlocks.COLORED_NETHER_PORTAL);
    blockMap.put(LightBlocks.WHITE_LIGHT, ColoredBlocks.COLORED_LIGHT);
    blockMap.put(LightBlocks.YELLOW_LIGHT, ColoredBlocks.COLORED_LIGHT);
    blockMap.put(LightBlocks.CYAN_LIGHT, ColoredBlocks.COLORED_LIGHT);
    blockMap.put(Blocks.GLASS, ColoredBlocks.COLORED_GLASS);
    blockMap.put(Blocks.WHITE_STAINED_GLASS, ColoredBlocks.COLORED_GLASS);
    blockMap.put(Blocks.ORANGE_STAINED_GLASS, ColoredBlocks.COLORED_GLASS);
    blockMap.put(Blocks.MAGENTA_STAINED_GLASS, ColoredBlocks.COLORED_GLASS);
    blockMap.put(Blocks.LIGHT_BLUE_STAINED_GLASS, ColoredBlocks.COLORED_GLASS);
    blockMap.put(Blocks.YELLOW_STAINED_GLASS, ColoredBlocks.COLORED_GLASS);
    blockMap.put(Blocks.LIME_STAINED_GLASS, ColoredBlocks.COLORED_GLASS);
    blockMap.put(Blocks.PINK_STAINED_GLASS, ColoredBlocks.COLORED_GLASS);
    blockMap.put(Blocks.GRAY_STAINED_GLASS, ColoredBlocks.COLORED_GLASS);
    blockMap.put(Blocks.LIGHT_GRAY_STAINED_GLASS, ColoredBlocks.COLORED_GLASS);
    blockMap.put(Blocks.CYAN_STAINED_GLASS, ColoredBlocks.COLORED_GLASS);
    blockMap.put(Blocks.PURPLE_STAINED_GLASS, ColoredBlocks.COLORED_GLASS);
    blockMap.put(Blocks.BLUE_STAINED_GLASS, ColoredBlocks.COLORED_GLASS);
    blockMap.put(Blocks.BROWN_STAINED_GLASS, ColoredBlocks.COLORED_GLASS);
    blockMap.put(Blocks.GREEN_STAINED_GLASS, ColoredBlocks.COLORED_GLASS);
    blockMap.put(Blocks.RED_STAINED_GLASS, ColoredBlocks.COLORED_GLASS);
    blockMap.put(Blocks.BLACK_STAINED_GLASS, ColoredBlocks.COLORED_GLASS);
    blockMap.put(Blocks.GLASS_PANE, ColoredBlocks.COLORED_GLASS_PANE);
    blockMap.put(Blocks.WHITE_STAINED_GLASS_PANE, ColoredBlocks.COLORED_GLASS_PANE);
    blockMap.put(Blocks.ORANGE_STAINED_GLASS_PANE, ColoredBlocks.COLORED_GLASS_PANE);
    blockMap.put(Blocks.MAGENTA_STAINED_GLASS_PANE, ColoredBlocks.COLORED_GLASS_PANE);
    blockMap.put(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, ColoredBlocks.COLORED_GLASS_PANE);
    blockMap.put(Blocks.YELLOW_STAINED_GLASS_PANE, ColoredBlocks.COLORED_GLASS_PANE);
    blockMap.put(Blocks.LIME_STAINED_GLASS_PANE, ColoredBlocks.COLORED_GLASS_PANE);
    blockMap.put(Blocks.PINK_STAINED_GLASS_PANE, ColoredBlocks.COLORED_GLASS_PANE);
    blockMap.put(Blocks.GRAY_STAINED_GLASS_PANE, ColoredBlocks.COLORED_GLASS_PANE);
    blockMap.put(Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, ColoredBlocks.COLORED_GLASS_PANE);
    blockMap.put(Blocks.CYAN_STAINED_GLASS_PANE, ColoredBlocks.COLORED_GLASS_PANE);
    blockMap.put(Blocks.PURPLE_STAINED_GLASS_PANE, ColoredBlocks.COLORED_GLASS_PANE);
    blockMap.put(Blocks.BLUE_STAINED_GLASS_PANE, ColoredBlocks.COLORED_GLASS_PANE);
    blockMap.put(Blocks.BROWN_STAINED_GLASS_PANE, ColoredBlocks.COLORED_GLASS_PANE);
    blockMap.put(Blocks.GREEN_STAINED_GLASS_PANE, ColoredBlocks.COLORED_GLASS_PANE);
    blockMap.put(Blocks.RED_STAINED_GLASS_PANE, ColoredBlocks.COLORED_GLASS_PANE);
    blockMap.put(Blocks.BLACK_STAINED_GLASS_PANE, ColoredBlocks.COLORED_GLASS_PANE);
    blockMap.put(Blocks.ICE, ColoredBlocks.COLORED_ICE);
    blockMap.put(Blocks.SNOW_BLOCK, ColoredBlocks.COLORED_SNOW_BLOCK);
    blockMap.put(Blocks.PACKED_ICE, ColoredBlocks.COLORED_PACKED_ICE);
    blockMap.put(Blocks.OAK_LEAVES, ColoredBlocks.COLORED_OAK_LEAVES);
    blockMap.put(Blocks.DARK_OAK_LEAVES, ColoredBlocks.COLORED_DARK_OAK_LEAVES);
    blockMap.put(Blocks.SPRUCE_LEAVES, ColoredBlocks.COLORED_SPRUCE_LEAVES);
    blockMap.put(Blocks.JUNGLE_LEAVES, ColoredBlocks.COLORED_JUNGLE_LEAVES);
    blockMap.put(Blocks.BIRCH_LEAVES, ColoredBlocks.COLORED_BIRCH_LEAVES);
    blockMap.put(Blocks.ACACIA_LEAVES, ColoredBlocks.COLORED_ACACIA_LEAVES);
    blockMap.put(Blocks.CHERRY_LEAVES, ColoredBlocks.COLORED_CHERRY_LEAVES);
    blockMap.put(Blocks.MANGROVE_LEAVES, ColoredBlocks.COLORED_MANGROVE_LEAVES);
    blockMap.put(Blocks.BRICKS, ColoredBlocks.COLORED_BRICKS);
    blockMap.put(Blocks.BRICK_STAIRS, ColoredBlocks.COLORED_BRICK_STAIRS);
    blockMap.put(Blocks.BRICK_SLAB, ColoredBlocks.COLORED_BRICK_SLAB);
    for (GlassHandrailBlock block : HandrailBlocks.DECORATED_IRON_HANDRAILS.values()) {
      blockMap.put(block, HandrailBlocks.COLORED_DECORATED_IRON_HANDRAIL);
      blockMap.put(block.corner(), HandrailBlocks.COLORED_DECORATED_IRON_HANDRAIL.corner());
      blockMap.put(block.central(), HandrailBlocks.COLORED_DECORATED_IRON_HANDRAIL.central());
      blockMap.put(block.outer(), HandrailBlocks.COLORED_DECORATED_IRON_HANDRAIL.outer());
      blockMap.put(block.stair(), HandrailBlocks.COLORED_DECORATED_IRON_HANDRAIL.stair());
    }

    tagMap.put(TagKey.create(Registries.BLOCK, id("concrete_hung_signs")), HungSignBlocks.COLORED_CONCRETE_HUNG_SIGN);
    tagMap.put(TagKey.create(Registries.BLOCK, id("glowing_concrete_hung_signs")), HungSignBlocks.COLORED_GLOWING_CONCRETE_HUNG_SIGN);
    tagMap.put(TagKey.create(Registries.BLOCK, id("concrete_hung_sign_bars")), HungSignBlocks.COLORED_CONCRETE_HUNG_SIGN_BAR);
    tagMap.put(TagKey.create(Registries.BLOCK, id("terracotta_hung_signs")), HungSignBlocks.COLORED_TERRACOTTA_HUNG_SIGN);
    tagMap.put(TagKey.create(Registries.BLOCK, id("glowing_terracotta_hung_signs")), HungSignBlocks.COLORED_GLOWING_TERRACOTTA_HUNG_SIGN);
    tagMap.put(TagKey.create(Registries.BLOCK, id("terracotta_hung_sign_bars")), HungSignBlocks.COLORED_TERRACOTTA_HUNG_SIGN_BAR);
    tagMap.put(TagKey.create(Registries.BLOCK, id("concrete_standing_signs")), StandingSignBlocks.COLORED_CONCRETE_STANDING_SIGN);
    tagMap.put(TagKey.create(Registries.BLOCK, id("terracotta_standing_signs")), StandingSignBlocks.COLORED_TERRACOTTA_STANDING_SIGN);
    tagMap.put(TagKey.create(Registries.BLOCK, id("glowing_concrete_standing_signs")), StandingSignBlocks.COLORED_GLOWING_CONCRETE_STANDING_SIGN);
    tagMap.put(TagKey.create(Registries.BLOCK, id("glowing_terracotta_standing_signs")), StandingSignBlocks.COLORED_GLOWING_TERRACOTTA_STANDING_SIGN);

    blockMap.put(HungSignBlocks.STONE_HUNG_SIGN, HungSignBlocks.COLORED_STONE_HUNG_SIGN);
    blockMap.put(HungSignBlocks.GLOWING_STONE_HUNG_SIGN, HungSignBlocks.COLORED_GLOWING_STONE_HUNG_SIGN);
    blockMap.put(HungSignBlocks.STONE_HUNG_SIGN_BAR, HungSignBlocks.COLORED_STONE_HUNG_SIGN_BAR);
    blockMap.put(HungSignBlocks.COBBLESTONE_HUNG_SIGN, HungSignBlocks.COLORED_COBBLESTONE_HUNG_SIGN);
    blockMap.put(HungSignBlocks.GLOWING_COBBLESTONE_HUNG_SIGN, HungSignBlocks.COLORED_GLOWING_COBBLESTONE_HUNG_SIGN);
    blockMap.put(HungSignBlocks.COBBLESTONE_HUNG_SIGN_BAR, HungSignBlocks.COLORED_COBBLESTONE_HUNG_SIGN_BAR);
    blockMap.put(HungSignBlocks.STONE_BRICK_HUNG_SIGN, HungSignBlocks.COLORED_STONE_BRICK_HUNG_SIGN);
    blockMap.put(HungSignBlocks.GLOWING_STONE_BRICK_HUNG_SIGN, HungSignBlocks.COLORED_GLOWING_STONE_BRICK_HUNG_SIGN);
    blockMap.put(HungSignBlocks.STONE_BRICK_HUNG_SIGN_BAR, HungSignBlocks.COLORED_STONE_BRICK_HUNG_SIGN_BAR);
    blockMap.put(HungSignBlocks.IRON_HUNG_SIGN, HungSignBlocks.COLORED_IRON_HUNG_SIGN);
    blockMap.put(HungSignBlocks.GLOWING_IRON_HUNG_SIGN, HungSignBlocks.COLORED_GLOWING_IRON_HUNG_SIGN);
    blockMap.put(HungSignBlocks.IRON_HUNG_SIGN_BAR, HungSignBlocks.COLORED_IRON_HUNG_SIGN_BAR);

    List.of(WallSignBlocks.OAK_WALL_SIGN, WallSignBlocks.SPRUCE_WALL_SIGN, WallSignBlocks.BIRCH_WALL_SIGN, WallSignBlocks.JUNGLE_WALL_SIGN, WallSignBlocks.ACACIA_WALL_SIGN, WallSignBlocks.CHERRY_WALL_SIGN, WallSignBlocks.DARK_OAK_WALL_SIGN, WallSignBlocks.MANGROVE_WALL_SIGN).forEach(b -> blockMap.put(b, WallSignBlocks.COLORED_WOODEN_WALL_SIGN));
    tagMap.put(TagKey.create(Registries.BLOCK, id("concrete_wall_signs")), WallSignBlocks.COLORED_CONCRETE_WALL_SIGN);
    tagMap.put(TagKey.create(Registries.BLOCK, id("terracotta_wall_signs")), WallSignBlocks.COLORED_TERRACOTTA_WALL_SIGN);
    tagMap.put(TagKey.create(Registries.BLOCK, id("glowing_concrete_wall_signs")), WallSignBlocks.COLORED_GLOWING_CONCRETE_WALL_SIGN);
    tagMap.put(TagKey.create(Registries.BLOCK, id("glowing_terracotta_wall_signs")), WallSignBlocks.COLORED_GLOWING_TERRACOTTA_WALL_SIGN);

    blockMap.put(WallSignBlocks.STONE_WALL_SIGN, WallSignBlocks.COLORED_STONE_WALL_SIGN);
    blockMap.put(WallSignBlocks.GLOWING_STONE_WALL_SIGN, WallSignBlocks.COLORED_GLOWING_STONE_WALL_SIGN);
    blockMap.put(WallSignBlocks.COBBLESTONE_WALL_SIGN, WallSignBlocks.COLORED_COBBLESTONE_WALL_SIGN);
    blockMap.put(WallSignBlocks.GLOWING_COBBLESTONE_WALL_SIGN, WallSignBlocks.COLORED_GLOWING_COBBLESTONE_WALL_SIGN);
    blockMap.put(WallSignBlocks.STONE_BRICK_WALL_SIGN, WallSignBlocks.COLORED_STONE_BRICK_WALL_SIGN);
    blockMap.put(WallSignBlocks.GLOWING_STONE_BRICK_WALL_SIGN, WallSignBlocks.COLORED_GLOWING_STONE_BRICK_WALL_SIGN);
    blockMap.put(WallSignBlocks.IRON_WALL_SIGN, WallSignBlocks.COLORED_IRON_WALL_SIGN);
    blockMap.put(WallSignBlocks.GLOWING_IRON_WALL_SIGN, WallSignBlocks.COLORED_GLOWING_IRON_WALL_SIGN);
    blockMap.put(StandingSignBlocks.STONE_STANDING_SIGN, StandingSignBlocks.COLORED_STONE_STANDING_SIGN);
    blockMap.put(StandingSignBlocks.GLOWING_STONE_STANDING_SIGN, StandingSignBlocks.COLORED_GLOWING_STONE_STANDING_SIGN);
    blockMap.put(StandingSignBlocks.COBBLESTONE_STANDING_SIGN, StandingSignBlocks.COLORED_COBBLESTONE_STANDING_SIGN);
    blockMap.put(StandingSignBlocks.GLOWING_COBBLESTONE_STANDING_SIGN, StandingSignBlocks.COLORED_GLOWING_COBBLESTONE_STANDING_SIGN);
    blockMap.put(StandingSignBlocks.STONE_BRICK_STANDING_SIGN, StandingSignBlocks.COLORED_STONE_BRICK_STANDING_SIGN);
    blockMap.put(StandingSignBlocks.GLOWING_STONE_BRICK_STANDING_SIGN, StandingSignBlocks.COLORED_GLOWING_STONE_BRICK_STANDING_SIGN);
    blockMap.put(StandingSignBlocks.IRON_STANDING_SIGN, StandingSignBlocks.COLORED_IRON_STANDING_SIGN);
    blockMap.put(StandingSignBlocks.GLOWING_IRON_STANDING_SIGN, StandingSignBlocks.COLORED_GLOWING_IRON_STANDING_SIGN);
  }

  private static void registerColorfulBlocks() {
    ColorfulBlockRegistry.registerColorfulBlocks(HandrailBlocks.DECORATED_IRON_HANDRAILS);
    ColorfulBlockRegistry.registerColorfulBlocks(HandrailBlocks.SIMPLE_CONCRETE_HANDRAILS);
    ColorfulBlockRegistry.registerColorfulBlocks(HandrailBlocks.SIMPLE_TERRACOTTA_HANDRAILS);
    ColorfulBlockRegistry.registerColorfulBlocks(HandrailBlocks.SIMPLE_STAINED_GLASS_HANDRAILS);
    ColorfulBlockRegistry.registerColorfulBlocks(HungSignBlocks.CONCRETE_HUNG_SIGNS);
    ColorfulBlockRegistry.registerColorfulBlocks(HungSignBlocks.CONCRETE_HUNG_SIGN_BARS);
    ColorfulBlockRegistry.registerColorfulBlocks(HungSignBlocks.TERRACOTTA_HUNG_SIGNS);
    ColorfulBlockRegistry.registerColorfulBlocks(HungSignBlocks.TERRACOTTA_HUNG_SIGN_BARS);
    ColorfulBlockRegistry.registerColorfulBlocks(HungSignBlocks.GLOWING_CONCRETE_HUNG_SIGNS);
    ColorfulBlockRegistry.registerColorfulBlocks(HungSignBlocks.GLOWING_TERRACOTTA_HUNG_SIGNS);
    ColorfulBlockRegistry.registerColorfulBlocks(StandingSignBlocks.CONCRETE_STANDING_SIGNS);
    ColorfulBlockRegistry.registerColorfulBlocks(StandingSignBlocks.GLOWING_CONCRETE_STANDING_SIGNS);
    ColorfulBlockRegistry.registerColorfulBlocks(StandingSignBlocks.TERRACOTTA_STANDING_SIGNS);
    ColorfulBlockRegistry.registerColorfulBlocks(StandingSignBlocks.GLOWING_TERRACOTTA_STANDING_SIGNS);
    ColorfulBlockRegistry.registerColorfulBlocks(WallSignBlocks.CONCRETE_WALL_SIGNS);
    ColorfulBlockRegistry.registerColorfulBlocks(WallSignBlocks.GLOWING_CONCRETE_WALL_SIGNS);
    ColorfulBlockRegistry.registerColorfulBlocks(WallSignBlocks.TERRACOTTA_WALL_SIGNS);
    ColorfulBlockRegistry.registerColorfulBlocks(WallSignBlocks.GLOWING_TERRACOTTA_WALL_SIGNS);
    ColorfulBlockRegistry.registerColorfulBlocks(WallSignBlocks.FULL_CONCRETE_WALL_SIGNS);
    ColorfulBlockRegistry.registerColorfulBlocks(WallSignBlocks.FULL_TERRACOTTA_WALL_SIGNS);
  }
}
