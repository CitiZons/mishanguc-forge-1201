package pers.solid.mishang.uc;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * A custom {@link GameRules.Value} implementation that stores an enum value.
 * <p>
 * This replaces the Fabric API's {@code EnumRule} for use in Forge.
 *
 * @param <T> the enum type, which must implement {@link StringRepresentable}
 */
public class EnumRule<T extends Enum<T> & StringRepresentable> extends GameRules.Value<EnumRule<T>> {

  private final Class<T> enumClass;
  private T value;

  public EnumRule(GameRules.Type<EnumRule<T>> type, T defaultValue) {
    super(type);
    this.enumClass = defaultValue.getDeclaringClass();
    this.value = defaultValue;
  }

  /**
   * Creates a {@link GameRules.Type} for an enum game rule with no change callback.
   */
  public static <T extends Enum<T> & StringRepresentable> GameRules.Type<EnumRule<T>> create(T defaultValue) {
    return create(defaultValue, (server, rule) -> {});
  }

  /**
   * Creates a {@link GameRules.Type} for an enum game rule with a change callback.
   */
  public static <T extends Enum<T> & StringRepresentable> GameRules.Type<EnumRule<T>> create(T defaultValue, BiConsumer<MinecraftServer, EnumRule<T>> changeCallback) {
    return new GameRules.Type<>(
        StringArgumentType::word,
        type -> new EnumRule<>(type, defaultValue),
        changeCallback,
        (visitor, key, type) -> visitor.visit(key, type)
    );
  }

  @Override
  protected void updateFromArgument(CommandContext<CommandSourceStack> context, String paramName) {
    String input = StringArgumentType.getString(context, paramName);
    T parsed = parse(input);
    if (parsed != null) {
      this.value = parsed;
    }
  }

  @Override
  protected void deserialize(String value) {
    T parsed = parse(value);
    if (parsed != null) {
      this.value = parsed;
    }
  }

  @Override
  public String serialize() {
    return this.value.getSerializedName();
  }

  @Override
  public int getCommandResult() {
    return this.value.ordinal();
  }

  @Override
  protected EnumRule<T> getSelf() {
    return this;
  }

  @Override
  protected EnumRule<T> copy() {
    EnumRule<T> copy = new EnumRule<>(this.type, this.value);
    return copy;
  }

  @Override
  public void setFrom(EnumRule<T> rule, @Nullable MinecraftServer server) {
    this.value = rule.value;
    this.onChanged(server);
  }

  /**
   * Gets the current enum value.
   */
  public T get() {
    return this.value;
  }

  /**
   * Gets the current enum value (alias for {@link #get()}).
   */
  public T getValue() {
    return this.value;
  }

  /**
   * Sets the enum value.
   */
  public void set(T value) {
    this.value = value;
  }

  @Nullable
  private T parse(String input) {
    T[] constants = enumClass.getEnumConstants();
    return Arrays.stream(constants)
        .filter(c -> c.getSerializedName().equalsIgnoreCase(input))
        .findFirst()
        .orElse(null);
  }
}
