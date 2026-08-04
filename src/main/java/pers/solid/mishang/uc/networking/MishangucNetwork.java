package pers.solid.mishang.uc.networking;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import pers.solid.mishang.uc.data.stubs.ClientPlayNetworking;
import pers.solid.mishang.uc.data.stubs.PacketSender;
import pers.solid.mishang.uc.data.stubs.ServerPlayNetworking;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 本模组的网络核心。用单个 Forge {@link SimpleChannel} 承载所有逻辑频道，
 * 以模拟 Fabric 的 {@code ClientPlayNetworking}/{@code ServerPlayNetworking} 多频道 API。
 *
 * <p>{@link pers.solid.mishang.uc.data.stubs.ClientPlayNetworking} 与
 * {@link pers.solid.mishang.uc.data.stubs.ServerPlayNetworking} 这两个类（保留原 Fabric API 形状）
 * 直接委托到本类，因此所有发送/接收调用点无需改动。
 *
 * <p>线程模型与 Fabric 一致：接收处理器在网络线程被同步调用、由处理器自身通过
 * {@code server.execute(...)} / {@code minecraft.execute(...)} 切换到主线程。
 * 收到的数据被包成一份独立的字节数组，因此在处理器内同步读取是安全的。
 */
public final class MishangucNetwork {
  private static final String PROTOCOL_VERSION = "1";

  public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
      new ResourceLocation("mishanguc", "main"),
      () -> PROTOCOL_VERSION,
      PROTOCOL_VERSION::equals,
      PROTOCOL_VERSION::equals);

  static final Map<ResourceLocation, ServerPlayNetworking.PlayChannelHandler> SERVER_RECEIVERS = new ConcurrentHashMap<>();
  static final Map<ResourceLocation, ClientPlayNetworking.PlayChannelHandler> CLIENT_RECEIVERS = new ConcurrentHashMap<>();

  private MishangucNetwork() {
  }

  /** 在 {@code FMLCommonSetupEvent} 中调用一次，注册封包编解码与处理。 */
  public static void register() {
    CHANNEL.registerMessage(
        0,
        MishangucPacket.class,
        MishangucPacket::encode,
        MishangucPacket::decode,
        MishangucNetwork::handle);
  }

  public static void registerServerReceiver(ResourceLocation channel, ServerPlayNetworking.PlayChannelHandler handler) {
    SERVER_RECEIVERS.put(channel, handler);
  }

  public static void registerClientReceiver(ResourceLocation channel, ClientPlayNetworking.PlayChannelHandler handler) {
    CLIENT_RECEIVERS.put(channel, handler);
  }

  /** 客户端 → 服务端。 */
  public static void sendToServer(ResourceLocation channel, FriendlyByteBuf buf) {
    CHANNEL.sendToServer(new MishangucPacket(channel, toBytes(buf)));
  }

  /** 服务端 → 指定客户端。 */
  public static void sendToClient(ServerPlayer player, ResourceLocation channel, FriendlyByteBuf buf) {
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MishangucPacket(channel, toBytes(buf)));
  }

  private static byte[] toBytes(FriendlyByteBuf buf) {
    final byte[] bytes = new byte[buf.readableBytes()];
    buf.getBytes(buf.readerIndex(), bytes);
    return bytes;
  }

  private static void handle(MishangucPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
    final NetworkEvent.Context context = contextSupplier.get();
    final FriendlyByteBuf payload = new FriendlyByteBuf(Unpooled.wrappedBuffer(packet.data()));
    // getSender() 在服务端收包时为发送的玩家，客户端收包时为 null，以此判断收包方向。
    final ServerPlayer sender = context.getSender();
    if (sender != null) {
      final ServerPlayNetworking.PlayChannelHandler handler = SERVER_RECEIVERS.get(packet.id());
      if (handler != null) {
        final MinecraftServer server = sender.getServer();
        final PacketSender responseSender = (channel, responseBuf) -> sendToClient(sender, channel, responseBuf);
        handler.receive(server, sender, sender.connection, payload, responseSender);
      }
    } else {
      // 客户端：在仅限客户端的类里分发，避免在专用服务端加载客户端类。
      DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketDispatcher.dispatch(packet.id(), payload));
    }
    context.setPacketHandled(true);
  }
}
