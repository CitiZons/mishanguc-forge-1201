package pers.solid.mishang.uc.data.stubs;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class Models {
    private Models() {}

    public static final Model GENERATED = new Model(Optional.of(new ResourceLocation("minecraft", "item/generated")), Optional.empty(), TextureKey.LAYER0);
    public static final Model HANDHELD = new Model(Optional.of(new ResourceLocation("minecraft", "item/handheld")), Optional.empty(), TextureKey.LAYER0);
    public static final Model LEAVES = new Model(Optional.of(new ResourceLocation("minecraft", "block/leaves")), Optional.empty(), TextureKey.ALL);
    public static final Model SLAB = new Model(Optional.of(new ResourceLocation("minecraft", "block/slab")), Optional.empty(), TextureKey.BOTTOM, TextureKey.TOP, TextureKey.SIDE);
    public static final Model SLAB_TOP = new Model(Optional.of(new ResourceLocation("minecraft", "block/slab_top")), Optional.empty(), TextureKey.BOTTOM, TextureKey.TOP, TextureKey.SIDE);
    public static final Model CUBE_ALL = new Model(Optional.of(new ResourceLocation("minecraft", "block/cube_all")), Optional.empty(), TextureKey.ALL);
    public static final Model CUBE_BOTTOM_TOP = new Model(Optional.of(new ResourceLocation("minecraft", "block/cube_bottom_top")), Optional.empty(), TextureKey.BOTTOM, TextureKey.TOP, TextureKey.SIDE);
    public static final Model CUBE_COLUMN = new Model(Optional.of(new ResourceLocation("minecraft", "block/cube_column")), Optional.empty(), TextureKey.END, TextureKey.SIDE);
}
