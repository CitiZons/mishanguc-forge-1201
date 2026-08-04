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

public class GlowingHungSignBlock extends HungSignBlock {
  @ApiStatus.AvailableSince("0.1.7")
  protected static final ResourceLocation DEFAULT_GLOW_TEXTURE = Mishanguc.id("block/white_light");
  public ResourceLocation glowTexture;

  public GlowingHungSignBlock(@Nullable Block baseBlock, BlockBehaviour.Properties settings) {
    super(baseBlock, settings.lightLevel(state -> 15));
    this.glowTexture = DEFAULT_GLOW_TEXTURE;
  }

  @ApiStatus.AvailableSince("0.1.7")
  public GlowingHungSignBlock(@NotNull Block baseBlock) {
    this(baseBlock, BlockBehaviour.Properties.copy(baseBlock).lightLevel(state -> 15));
  }

  @Override
  public MutableComponent getName() {
    if (baseBlock != null) {
      return TextBridge.translatable("block.mishanguc.glowing_hung_sign", baseBlock.getName());
    }
    return super.getName();
  }

  @Override
  public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    final ResourceLocation texture = getBaseTexture();
    final TextureMap textures = TextureMap.texture(texture);
    if (barTexture != null) textures.put(MishangucTextureKeys.BAR, barTexture);
    if (textureTop != null) textures.put(MishangucTextureKeys.TEXTURE_TOP, textureTop);
    textures.put(MishangucTextureKeys.GLOW, glowTexture);

    final ResourceLocation id = MishangucModels.GLOWING_HUNG_SIGN.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation bodyId = MishangucModels.GLOWING_HUNG_SIGN_BODY.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation topBarId = MishangucModels.HUNG_SIGN_TOP_BAR.upload(this, textures, blockStateModelGenerator.modelCollector);
    final ResourceLocation topBarEdgeId = MishangucModels.HUNG_SIGN_TOP_BAR_EDGE.upload(this, textures, blockStateModelGenerator.modelCollector);

    blockStateModelGenerator.blockStateCollector.accept(createBlockStates(bodyId, topBarId, topBarEdgeId));
    blockStateModelGenerator.registerParentedItemModel(this, id);
  }

  private @Nullable String getRecipeGroup() {
    if (baseBlock instanceof ColoredBlock) return null;
    if (MishangUtils.isConcrete(baseBlock)) return "mishanguc:glowing_concrete_hung_sign";
    if (MishangUtils.isTerracotta(baseBlock)) return "mishanguc:glowing_terracotta_hung_sign";
    if (baseBlock == Blocks.BLUE_ICE || baseBlock == Blocks.PACKED_ICE) {
      return "mishanguc:glowing_ice_hung_sign";
    }
    return null;
  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    if (baseBlock == null) return null;
    return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, this, 6)
        .pattern("-#-")
        .pattern("-#-")
        .pattern("-#-")
        .define('#', baseBlock).define('-', WallSignBlocks.INVISIBLE_GLOWING_WALL_SIGN)
        .unlockedBy("has_base_block", FabricRecipeProvider.has(baseBlock))
        .unlockedBy("has_sign", FabricRecipeProvider.has(WallSignBlocks.INVISIBLE_GLOWING_WALL_SIGN))
        .group(getRecipeGroup());
  }
}
