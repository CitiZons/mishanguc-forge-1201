package pers.solid.mishang.uc;

import com.google.gson.JsonParseException;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.*;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.block.StandingSignBlock;
import pers.solid.mishang.uc.blockentity.HungSignBlockEntity;
import pers.solid.mishang.uc.blockentity.StandingSignBlockEntity;
import pers.solid.mishang.uc.blockentity.WallSignBlockEntity;
import pers.solid.mishang.uc.screen.SignPreset;
import pers.solid.mishang.uc.screen.SignPresets;
import pers.solid.mishang.uc.text.TextContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import net.minecraft.commands.CommandSourceStack;
import pers.solid.mishang.uc.data.stubs.ClientCommandRegistrationCallback;

@OnlyIn(Dist.CLIENT)
public enum SignPresetCommand implements ClientCommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher, @NotNull CommandBuildContext registryAccess) {
    dispatcher.register(literal("mishanguc:signpreset")
        .then(literal("path")
            .executes(commandContext -> {
              commandContext.getSource().sendSuccess(() -> Component.translatable("message.mishanguc.signPreset.path", Component.literal(SignPresets.PATH.getFileName().toString()).withStyle(style -> style
                  .withUnderlined(true)
                  .withColor(ChatFormatting.YELLOW)
                  .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("message.mishanguc.signPreset.save.success.click_to_open")))
                  .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, SignPresets.PATH.toString())))), false);
              return 1;
            }))
        .then(literal("list")
            .executes(commandContext -> {
              final Collection<SignPreset> values = SignPresets.streamValues().toList();
              commandContext.getSource().sendSuccess(() -> Component.translatable("message.mishanguc.signPreset.list", ComponentUtils.formatList(values, ComponentUtils.DEFAULT_SEPARATOR, signPreset -> signPreset.name().copy().withStyle(ChatFormatting.YELLOW).withStyle(style -> {
                final MutableComponent text = Component.empty();
                final Component description = signPreset.description();
                if (description != null) {
                  text.append(description);
                  text.append(CommonComponents.NEW_LINE);
                }
                return style
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, text.append(Component.translatable("message.mishanguc.signPreset.list.id_info", signPreset.name()).withStyle(ChatFormatting.GRAY))));
              }))), false);
              return values.size();
            }))
        .then(literal("reload")
            .executes(commandContext -> {
              final CommandSourceStack source = commandContext.getSource();
              final Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
              source.sendSuccess(() -> Component.translatable("message.mishanguc.signPreset.list.reload"), false);
              final Thread thread = new Thread(() -> {
                final int i = SignPresets.loadAll();
                if (i >= 0) {
                  minecraft.execute(() -> source.sendSuccess(() -> Component.translatable("message.mishanguc.signPreset.list.reload.success", i), false));
                } else {
                  minecraft.execute(() -> source.sendSuccess(() -> Component.translatable("message.mishanguc.signPreset.list.reload.error", Component.literal(SignPresets.PATH.toString()).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, SignPresets.PATH.toString())))), false));
                }
              });
              thread.start();
              return 1;
            }))
        .then(literal("save")
            .then(argument("id", string())
                .executes(commandContext -> executeSave(commandContext, null))
                .then(argument("args", CompoundTagArgument.compoundTag())
                    .executes(commandContext -> executeSave(commandContext, CompoundTagArgument.getCompoundTag(commandContext, "args"))))))
        .then(literal("delete")
            .then(argument("id", string()).suggests(SignPresets.SUGGEST_KEYS)
                .executes(commandContext -> executeDelete(commandContext, true))))
        .then(literal("reset")
            .executes(SignPresetCommand::executeResetAll)
            .then(argument("id", string()).suggests(SignPresets.SUGGEST_KEYS_AND_BUILTIN)
                .executes(commandContext -> executeDelete(commandContext, false)))));
  }

  private static int executeSave(CommandContext<CommandSourceStack> commandContext, @Nullable CompoundTag args) throws CommandSyntaxException {
    final boolean force;
    final int order;
    final int initialFocus;

    if (args != null) {
      force = args.getBoolean("force");
      order = args.getInt("order");
      initialFocus = args.getInt("initial_focus");
    } else {
      force = false;
      order = 0;
      initialFocus = 0;
    }

    final CommandSourceStack source = commandContext.getSource();
    final HitResult hitResult = net.minecraft.client.Minecraft.getInstance().hitResult;
    if (!(hitResult instanceof BlockHitResult blockHitResult)) {
      source.sendFailure(Component.translatable("message.mishanguc.signPreset.save.not_sign"));
      return -1;
    }
    final ClientLevel world = net.minecraft.client.Minecraft.getInstance().level;
    final BlockPos blockPos = blockHitResult.getBlockPos();
    final BlockEntity blockEntity = world.getBlockEntity(blockPos);

    final List<TextContext> textContexts;
    if (blockEntity instanceof WallSignBlockEntity wallSignBlockEntity) {
      textContexts = wallSignBlockEntity.textContexts;
    } else if (blockEntity instanceof StandingSignBlockEntity standingSignBlockEntity) {
      final Boolean hitSide = StandingSignBlock.getHitSide(world.getBlockState(blockPos), blockHitResult);
      textContexts = hitSide == null ? null : standingSignBlockEntity.getTextsOnSide(hitSide);
    } else if (blockEntity instanceof HungSignBlockEntity hungSignBlockEntity) {
      textContexts = hungSignBlockEntity.texts.get(blockHitResult.getDirection());
    } else {
      source.sendFailure(Component.translatable("message.mishanguc.signPreset.save.not_sign"));
      return -1;
    }

    if (textContexts == null) {
      source.sendFailure(Component.translatable("message.mishanguc.signPreset.save.fail.no_text"));
      return -2;
    } else if (!force && textContexts.isEmpty()) {
      source.sendFailure(Component.translatable("message.mishanguc.signPreset.save.fail.empty_text"));
      return -1;
    }

    final List<TextContext> textContextsCopy = textContexts.stream().map(TextContext::clone).toList();
    final String id = getString(commandContext, "id");
    final Optional<Component> name;
    if (args == null || !args.contains("name")) {
      name = Optional.empty();
    } else {
      try {
        name = Optional.of(Component.Serializer.fromJson(new StringReader(args.getString("name"))));
      } catch (JsonParseException e) {
        throw ComponentArgument.ERROR_INVALID_JSON.create(e.getMessage());
      }
    }
    final Optional<Component> description;
    if (args == null || !args.contains("description")) {
      description = Optional.empty();
    } else {
      try {
        description = Optional.of(Component.Serializer.fromJson(new StringReader(args.getString("description"))));
      } catch (JsonParseException e) {
        throw ComponentArgument.ERROR_INVALID_JSON.create(e.getMessage());
      }
    }
    if (initialFocus < 0 || (initialFocus >= textContextsCopy.size() && initialFocus > 0)) {
      source.sendFailure(Component.translatable("message.mishanguc.signPreset.save.initial_focus_invalid", initialFocus, textContextsCopy.size()));
      return -1;
    }
    final Thread thread = new Thread(() -> {
      final SignPreset.Info info = new SignPreset.Info(order, name, description, textContextsCopy, initialFocus);
      info.save(id, source, force);
    });
    thread.start();
    source.sendSuccess(() -> Component.translatable("message.mishanguc.signPreset.save.start"), false);
    return 1;
  }

  private static int executeDelete(CommandContext<CommandSourceStack> commandContext, boolean hideIdBuiltin) {
    final CommandSourceStack source = commandContext.getSource();
    final String id = getString(commandContext, "id");
    final SignPreset signPreset = SignPresets.getOrBuiltin(id);
    if (signPreset == null) {
      source.sendFailure(Component.translatable("message.mishanguc.signPreset.delete.not_exist", id));
      return -1;
    }

    final Thread thread = new Thread(() -> {
      final SignPreset.Info info = signPreset.asInfo();
      info.delete(id, source, hideIdBuiltin);
    });
    thread.start();
    source.sendSuccess(() -> Component.translatable("message.mishanguc.signPreset.delete.start", signPreset.name()), false);
    return 1;
  }

  private static int executeResetAll(CommandContext<CommandSourceStack> commandContext) {
    final CommandSourceStack source = commandContext.getSource();
    final Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
    final Thread thread = new Thread(() -> {
      try (final Stream<Path> stream = Files.walk(SignPresets.PATH)) {
        stream
            .peek(System.out::println)
            .filter(Files::isRegularFile)
            .filter(path -> path.getFileName().toString().endsWith(".json"))
            .forEach(path -> {
              try {
                Files.delete(path);
              } catch (IOException e) {
                SignPresets.LOGGER.error("Failed to delete sign preset {}", path, e);
              }
            });
        minecraft.execute(() -> source.sendSuccess(() -> Component.translatable("message.mishanguc.signPreset.reset.success"), false));
        SignPresets.resetToBuiltin();
      } catch (IOException e) {
        SignPresets.LOGGER.error("Failed to delete sign presets", e);
        minecraft.execute(() -> source.sendFailure(Component.translatable("message.mishanguc.signPreset.reset.fail.unknown")));
      }
    });
    thread.start();
    source.sendSuccess(() -> Component.translatable("message.mishanguc.signPreset.reset.start"), false);
    return 1;
  }
}
