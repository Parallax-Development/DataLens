package dev.darkblade.datalens.ui.gui;

import dev.darkblade.datalens.model.DataNode;
import dev.darkblade.datalens.model.DataType;
import dev.darkblade.datalens.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Converts a {@link DataNode} into an {@link ItemStack} for display in the Inspector GUI.
 *
 * <ul>
 *   <li>Item name = node key, coloured by DataType</li>
 *   <li>Item lore = type label + value preview</li>
 *   <li>Item material encodes container vs primitive</li>
 * </ul>
 */
public final class NodeRenderer {

    // ── Material mapping ──────────────────────────────────────────────────────

    private static final Material COMPOUND_MATERIAL = Material.CHEST;
    private static final Material LIST_MATERIAL = Material.BOOKSHELF;
    private static final Material STRING_MATERIAL = Material.NAME_TAG;
    private static final Material NUMBER_MATERIAL = Material.GOLD_NUGGET;
    private static final Material BOOLEAN_MATERIAL = Material.LEVER;
    private static final Material ARRAY_MATERIAL = Material.PAPER;
    private static final Material UUID_MATERIAL = Material.ENDER_PEARL;
    private static final Material UNKNOWN_MATERIAL = Material.BARRIER;

    // ── Colour mapping ────────────────────────────────────────────────────────

    public static TextColor colorFor(DataType type) {
        return switch (type) {
            case STRING  -> TextColor.color(0x7EC8E3);  // light blue
            case INT, SHORT, BYTE -> TextColor.color(0xFFD700);  // gold
            case LONG    -> TextColor.color(0xFF8C00);  // dark orange
            case DOUBLE, FLOAT -> TextColor.color(0x98FB98);  // pale green
            case BOOLEAN -> TextColor.color(0xFF69B4);  // hot pink
            case BYTE_ARRAY, INT_ARRAY, LONG_ARRAY -> TextColor.color(0xDDA0DD);  // plum
            case LIST    -> TextColor.color(0x87CEEB);  // sky blue
            case COMPOUND -> NamedTextColor.YELLOW;
            case UUID    -> TextColor.color(0xADD8E6);  // light blue-grey
            case UNKNOWN -> NamedTextColor.DARK_GRAY;
        };
    }

    private static Material materialFor(DataType type) {
        if (type == DataType.COMPOUND) return COMPOUND_MATERIAL;
        if (type == DataType.LIST) return LIST_MATERIAL;
        if (type == DataType.STRING) return STRING_MATERIAL;
        if (type == DataType.BOOLEAN) return BOOLEAN_MATERIAL;
        if (type == DataType.UUID) return UUID_MATERIAL;
        if (type == DataType.UNKNOWN) return UNKNOWN_MATERIAL;
        if (type.isArray()) return ARRAY_MATERIAL;
        return NUMBER_MATERIAL;
    }

    // ── Public render ──────────────────────────────────────────────────────────

    /**
     * Creates a display ItemStack for the given DataNode.
     */
    public static ItemStack render(DataNode node) {
        ItemStack item = new ItemStack(materialFor(node.getType()));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Name: key in type colour, bold, no italic
        Component name = Component.text(node.getKey())
                .color(colorFor(node.getType()))
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false);
        meta.setDisplayName(TextUtil.legacy(name));

        // Lore
        meta.setLore(TextUtil.legacyList(buildLore(node)));

        item.setItemMeta(meta);
        return item;
    }

    private static List<Component> buildLore(DataNode node) {
        Component typeLine = Component.text("[" + node.getType().label() + "]")
                .color(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false);

        if (node.getType() == DataType.COMPOUND) {
            Component childCount = Component.text(node.childCount() + " keys")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false);
            Component hint = Component.text("Left-click to expand")
                    .color(NamedTextColor.DARK_GREEN)
                    .decoration(TextDecoration.ITALIC, true);
            return List.of(typeLine, childCount, Component.empty(), hint);
        }

        if (node.getType() == DataType.LIST) {
            Component childCount = Component.text(node.childCount() + " elements")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false);
            Component hint = Component.text("Left-click to expand")
                    .color(NamedTextColor.DARK_GREEN)
                    .decoration(TextDecoration.ITALIC, true);
            return List.of(typeLine, childCount, Component.empty(), hint);
        }

        // Primitive — show value
        String valueStr = valueString(node);
        Component valueLine = Component.text(valueStr)
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false);
        Component editHint = Component.text("Shift+Click to edit  |  Right-Click to delete")
                .color(NamedTextColor.DARK_GREEN)
                .decoration(TextDecoration.ITALIC, true);
        return List.of(typeLine, valueLine, Component.empty(), editHint);
    }

    private static String valueString(DataNode node) {
        Object val = node.getValue();
        if (val == null) return "<null>";
        if (val instanceof byte[] a) return Arrays.toString(a);
        if (val instanceof int[] a) return Arrays.toString(a);
        if (val instanceof long[] a) return Arrays.toString(a);
        String s = val.toString();
        return s.length() > 60 ? s.substring(0, 57) + "..." : s;
    }

    // ── Special navigation items ───────────────────────────────────────────────

    public static ItemStack backButton() {
        return namedItem(Material.ARROW, Component.text("← Back")
                .color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
    }

    public static ItemStack prevPage() {
        return namedItem(Material.SPECTRAL_ARROW, Component.text("◄ Previous page")
                .color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
    }

    public static ItemStack nextPage() {
        return namedItem(Material.SPECTRAL_ARROW, Component.text("Next page ►")
                .color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
    }

    public static ItemStack closeButton() {
        return namedItem(Material.BARRIER, Component.text("✕ Close")
                .color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
    }

    public static ItemStack exportButton() {
        return namedItem(Material.WRITABLE_BOOK, Component.text("⬇ Export JSON")
                .color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
    }

    public static ItemStack filler() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = glass.getItemMeta();
        if (m != null) { m.setDisplayName(TextUtil.legacy(Component.text(" "))); glass.setItemMeta(m); }
        return glass;
    }

    private static ItemStack namedItem(Material mat, Component name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(TextUtil.legacy(name)); item.setItemMeta(meta); }
        return item;
    }

    /** Creates a gray stained glass pane with the given display name. */
    public static ItemStack namedPane(Component name) {
        return namedItem(Material.GRAY_STAINED_GLASS_PANE, name);
    }
}
