package pers.solid.mishang.uc.util;

import net.minecraft.network.chat.*;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;

/**
 * 实用类，用于在不同版本之间减少代码差异。
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ApiStatus.AvailableSince("0.2.4")
@ApiStatus.NonExtendable
public interface TextBridge extends Component {
  static MutableComponent literal(String string) {
    return Component.literal(string);
  }

  static MutableComponent translatable(String key) {
    return Component.translatable(key);
  }

  static MutableComponent translatable(String key, Object... args) {
    return Component.translatable(key, args);
  }

  static MutableComponent empty() {
    return Component.empty();
  }

  static MutableComponent keybind(String string) {
    return Component.keybind(string);
  }

  static MutableComponent nbt(String rawPath, boolean interpret, Optional<Component> separator, net.minecraft.network.chat.contents.DataSource dataSource) {
    return Component.nbt(rawPath, interpret, separator, dataSource);
  }

  static MutableComponent score(String name, String objective) {
    return Component.score(name, objective);
  }

  static MutableComponent selector(String pattern, Optional<Component> separator) {
    return Component.selector(pattern, separator);
  }

  static boolean isEmpty(Component text) {
    final ComponentContents content = text.getContents();
    return content == ComponentContents.EMPTY || content instanceof final LiteralContents literalComponentContents && literalComponentContents.text().isEmpty();
  }
}
