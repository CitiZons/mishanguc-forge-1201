package pers.solid.mishang.uc.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import pers.solid.mishang.uc.data.stubs.ClientPlayNetworking;
import pers.solid.mishang.uc.data.stubs.PacketSender;

/**
 * 仅限客户端的封包分发器。从 {@link MishangucNetwork} 经 {@code DistExecutor} 调用，
 * 避免在专用服务端加载客户端类（{@link Minecraft} 等）。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientPacketDispatcher {
  private ClientPacketDispatcher() {
  }

  static void dispatch(ResourceLocation id, FriendlyByteBuf buf) {
    final ClientPlayNetworking.PlayChannelHandler handler = MishangucNetwork.CLIENT_RECEIVERS.get(id);
    if (handler == null) {
      return;
    }
    final Minecraft minecraft = Minecraft.getInstance();
    final PacketSender responseSender = (channel, responseBuf) -> MishangucNetwork.sendToServer(channel, responseBuf);
    handler.receive(minecraft, minecraft.getConnection(), buf, responseSender);
  }
}
