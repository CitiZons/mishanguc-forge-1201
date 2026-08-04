package pers.solid.mishang.uc;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.util.TextBridge;
import net.minecraft.resources.ResourceLocation;
import pers.solid.mishang.uc.data.stubs.PacketByteBufs;
import pers.solid.mishang.uc.data.stubs.PacketSender;
import pers.solid.mishang.uc.data.stubs.ServerPlayNetworking;

/**
 * 迷上城建模组新增加的游戏规则。
 *
 * @see GameRules
 */
@ApiStatus.AvailableSince("1.0.0")
public final class MishangucRules {

  public static final GameRules.Key<EnumRule<ToolAccess>> FORCE_PLACING_TOOL_ACCESS = register("mishanguc:force_placing_tool_access", EnumRule.create(ToolAccess.CREATIVE_ONLY, (server, rule) -> sync(server, rule, 0)));

  public static final GameRules.Key<EnumRule<ToolAccess>> CARRYING_TOOL_ACCESS = register("mishanguc:carrying_tool_access", EnumRule.create(ToolAccess.ALL, (server, rule) -> sync(server, rule, 1)));

  public static final GameRules.Key<EnumRule<ToolAccess>> EXPLOSION_TOOL_ACCESS = register("mishanguc:explosion_tool_access", EnumRule.create(ToolAccess.ALL));

  private static void sync(MinecraftServer server, EnumRule<?> rule, int type) {
    // 规则变化时，同步到所有在线玩家。
    for (ServerPlayer serverPlayerEntity : server.getPlayerList().getPlayers()) {
      sync(rule, type, serverPlayerEntity);
    }
  }

  static void sync(GameRules.Value<?> rule, int type, ServerPlayer serverPlayerEntity) {
    final FriendlyByteBuf buf = PacketByteBufs.create();
    buf.writeShort(type);
    if (rule instanceof EnumRule<?>) {
      buf.writeEnum(((EnumRule<?>) rule).get());
    } else if (rule instanceof GameRules.BooleanValue) {
      buf.writeBoolean(((GameRules.BooleanValue) rule).get());
    }
    ServerPlayNetworking.send(serverPlayerEntity, new ResourceLocation("mishanguc", "rule_changed"), buf);
  }

  private static <T extends GameRules.Value<T>> GameRules.Key<T> register(String name, GameRules.Type<T> ruleType) {
    return GameRules.register(name, GameRules.Category.MISC, ruleType);
  }

  @OnlyIn(Dist.CLIENT)
  static void handle(Minecraft minecraft, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
    final short type = buf.readShort();
    final ToolAccess value = type == 0 || type == 1 ? buf.readEnum(ToolAccess.class) : null;
    final boolean booleanValue = type == 2 && buf.readBoolean();
    final double doubleValue = type == 3 ? buf.readDouble() : Double.NaN;
    minecraft.execute(() -> {
      switch (type) {
        case 0 -> MishangucClient.CLIENT_FORCE_PLACING_TOOL_ACCESS.set(value);
        case 1 -> MishangucClient.CLIENT_CARRYING_TOOL_ACCESS.set(value);
      }
    });
  }

  public enum ToolAccess implements StringRepresentable {
    ALL {
      @Override
      public boolean hasAccess(@Nullable Player player) {
        return true;
      }
    }, CREATIVE_ONLY {
      @Override
      public boolean hasAccess(@Nullable Player player) {
        return player != null && player.isCreative();
      }
    }, OP_ONLY {
      @Override
      public boolean hasAccess(@Nullable Player player) {
        return player != null && player.hasPermissions(2);
      }
    }, CREATIVE_OP_ONLY {
      @Override
      public boolean hasAccess(@Nullable Player player) {
        return player != null && player.isCreative() && player.hasPermissions(2);
      }
    };
    private final String name;

    ToolAccess() {
      this.name = name().toLowerCase();
    }

    @Override
    public String getSerializedName() {
      return name;
    }

    @Contract(pure = true)
    public abstract boolean hasAccess(@Nullable Player player);

    public boolean hasAccess(Player player, boolean warn) {
      final boolean hasAccess = hasAccess(player);
      if (warn && !hasAccess && !player.level().isClientSide) {
        player.displayClientMessage(createWarnText(), true);
      }
      return hasAccess;
    }

    public MutableComponent createWarnText() {
      return TextBridge.translatable("message.tool_access", TextBridge.translatable("message.tool_access." + getSerializedName())).withStyle(ChatFormatting.RED);
    }
  }
}
