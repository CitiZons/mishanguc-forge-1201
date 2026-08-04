package pers.solid.mishang.uc.data.stubs;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Stub for Fabric's ClientPlayNetworking
 * (net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking).
 * To be replaced with proper Forge networking implementation later.
 */
public final class ClientPlayNetworking {
    private ClientPlayNetworking() {}

    /**
     * 把封包发送到服务端。委托到 Forge 的 {@link pers.solid.mishang.uc.networking.MishangucNetwork}。
     */
    public static void send(ResourceLocation channel, FriendlyByteBuf buf) {
        pers.solid.mishang.uc.networking.MishangucNetwork.sendToServer(channel, buf);
    }

    /**
     * 注册一个客户端频道接收器。委托到 {@link pers.solid.mishang.uc.networking.MishangucNetwork}。
     */
    public static void registerGlobalReceiver(ResourceLocation channel, PlayChannelHandler handler) {
        pers.solid.mishang.uc.networking.MishangucNetwork.registerClientReceiver(channel, handler);
    }

    /**
     * Handler interface for client-play networking channels.
     */
    @FunctionalInterface
    public interface PlayChannelHandler {
        void receive(net.minecraft.client.Minecraft client,
                     net.minecraft.client.multiplayer.ClientPacketListener handler,
                     FriendlyByteBuf buf,
                     PacketSender responseSender);
    }
}
