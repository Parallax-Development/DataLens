package dev.darkblade.datalens.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Objects;

/**
 * Centralised text utilities for cross-platform Adventure support.
 *
 * <p>On <strong>Paper</strong>, the native Adventure implementation is used.
 * On <strong>Spigot</strong>, the shaded {@code adventure-platform-bukkit} adapter
 * translates Components to legacy format automatically.
 *
 * <p>Must be initialised via {@link #init(Plugin)} before first use and
 * closed via {@link #close()} on plugin disable.
 */
public final class TextUtil {

    private static BukkitAudiences audiences;
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    private TextUtil() {}

    /**
     * Initialises the BukkitAudiences instance. Call once during {@code onEnable()}.
     */
    public static void init(Plugin plugin) {
        audiences = BukkitAudiences.create(Objects.requireNonNull(plugin));
    }

    /**
     * Closes the BukkitAudiences instance. Call during {@code onDisable()}.
     */
    public static void close() {
        if (audiences != null) {
            audiences.close();
            audiences = null;
        }
    }

    /**
     * Wraps a Bukkit {@link CommandSender} as an Adventure {@link Audience}.
     * Use this instead of {@code sender.sendMessage(Component)} for Spigot compatibility.
     */
    public static Audience audience(CommandSender sender) {
        return audiences.sender(sender);
    }

    /**
     * Serializes a {@link Component} to a legacy §-coded string.
     * Use for APIs that only accept {@code String} (e.g. {@code ItemMeta.setDisplayName},
     * {@code Bukkit.createInventory} title, etc.)
     */
    public static String legacy(Component component) {
        return LEGACY.serialize(component);
    }

    /**
     * Converts a list of Components to a list of legacy §-coded strings.
     * Use for {@code ItemMeta.setLore(List<String>)}.
     */
    public static List<String> legacyList(List<Component> components) {
        return components.stream()
                .map(LEGACY::serialize)
                .toList();
    }
}
