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
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.Mishanguc;
import pers.solid.mishang.uc.blocks.WallSignBlocks;
import pers.solid.mishang.uc.data.MishangucModels;
import pers.solid.mishang.uc.data.MishangucTextureKeys;
import pers.solid.mishang.uc.util.TextBridge;

public class GlowingWallSignBlock extends WallSignBlock {
  @ApiStatus.AvailableSince("0.1.7")
  protected static final ResourceLocation DEFAULT_GLOW_TEXTURE = Mishanguc.id("block/white_light");
  /**
   * 告示牌发光部分的纹理。默认为 {@link #DEFAULT_GLOW_TEXTURE}。
   */
  public @Nullable ResourceLocation glowTexture = DEFAULT_GLOW_TEXTURE;

  public GlowingWallSignBlock(@Nullable Block baseBlock, Properties settings) {
    super(baseBlock, settings);
  }

  public GlowingWallSignBlock(@NotNull Block baseBlock) {
    this(baseBlock, BlockBehaviour.Properties.copy(baseBlock).lightLevel(state -> 15));
  }

  @Override
  public MutableComponent getName() {
    return TextBridge.translatable("block.mishanguc.glowing_wall_sign", baseBlock.getName());
  }

  private @Nullable String getRecipeGroup() {
    if (baseBlock instanceof ColoredBlock) return null;
    if (MishangUtils.isConcrete(baseBlock)) return "mishanguc:glowing_concrete_wall_sign";
    if (MishangUtils.isTerracotta(baseBlock)) return "mishanguc:glowing_terracotta_wall_sign";
    if (baseBlock == Blocks.BLUE_ICE || baseBlock == Blocks.PACKED_ICE) {
      return "mishanguc:glowing_ice_wall_sign";
    }
    return null;
  }

  @Override
  public @Nullable RecipeBuilder getCraftingRecipe() {
    if (baseBlock == null) return null;
    return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, this, 6)
        .pattern("---")
        .pattern("###")
        .pattern("---")
        .define('#', baseBlock).define('-', WallSignBlocks.INVISIBLE_GLOWING_WALL_SIGN)
        .unlockedBy("has_base_block", FabricRecipeProvider.has(baseBlock)).unlockedBy("has_sign", FabricRecipeProvider.has(WallSignBlocks.INVISIBLE_GLOWING_WALL_SIGN))
        .group(getRecipeGroup());
  }

  @Override
  public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    final TextureMap textures = TextureMap.texture(getBaseTexture()).put(MishangucTextureKeys.GLOW, glowTexture);
    final ResourceLocation modelId = MishangucModels.GLOWING_WALL_SIGN.upload(this, textures, blockStateModelGenerator.modelCollector);
    blockStateModelGenerator.blockStateCollector.accept(createBlockStates(modelId));
    blockStateModelGenerator.registerParentedItemModel(this, modelId);
  }
}
