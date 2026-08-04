package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.data.stubs.FabricRecipeProvider;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import pers.solid.mishang.uc.data.stubs.*;
import pers.solid.mishang.uc.data.stubs.TextureKey;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.data.MishangucModels;
import pers.solid.mishang.uc.util.TextBridge;
import net.minecraft.world.level.block.StairBlock;

/**
 * 简单的栏杆方块。基本上都是采用相同的纹理，如有使用也可以采用不同的纹理。其形状都是最基本的图形。
 */
public class SimpleHandrailBlock extends HandrailBlock {
  /**
   * 该栏杆的基础方块。
   */
  public final @Nullable Block baseBlock;

  /**
   * 该方块对应的中间位置版本。
   */
  public final CentralBlock central;
  /**
   * 该方块对应的角落位置版本。
   */
  public final CornerBlock corner;
  /**
   * 该方块对应的楼梯扶手方块。
   */
  public final StairBlock stair;
  /**
   * 该方块对应的外角方块。
   */
  public final OuterBlock outer;

  /**
   * 栏杆的纹理。若为 {@code null}，则默认根据 {@link #baseBlock} 推断纹理。
   */
  public @Nullable ResourceLocation texture;
  /**
   * 栏杆顶部部分的纹理。
   */
  public @Nullable ResourceLocation top;
  /**
   * 栏杆底部部分的纹理。
   */
  public @Nullable ResourceLocation bottom;

  public SimpleHandrailBlock(@Nullable Block baseBlock, Properties settings) {
    super(settings.noOcclusion());
    this.baseBlock = baseBlock;
    this.central = new CentralBlock(this);
    this.corner = new CornerBlock(this);
    this.stair = new StairBlock(this);
    this.outer = new OuterBlock(this);
  }

  public SimpleHandrailBlock(@NotNull Block baseBlock) {
    this(baseBlock, BlockBehaviour.Properties.copy(baseBlock));
  }

  @Override
  public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    final TextureMap textures = getTextures();
    final ResourceLocation modelId = MishangucModels.SIMPLE_HANDRAIL.upload(this, textures, blockStateModelGenerator.modelCollector);
    MishangucModels.SIMPLE_HANDRAIL_INVENTORY.upload(ModelIds.getItemModelId(asItem()), textures, blockStateModelGenerator.modelCollector);
    blockStateModelGenerator.blockStateCollector.accept(createBlockStates(modelId));
  }

  @Override
  public @NotNull TextureMap getTextures() {
    return TextureMap.all(getTexture()).put(TextureKey.TOP, top).put(TextureKey.BOTTOM, bottom);
  }

  @Override
  public final HandrailCentralBlock<SimpleHandrailBlock> central() {
    return central;
  }

  @Override
  public HandrailCornerBlock<? extends HandrailBlock> corner() {
    return corner;
  }

  @Override
  public HandrailStairBlock<? extends HandrailBlock> stair() {
    return stair;
  }

  @Override
  public HandrailOuterBlock<? extends HandrailBlock> outer() {
    return outer;
  }

  @Override
  public @Nullable Block baseBlock() {
    return baseBlock;
  }

  /**
   * @return 该方块的基础纹理变量。
   */
  protected ResourceLocation getTexture() {
    return texture == null ? TextureMap.getId(baseBlock) : texture;
  }

  @Override
  public MutableComponent getName() {
    if (baseBlock != null) {
      return TextBridge.translatable("block.mishanguc.simple_handrail", baseBlock.getName());
    } else return super.getName();
  }

  public static class CentralBlock extends HandrailCentralBlock<SimpleHandrailBlock> {
    public CentralBlock(@NotNull SimpleHandrailBlock baseBlock) {
      super(baseBlock, BlockBehaviour.Properties.copy(baseBlock).noOcclusion());
    }

    @Override
    public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
      final ResourceLocation postModelId = MishangucModels.SIMPLE_HANDRAIL_POST.upload(this, baseHandrail.getTextures(), blockStateModelGenerator.modelCollector);
      final ResourceLocation sideModelId = MishangucModels.SIMPLE_HANDRAIL_SIDE.upload(this, baseHandrail.getTextures(), blockStateModelGenerator.modelCollector);
      final ResourceLocation postSideModelId = MishangucModels.SIMPLE_HANDRAIL_POST_SIDE.upload(this, baseHandrail.getTextures(), blockStateModelGenerator.modelCollector);
      blockStateModelGenerator.blockStateCollector.accept(createBlockStates(postModelId, postSideModelId, sideModelId));
    }

    @Override
    public MutableComponent getName() {
      final Block block = baseBlock();
      return block == null ? super.getName() : TextBridge.translatable("block.mishanguc.simple_handrail_central", block.getName());
    }
  }

  public static class CornerBlock extends HandrailCornerBlock<SimpleHandrailBlock> {
    public CornerBlock(@NotNull SimpleHandrailBlock baseHandrail) {
      super(baseHandrail, BlockBehaviour.Properties.copy(baseHandrail).noOcclusion());
    }

    @Override
    public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
      final ResourceLocation modelId = MishangucModels.SIMPLE_HANDRAIL_CORNER.upload(this, baseHandrail.getTextures(), blockStateModelGenerator.modelCollector);
      blockStateModelGenerator.blockStateCollector.accept(createBlockStates(modelId));
    }

    @Override
    public MutableComponent getName() {
      final Block block = baseBlock();
      return block == null ? super.getName() : TextBridge.translatable("block.mishanguc.simple_handrail_corner", block.getName());
    }
  }

  public static class StairBlock extends HandrailStairBlock<SimpleHandrailBlock> {
    public StairBlock(@NotNull SimpleHandrailBlock baseRail) {
      super(baseRail, BlockBehaviour.Properties.copy(baseRail).noOcclusion());
    }

    @Override
    public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
      final TextureMap textures = baseHandrail.getTextures();
      final ResourceLocation baseModelId = MishangucModels.createBlock("simple_handrail_stair_middle_center", TextureKey.TEXTURE, TextureKey.TOP, TextureKey.BOTTOM).upload(this, textures, blockStateModelGenerator.modelCollector);
      for (Shape shape : Shape.values()) {
        for (Position position : Position.values()) {
          MishangucModels.createBlock(String.format("simple_handrail_stair_%s_%s", shape.getSerializedName(), position.getSerializedName()), "_" + shape.getSerializedName() + "_" + position.getSerializedName(), TextureKey.TEXTURE, TextureKey.TOP, TextureKey.BOTTOM).upload(this, textures, blockStateModelGenerator.modelCollector);
        }
      }
      blockStateModelGenerator.blockStateCollector.accept(createBlockStates(baseModelId));
    }

    @Override
    public MutableComponent getName() {
      final Block block = baseBlock();
      return block == null ? super.getName() : TextBridge.translatable("block.mishanguc.simple_handrail_stair", block.getName());
    }
  }

  public static class OuterBlock extends HandrailOuterBlock<SimpleHandrailBlock> {
    public OuterBlock(@NotNull SimpleHandrailBlock baseRail) {
      super(baseRail, BlockBehaviour.Properties.copy(baseRail).noOcclusion());
    }

    @Override
    public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
      final ResourceLocation modelId = MishangucModels.SIMPLE_HANDRAIL_OUTER.upload(this, baseHandrail.getTextures(), blockStateModelGenerator.modelCollector);
      blockStateModelGenerator.blockStateCollector.accept(createBlockStates(modelId));
    }

    @Override
    public MutableComponent getName() {
      final Block block = baseBlock();
      return block == null ? super.getName() : TextBridge.translatable("block.mishanguc.simple_handrail_outer", block.getName());
    }
  }

  private String getRecipeGroup() {
    if (baseBlock instanceof ColoredBlock) return null;
    if (MishangUtils.isConcrete(baseBlock)) return "mishanguc:simple_concrete_handrail";
    if (MishangUtils.isTerracotta(baseBlock)) return "mishanguc:simple_terracotta_handrail";
    if (MishangUtils.isStained_glass(baseBlock)) return "mishanguc:simple_stained_glass_handrail";
    if (MishangUtils.isWood(baseBlock)) return "mishanguc:simple_wood_handrail";
    if (MishangUtils.isPlanks(baseBlock)) return "mishanguc:simple_plank_handrail";
    if (baseBlock == Blocks.ICE || baseBlock == Blocks.PACKED_ICE || baseBlock == Blocks.BLUE_ICE) {
      return "mishanguc:simple_ice_handrail";
    }
    return null;
  }

  @Override
  public RecipeBuilder getCraftingRecipe() {
    return SingleItemRecipeBuilder.stonecutting(Ingredient.of(baseBlock), RecipeCategory.DECORATIONS, this, 5)
        .unlockedBy(FabricRecipeProvider.getHasName(baseBlock), FabricRecipeProvider.has(baseBlock))
        .group(getRecipeGroup());
  }
}
