package pers.solid.mishang.uc.item;

import pers.solid.mishang.uc.MishangUtils;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.Mishanguc;
import pers.solid.mishang.uc.block.AbstractRoadBlock;
import pers.solid.mishang.uc.block.AbstractRoadSlabBlock;
import pers.solid.mishang.uc.block.Road;
import pers.solid.mishang.uc.block.RoadWithAutoLine;
import pers.solid.mishang.uc.blocks.RoadBlocks;
import pers.solid.mishang.uc.util.LineColor;
import pers.solid.mishang.uc.util.TextBridge;

import java.util.List;
import pers.solid.mishang.uc.data.stubs.WorldRenderContext;

@ApiStatus.AvailableSince("0.2.4")
public class RoadToolItem extends BlockToolItem implements MishangucItem {
  public RoadToolItem(Properties settings) {
    super(settings, Boolean.FALSE);
  }

  @Override
  public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    super.appendHoverText(stack, world, tooltip, context);
    tooltip.add(TextBridge.translatable("item.mishanguc.road_tool.tooltip.1", TextBridge.keybind("key.attack").withStyle(style -> style.withColor(0xdddddd))).withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.road_tool.tooltip.2", TextBridge.keybind("key.use").withStyle(style -> style.withColor(0xdddddd))).withStyle(ChatFormatting.GRAY));
    tooltip.add(TextBridge.translatable("item.mishanguc.road_tool.tooltip.3").withStyle(ChatFormatting.GRAY));
  }

  @Override
  public InteractionResult useOnBlock(ItemStack stack, Player player, Level world, BlockHitResult blockHitResult, InteractionHand hand, boolean fluidIncluded) {
    final BlockPos blockPos = blockHitResult.getBlockPos();
    final BlockState blockState = world.getBlockState(blockPos);
    if (blockState.is(RoadBlocks.ROAD_BLOCK)) {
      if (!world.isClientSide) {
        world.setBlockAndUpdate(blockPos, MishangUtils.getStateWithProperties((player.isShiftKeyDown() ? RoadBlocks.ROAD_WITH_WHITE_AUTO_RA_LINE : RoadBlocks.ROAD_WITH_WHITE_AUTO_BA_LINE), blockState));
        player.getItemInHand(hand).hurtAndBreak(1, player, playerEntity -> playerEntity.broadcastBreakEvent(hand));
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.road_tool.message.painted"), true);
      }
      return InteractionResult.SUCCESS;
    } else if (blockState.is(RoadBlocks.ROAD_BLOCK.getRoadSlab())) {
      if (!world.isClientSide) {
        world.setBlockAndUpdate(blockPos, MishangUtils.getStateWithProperties((player.isShiftKeyDown() ? RoadBlocks.ROAD_WITH_WHITE_AUTO_RA_LINE : RoadBlocks.ROAD_WITH_WHITE_AUTO_BA_LINE).getRoadSlab(), blockState));
        player.getItemInHand(hand).hurtAndBreak(1, player, playerEntity -> playerEntity.broadcastBreakEvent(hand));
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.road_tool.message.painted"), true);
      }
      return InteractionResult.SUCCESS;
    } else if (blockState.getBlock() instanceof RoadWithAutoLine roadWithAutoLine) {
      if (!world.isClientSide) {
        try {
          final BlockState newState = roadWithAutoLine.makeState(roadWithAutoLine.getConnectionStateMap(world, blockPos), blockState);
          world.setBlockAndUpdate(blockPos, newState);
          player.displayClientMessage(TextBridge.translatable("item.mishanguc.road_tool.message.converted"), true);
        } catch (Throwable throwable) {
          Mishanguc.MISHANG_LOGGER.error("An error was found when converting block state at {}:", blockPos, throwable);
          player.displayClientMessage(TextBridge.translatable("item.mishanguc.road_tool.message.error").withStyle(ChatFormatting.RED), true);
        }
      }
      return InteractionResult.SUCCESS;
    } else if (!(blockState.getBlock() instanceof Road)) {
      player.displayClientMessage(TextBridge.translatable("item.mishanguc.road_tool.message.not_road").withStyle(ChatFormatting.RED), true);
      return InteractionResult.FAIL;
    }
    return InteractionResult.PASS;
  }

  @Override
  public InteractionResult beginAttackBlock(ItemStack stack, Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction, boolean fluidIncluded) {
    final BlockState blockState = world.getBlockState(pos);
    final Block block = blockState.getBlock();
    if (!(block instanceof Road)) {
      player.displayClientMessage(TextBridge.translatable("item.mishanguc.road_tool.message.not_road").withStyle(ChatFormatting.RED), true);
      return InteractionResult.FAIL;
    }
    if (block instanceof AbstractRoadBlock) {
      if (!world.isClientSide) {
        world.setBlockAndUpdate(pos, MishangUtils.getStateWithProperties(RoadBlocks.ROAD_BLOCK, blockState));
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.road_tool.message.cleared"), true);
        player.getItemInHand(hand).hurtAndBreak(1, player, playerEntity -> playerEntity.broadcastBreakEvent(hand));
      }
      return InteractionResult.SUCCESS;
    } else if (block instanceof AbstractRoadSlabBlock) {
      if (!world.isClientSide) {
        world.setBlockAndUpdate(pos, MishangUtils.getStateWithProperties(RoadBlocks.ROAD_BLOCK.getRoadSlab(), blockState));
        player.displayClientMessage(TextBridge.translatable("item.mishanguc.road_tool.message.cleared"), true);
        player.getItemInHand(hand).hurtAndBreak(1, player, playerEntity -> playerEntity.broadcastBreakEvent(hand));
      }
      return InteractionResult.SUCCESS;
    }
    return InteractionResult.PASS;
  }

  @OnlyIn(Dist.CLIENT)
  @Override
  public boolean renderBlockOutline(Player player, ItemStack itemStack, WorldRenderContext worldRenderContext, WorldRenderContext.BlockOutlineContext blockOutlineContext, InteractionHand hand) {
    if (worldRenderContext.world().getBlockState(blockOutlineContext.blockPos()).getBlock() instanceof Road) {
      return super.renderBlockOutline(player, itemStack, worldRenderContext, blockOutlineContext, hand);
    }
    return false;
  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, this)
        .pattern("aba")
        .pattern("bXb")
        .pattern("aba")
        .define('a', LineColor.WHITE.getIngredient())
        .define('b', LineColor.YELLOW.getIngredient())
        .define('X', Items.STICK)
        .unlockedBy("has_white_dye", FabricRecipeProvider.has(LineColor.WHITE.getIngredient()))
        .unlockedBy("has_yellow_dye", FabricRecipeProvider.has(LineColor.YELLOW.getIngredient()));
  }
}
