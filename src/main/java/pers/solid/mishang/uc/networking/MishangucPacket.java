package pers.solid.mishang.uc.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 一个通用的封包，携带一个频道标识（{@link ResourceLocation}）和原始字节数据。
 *
 * <p>本模组用单个 Forge {@code SimpleChannel} 承载所有逻辑频道，借此模拟 Fabric 多频道的网络 API：
 * 发送方把目标频道写进 {@link #id}，接收方按 id 分发到对应的处理器。
 */
public record MishangucPacket(ResourceLocation id, byte[] data) {
  static void encode(MishangucPacket packet, FriendlyByteBuf buf) {
    buf.writeResourceLocation(packet.id);
    buf.writeByteArray(packet.data);
  }

  static MishangucPacket decode(FriendlyByteBuf buf) {
    final ResourceLocation id = buf.readResourceLocation();
    final byte[] data = buf.readByteArray();
    return new MishangucPacket(id, data);
  }
}
