package pers.solid.mishang.uc.data.stubs;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Stub for Fabric's PacketByteBufs
 * (net.fabricmc.fabric.api.networking.v1.PacketByteBufs).
 * To be replaced with proper Forge networking implementation later.
 */
public final class PacketByteBufs {
    private PacketByteBufs() {}

    /**
     * Create a new empty FriendlyByteBuf for packet data.
     */
    public static FriendlyByteBuf create() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    /**
     * Create an empty, read-only FriendlyByteBuf.
     */
    public static FriendlyByteBuf empty() {
        return new FriendlyByteBuf(Unpooled.EMPTY_BUFFER);
    }
}
