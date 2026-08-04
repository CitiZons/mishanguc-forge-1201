package pers.solid.mishang.uc.data.stubs;

import net.minecraft.world.phys.HitResult;

public class WorldRenderEvents {
    @FunctionalInterface
    public interface BlockOutline {
        boolean onBlockOutline(WorldRenderContext context, WorldRenderContext.BlockOutlineContext blockOutlineContext);
    }

    @FunctionalInterface
    public interface BeforeBlockOutline {
        boolean onBeforeBlockOutline(WorldRenderContext context, HitResult hitResult);
    }

    public static final Event<BlockOutline> BLOCK_OUTLINE = new Event<>();
    public static final Event<BeforeBlockOutline> BEFORE_BLOCK_OUTLINE = new Event<>();

    public static class Event<T> {
        public void register(T listener) {}
    }
}
