package pers.solid.mishang.uc.data.stubs;

import net.minecraft.world.level.block.state.properties.Property;

/**
 * Stub for Fabric's When (multipart blockstate condition).
 */
public class When {
    public static When create() {
        return new When();
    }

    public <T extends Comparable<T>> When set(Property<T> property, T value) {
        return this;
    }

    @SafeVarargs
    public final <T extends Comparable<T>> When set(Property<T> property, T... values) {
        return this;
    }

    public static When anyOf(When... conditions) {
        return new When();
    }

    public static When allOf(When... conditions) {
        return new When();
    }
}
