package pers.solid.mishang.uc.data.stubs;

/**
 * Stub for Fabric's TextureKey (net.minecraft.data.models.TextureKey in Fabric mappings).
 * To be replaced with proper Forge data gen implementation later.
 */
public final class TextureKey {
    private final String name;

    private TextureKey(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // Common static texture keys mirroring Fabric's API
    public static final TextureKey ALL = new TextureKey("all");
    public static final TextureKey TEXTURE = new TextureKey("texture");
    public static final TextureKey TOP = new TextureKey("top");
    public static final TextureKey BOTTOM = new TextureKey("bottom");
    public static final TextureKey SIDE = new TextureKey("side");
    public static final TextureKey PARTICLE = new TextureKey("particle");
    public static final TextureKey PANE = new TextureKey("pane");
    public static final TextureKey EDGE = new TextureKey("edge");
    public static final TextureKey END = new TextureKey("end");
    public static final TextureKey LAYER0 = new TextureKey("layer0");

    public static TextureKey of(String name) {
        return new TextureKey(name);
    }

    public static TextureKey of(String name, TextureKey parent) {
        return new TextureKey(name);
    }

    @Override
    public String toString() {
        return "TextureKey{" + name + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TextureKey)) return false;
        return name.equals(((TextureKey) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
