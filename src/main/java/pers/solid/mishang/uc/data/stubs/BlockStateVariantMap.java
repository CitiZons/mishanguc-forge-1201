package pers.solid.mishang.uc.data.stubs;

import net.minecraft.world.level.block.state.properties.Property;

import java.util.function.Function;

/**
 * Stub for Fabric's BlockStateVariantMap.
 * TODO: Replace with Forge data gen.
 */
public class BlockStateVariantMap {
    public static <T extends Comparable<T>> SingleProperty<T> create(Property<T> property) {
        return new SingleProperty<>();
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> DoubleProperty<T1, T2> create(Property<T1> p1, Property<T2> p2) {
        return new DoubleProperty<>();
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> TripleProperty<T1, T2, T3> create(Property<T1> p1, Property<T2> p2, Property<T3> p3) {
        return new TripleProperty<>();
    }

    public <T extends Comparable<T>> BlockStateVariantMap register(T value, BlockStateVariant variant) {
        return this;
    }

    public <T extends Comparable<T>> BlockStateVariantMap register(T value, BlockStateSupplier supplier) {
        return this;
    }

    public BlockStateVariantMap register(java.util.List<BlockStateVariant> variants) {
        return this;
    }

    public static class SingleProperty<T> extends BlockStateVariantMap {
        public SingleProperty<T> register(T v, BlockStateVariant variant) {
            return this;
        }
        public SingleProperty<T> register(T v, java.util.List<BlockStateVariant> variants) {
            return this;
        }
        public SingleProperty<T> register(java.util.function.Function<T, BlockStateVariant> func) {
            return this;
        }
    }

    public static class DoubleProperty<T1, T2> extends BlockStateVariantMap {
        public DoubleProperty<T1, T2> register(T1 v1, T2 v2, BlockStateVariant variant) {
            return this;
        }

        public DoubleProperty<T1, T2> register(T1 v1, T2 v2, java.util.List<BlockStateVariant> variants) {
            return this;
        }

        public DoubleProperty<T1, T2> register(BiFunc<T1, T2, BlockStateVariant> func) {
            return this;
        }
    }

    @FunctionalInterface
    public interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    @FunctionalInterface
    public interface BiFunc<A, B, R> {
        R apply(A a, B b);
    }

    public static class SingleProperty2<T> extends BlockStateVariantMap {}

    public static class TripleProperty<T1, T2, T3> extends BlockStateVariantMap {
        public TripleProperty<T1, T2, T3> register(T1 v1, T2 v2, T3 v3, BlockStateVariant variant) {
            return this;
        }

        public TripleProperty<T1, T2, T3> register(T1 v1, T2 v2, T3 v3, java.util.List<BlockStateVariant> variants) {
            return this;
        }

        public TripleProperty<T1, T2, T3> register(TriFunction<T1, T2, T3, BlockStateVariant> func) {
            return this;
        }
    }
}
