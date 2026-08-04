package pers.solid.mishang.uc.data.stubs;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Stub for Fabric's ServerPlayNetworking
 * (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking).
 * To be replaced with proper Forge networking implementation later.
 */
public final class ServerPlayNetworking {
    private ServerPlayNetworking() {}

    /**
     * 把封包发送到指定玩家的客户端。委托到 Forge 的 {@link pers.solid.mishang.uc.networking.MishangucNetwork}。
     */
    public static void send(ServerPlayer player, ResourceLocation channel, FriendlyByteBuf buf) {
        pers.solid.mishang.uc.networking.MishangucNetwork.sendToClient(player, channel, buf);
    }

    /**
     * 注册一个服务端频道接收器。委托到 {@link pers.solid.mishang.uc.networking.MishangucNetwork}。
     */
    public static void registerGlobalReceiver(ResourceLocation channel, PlayChannelHandler handler) {
        pers.solid.mishang.uc.networking.MishangucNetwork.registerServerReceiver(channel, handler);
    }

    /**
     * Handler interface for server-play networking channels.
     */
    @FunctionalInterface
    public interface PlayChannelHandler {
        void receive(net.minecraft.server.MinecraftServer server,
                     ServerPlayer player,
                     net.minecraft.server.network.ServerGamePacketListenerImpl handler,
                     FriendlyByteBuf buf,
                     PacketSender responseSender);
    }
}
