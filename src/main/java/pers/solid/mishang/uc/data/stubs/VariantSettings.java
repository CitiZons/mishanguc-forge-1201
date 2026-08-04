package pers.solid.mishang.uc.data.stubs;

import net.minecraft.resources.ResourceLocation;

/**
 * Stub for Fabric's VariantSettings.
 * TODO: Replace with Forge data gen.
 */
public final class VariantSettings {
    private VariantSettings() {}

    public static final Key<ResourceLocation> MODEL = new Key<>("model");
    public static final Key<Rotation> X = new Key<>("x");
    public static final Key<Rotation> Y = new Key<>("y");
    public static final Key<Boolean> UVLOCK = new Key<>("uvlock");

    public static class Key<T> {
        private final String name;
        public Key(String name) { this.name = name; }
    }

    public enum Rotation {
        R0(0), R90(90), R180(180), R270(270);
        public final int degrees;
        Rotation(int degrees) { this.degrees = degrees; }
    }
}
