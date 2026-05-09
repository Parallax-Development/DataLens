package dev.darkblade.datalens.ui.chat;

import dev.darkblade.datalens.model.DataNode;
import dev.darkblade.datalens.model.diff.DataDiff;
import dev.darkblade.datalens.ui.gui.NodeRenderer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Renders {@link DataNode} trees and diff results as formatted chat messages.
 * Used by {@code /data export} and {@code /data diff}.
 */
public final class ChatRenderer {

    private static final int MAX_DEPTH = 6;

    /** Prints a DataNode tree to the sender's chat, up to {@link #MAX_DEPTH} levels. */
    public void renderTree(CommandSender sender, DataNode root) {
        sender.sendMessage(Component.text("══ DataLens: " + root.getKey() + " ══")
                .color(NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.BOLD, true));
        renderNode(sender, root, "", 0);
    }

    private void renderNode(CommandSender sender, DataNode node, String indent, int depth) {
        if (depth > MAX_DEPTH) {
            sender.sendMessage(Component.text(indent + "  ... (truncated)").color(NamedTextColor.DARK_GRAY));
            return;
        }

        Component prefix = Component.text(indent + "├ ").color(NamedTextColor.DARK_GRAY);
        Component key = Component.text(node.getKey()).color(NodeRenderer.colorFor(node.getType()))
                .decoration(TextDecoration.BOLD, true);
        Component type = Component.text(" [" + node.getType().label() + "]").color(NamedTextColor.DARK_GRAY);

        if (node.getType().isContainer()) {
            sender.sendMessage(prefix.append(key).append(type)
                    .append(Component.text(" (" + node.childCount() + ")").color(NamedTextColor.GRAY)));
            for (DataNode child : node.getChildren()) {
                renderNode(sender, child, indent + "  ", depth + 1);
            }
        } else {
            String valStr = String.valueOf(node.getValue());
            if (valStr.length() > 50) valStr = valStr.substring(0, 47) + "...";
            Component value = Component.text(": " + valStr).color(NamedTextColor.WHITE);
            sender.sendMessage(prefix.append(key).append(type).append(value));
        }
    }

    // ── Diff rendering ────────────────────────────────────────────────────────

    /** Prints a diff result list to chat. */
    public void renderDiff(CommandSender sender, List<DataDiff> diffs) {
        if (diffs.isEmpty()) {
            sender.sendMessage(Component.text("No differences found.").color(NamedTextColor.GREEN));
            return;
        }
        sender.sendMessage(Component.text("══ DataLens Diff (" + diffs.size() + " changes) ══")
                .color(NamedTextColor.DARK_AQUA));
        for (DataDiff diff : diffs) {
            Component line = switch (diff.getDiffType()) {
                case ADDED -> Component.text("+ " + diff.getPath() + " = " + diff.getNewValue())
                        .color(NamedTextColor.GREEN);
                case REMOVED -> Component.text("- " + diff.getPath() + " (was " + diff.getOldValue() + ")")
                        .color(NamedTextColor.RED);
                case CHANGED -> Component.text("~ " + diff.getPath()
                        + ": " + diff.getOldValue() + " → " + diff.getNewValue())
                        .color(NamedTextColor.YELLOW);
            };
            sender.sendMessage(line.decoration(TextDecoration.ITALIC, false));
        }
    }
}
