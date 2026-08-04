package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
// TODO: Forge data gen - BlockStateModelGenerator
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.Mishanguc;
import pers.solid.mishang.uc.blocks.WallSignBlocks;
import pers.solid.mishang.uc.data.MishangucModels;
import pers.solid.mishang.uc.data.MishangucTextureKeys;
import pers.solid.mishang.uc.util.TextBridge;

/**
 * 发光的直立告示牌。
 */
public class GlowingStandingSignBlock extends StandingSignBlock {
  protected static final ResourceLocation DEFAULT_GLOW_TEXTURE = Mishanguc.id("block/white_light");
  public ResourceLocation glowTexture = DEFAULT_GLOW_TEXTURE;

  public GlowingStandingSignBlock(@Nullable Block baseBlock, Properties settings) {
    super(baseBlock, settings);
  }

  public GlowingStandingSignBlock(@NotNull Block baseBlock) {
    this(baseBlock, BlockBehaviour.Properties.copy(baseBlock).lightLevel(state -> 15));
  }

  @Override
  public MutableComponent getName() {
    if (baseBlock != null) return TextBridge.translatable("block.mishanguc.glowing_standing_sign", baseBlock.getName());
    return super.getName();
  }

  @Override
  public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    final TextureMap textures = TextureMap.texture(getBaseTexture()).put(MishangucTextureKeys.BAR, barTexture).put(MishangucTextureKeys.GLOW, glowTexture);
    final ResourceLocation modelId = MishangucModels.GLOWING_STANDING_SIGN.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation r1ModelId = MishangucModels.GLOWING_STANDING_SIGN_1.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation r2ModelId = MishangucModels.GLOWING_STANDING_SIGN_2.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation r3ModelId = MishangucModels.GLOWING_STANDING_SIGN_3.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation barredModelId = MishangucModels.GLOWING_STANDING_SIGN_BARRED.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation barredR1ModelId = MishangucModels.GLOWING_STANDING_SIGN_BARRED_1.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation barredR2ModelId = MishangucModels.GLOWING_STANDING_SIGN_BARRED_2.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation barredR3ModelId = MishangucModels.GLOWING_STANDING_SIGN_BARRED_3.upload(this, textures, blockStateModelGenerator.modelCollector);
    blockStateModelGenerator.blockStateCollector.accept(createBlockStates(modelId, r1ModelId, r2ModelId, r3ModelId, barredModelId, barredR1ModelId, barredR2ModelId, barredR3ModelId));
    blockStateModelGenerator.registerParentedItemModel(this, barredModelId);
  }

  private @Nullable String getRecipeGroup() {
    if (baseBlock instanceof ColoredBlock) return null;
    if (MishangUtils.isConcrete(baseBlock)) return "mishanguc:glowing_concrete_standing_sign";
    if (MishangUtils.isTerracotta(baseBlock)) return "mishanguc:glowing_terracotta_standing_sign";
    if (baseBlock == Blocks.BLUE_ICE || baseBlock == Blocks.PACKED_ICE) {
      return "mishanguc:glowing_ice_standing_sign";
    }
    return null;
  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    if (baseBlock == null) return null;
    return ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, this, 4)
        .pattern("---")
        .pattern("###")
        .pattern(" | ")
        .define('#', baseBlock).define('-', WallSignBlocks.INVISIBLE_GLOWING_WALL_SIGN).define('|', Items.STICK)
        .unlockedBy("has_base_block", FabricRecipeProvider.has(baseBlock))
        .unlockedBy("has_sign", FabricRecipeProvider.has(WallSignBlocks.INVISIBLE_GLOWING_WALL_SIGN))
        .group(getRecipeGroup());
  }
}
