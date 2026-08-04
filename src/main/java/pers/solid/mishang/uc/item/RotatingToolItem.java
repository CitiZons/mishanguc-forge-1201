package pers.solid.mishang.uc.item;

import net.minecraft.world.level.block.state.pattern.BlockInWorld;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.util.TextBridge;

import java.util.List;

public class RotatingToolItem extends BlockToolItem implements MishangucItem {

  public RotatingToolItem(Properties settings, @Nullable Boolean includesFluid) {
    super(settings, includesFluid);
  }

  @Override
  public InteractionResult useOnBlock(
      ItemStack stack, Player player,
      Level world,
      BlockHitResult blockHitResult,
      InteractionHand hand,
      boolean fluidIncluded) {
    final BlockPos blockPos = blockHitResult.getBlockPos();
    final InteractionResult result = rotateBlock(player, world, blockPos);
    if (result == InteractionResult.SUCCESS) {
      stack.hurtAndBreak(1, player, player1 -> player1.broadcastBreakEvent(hand));
    }
    return result;
  }

  @NotNull
  private InteractionResult rotateBlock(Player player, Level world, BlockPos blockPos) {
    if (world.getBlockState(blockPos).getBlock() instanceof GameMasterBlock && !player.hasPermissions(2)) {
      return InteractionResult.FAIL;
    }
    final Rotation rotation = player.isShiftKeyDown() ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90;
    return rotateBlock(world, blockPos, rotation);
  }

  @NotNull
  private InteractionResult rotateBlock(Level world, BlockPos blockPos, Rotation rotation) {
    final boolean b = world.setBlockAndUpdate(blockPos, world.getBlockState(blockPos).rotate(rotation));
    return InteractionResult.sidedSuccess(b);
  }

  @Override
  public InteractionResult beginAttackBlock(
      ItemStack stack, Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction, boolean fluidIncluded) {
    if (!player.getAbilities().mayBuild && !stack.hasAdventureModeBreakTagForBlock(BuiltInRegistries.BLOCK, new BlockInWorld(world, pos, false))) {
      return InteractionResult.PASS;
    }
    final InteractionResult result = rotateBlock(player, world, pos);
    if (result == InteractionResult.SUCCESS) {
      stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
    }
    return result;
  }

  @Override
  public void appendHoverText(
      ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    super.appendHoverText(stack, world, tooltip, context);
    tooltip.add(
        TextBridge.translatable("item.mishanguc.rotating_tool.tooltip.1")
            .withStyle(ChatFormatting.GRAY));
    tooltip.add(
        TextBridge.translatable("item.mishanguc.rotating_tool.tooltip.2")
            .withStyle(ChatFormatting.GRAY));
  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, this)
        .pattern("DND")
        .pattern(" | ")
        .pattern(" | ")
        .define('D', Items.PINK_DYE)
        .define('N', Items.NETHERITE_INGOT)
        .define('|', Items.STICK)
        .unlockedBy("has_pink_dye", FabricRecipeProvider.has(Items.PINK_DYE))
        .unlockedBy("has_netherite_ingot", FabricRecipeProvider.has(Items.NETHERITE_INGOT));
  }
}
