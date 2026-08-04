package pers.solid.mishang.uc.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import pers.solid.mishang.uc.data.stubs.*;
import pers.solid.mishang.uc.data.stubs.TextureKey;
import pers.solid.mishang.uc.data.stubs.TextureMap;
import pers.solid.mishang.uc.data.stubs.BlockStateModelGenerator;
import pers.solid.mishang.uc.data.stubs.ModelProvider;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.Mishanguc;
import pers.solid.mishang.uc.data.MishangucModels;
import pers.solid.mishang.uc.util.TextBridge;

import java.util.function.Function;
import net.minecraft.world.level.block.StairBlock;

@ApiStatus.AvailableSince("0.2.4")
public class GlassHandrailBlock extends HandrailBlock {
  public final ResourceLocation decorationTexture;
  private final CentralBlock central;
  private final CornerBlock corner;
  private final StairBlock stair;
  private final OuterBlock outer;
  private final Block baseBlock;
  private final ResourceLocation frameTexture;

  public GlassHandrailBlock(Block baseBlock, Properties settings, String frameTexture, String decorationTexture) {
    super(settings);
    this.baseBlock = baseBlock;
    this.frameTexture = new ResourceLocation(frameTexture);
    this.decorationTexture = new ResourceLocation(decorationTexture);
    this.central = new CentralBlock(this);
    this.corner = new CornerBlock(this);
    this.stair = new StairBlock(this);
    this.outer = new OuterBlock(this);
  }

  protected GlassHandrailBlock(Block baseBlock, Properties settings, String frameTexture, String decorationTexture, Function<GlassHandrailBlock, CentralBlock> centralProvider, Function<GlassHandrailBlock, CornerBlock> cornerProvider, Function<GlassHandrailBlock, StairBlock> stairProvider, Function<GlassHandrailBlock, OuterBlock> outerProvider) {
    super(settings.noOcclusion());
    this.baseBlock = baseBlock;
    this.frameTexture = new ResourceLocation(frameTexture);
    this.decorationTexture = new ResourceLocation(decorationTexture);
    central = centralProvider.apply(this);
    corner = cornerProvider.apply(this);
    stair = stairProvider.apply(this);
    outer = outerProvider.apply(this);
  }

  @Override
  public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
    final TextureMap textures = getTextures();
    final ResourceLocation modelId = MishangucModels.GLASS_HANDRAIL.upload(this, textures, blockStateModelGenerator.modelCollector);
    MishangucModels.GLASS_HANDRAIL_INVENTORY.upload(ModelIds.getItemModelId(asItem()), textures, blockStateModelGenerator.modelCollector);
    blockStateModelGenerator.blockStateCollector.accept(createBlockStates(modelId));
  }

  public static final TextureKey FRAME = TextureKey.of("frame");
  public static final TextureKey GLASS = TextureKey.of("glass");
  public static final TextureKey DECORATION = TextureKey.of("decoration");

  @Override
  public @NotNull TextureMap getTextures() {
    return new TextureMap().put(FRAME, frameTexture).put(GLASS, Mishanguc.id("block/glass_unframed")).put(DECORATION, decorationTexture);
  }

  @Override
  public HandrailCentralBlock<? extends HandrailBlock> central() {
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

  public static class CentralBlock extends HandrailCentralBlock<GlassHandrailBlock> {

    @Override
    public MutableComponent getName() {
      return TextBridge.translatable("block.mishanguc.handrail_central", baseHandrail.getName());
    }

    protected CentralBlock(@NotNull GlassHandrailBlock baseRail) {
      super(baseRail, BlockBehaviour.Properties.copy(baseRail).noOcclusion());
    }

    @Override
    public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
      final TextureMap textures = baseHandrail.getTextures();
      final ResourceLocation postModelId = MishangucModels.GLASS_HANDRAIL_POST.upload(this, textures, blockStateModelGenerator.modelCollector);
      final ResourceLocation sideModelId = MishangucModels.GLASS_HANDRAIL_SIDE.upload(this, textures, blockStateModelGenerator.modelCollector);
      final ResourceLocation postSideModelId = MishangucModels.GLASS_HANDRAIL_POST_SIDE.upload(this, textures, blockStateModelGenerator.modelCollector);
      blockStateModelGenerator.blockStateCollector.accept(createBlockStates(postModelId, postSideModelId, sideModelId));
    }
  }

  public static class CornerBlock extends HandrailCornerBlock<GlassHandrailBlock> {

    @Override
    public MutableComponent getName() {
      return TextBridge.translatable("block.mishanguc.handrail_corner", baseHandrail.getName());
    }

    protected CornerBlock(@NotNull GlassHandrailBlock baseRail) {
      super(baseRail, BlockBehaviour.Properties.copy(baseRail).noOcclusion());
    }

    @Override
    public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
      final ResourceLocation modelId = MishangucModels.GLASS_HANDRAIL_CORNER.upload(this, baseHandrail.getTextures(), blockStateModelGenerator.modelCollector);
      blockStateModelGenerator.blockStateCollector.accept(createBlockStates(modelId));
    }
  }

  public static class StairBlock extends HandrailStairBlock<GlassHandrailBlock> {

    protected StairBlock(@NotNull GlassHandrailBlock baseRail) {
      super(baseRail, BlockBehaviour.Properties.copy(baseRail).noOcclusion());
    }

    @Override
    public MutableComponent getName() {
      return TextBridge.translatable("block.mishanguc.handrail_stair", baseHandrail.getName());
    }

    @Override
    public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
      final TextureMap textures = baseHandrail.getTextures();
      final ResourceLocation baseModelId = MishangucModels.createBlock("glass_handrail_stair_middle_center", FRAME, GLASS, DECORATION).upload(this, textures, blockStateModelGenerator.modelCollector);
      for (Shape shape : Shape.values()) {
        for (Position position : Position.values()) {
          MishangucModels.createBlock(String.format("glass_handrail_stair_%s_%s", shape.getSerializedName(), position.getSerializedName()), "_" + shape.getSerializedName() + "_" + position.getSerializedName(), FRAME, GLASS, DECORATION).upload(this, textures, blockStateModelGenerator.modelCollector);
        }
      }
      blockStateModelGenerator.blockStateCollector.accept(createBlockStates(baseModelId));
    }
  }

  public static class OuterBlock extends HandrailOuterBlock<GlassHandrailBlock> {

    protected OuterBlock(@NotNull GlassHandrailBlock baseRail) {
      super(baseRail, BlockBehaviour.Properties.copy(baseRail).noOcclusion());
    }

    @Override
    public MutableComponent getName() {
      return TextBridge.translatable("block.mishanguc.handrail_outer", baseHandrail.getName());
    }

    @Override
    public void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator) {
      final ResourceLocation modelId = MishangucModels.GLASS_HANDRAIL_OUTER.upload(this, baseHandrail.getTextures(), blockStateModelGenerator.modelCollector);
      blockStateModelGenerator.blockStateCollector.accept(createBlockStates(modelId));
    }
  }
}
