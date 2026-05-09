package dev.darkblade.datalens.command;

import dev.darkblade.datalens.core.diff.DiffService;
import dev.darkblade.datalens.core.edit.EditService;
import dev.darkblade.datalens.core.serialize.SerializationService;
import dev.darkblade.datalens.core.session.PlayerSession;
import dev.darkblade.datalens.core.session.SessionService;
import dev.darkblade.datalens.model.DataNode;
import dev.darkblade.datalens.model.diff.DataDiff;
import dev.darkblade.datalens.security.PermissionGuard;
import dev.darkblade.datalens.service.DataLensServiceLocator;
import dev.darkblade.datalens.ui.chat.ChatRenderer;
import dev.darkblade.datalens.util.PathCompleter;
import dev.darkblade.datalens.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * {@code /data <subcommand> [args...]} — manage the currently inspected object.
 *
 * <ul>
 *   <li>{@code set <path> <value>} — edit a primitive value</li>
 *   <li>{@code remove <path>} — delete a node</li>
 *   <li>{@code export [json|yaml]} — dump current data</li>
 *   <li>{@code diff} — compare live vs working copy</li>
 * </ul>
 */
public final class DataCommand implements CommandExecutor, TabCompleter {

    private final ChatRenderer chatRenderer = new ChatRenderer();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player;
        try {
            PermissionGuard.require(sender, PermissionGuard.INSPECT);
            player = PermissionGuard.requirePlayer(sender);
        } catch (PermissionGuard.PermissionException ex) {
            TextUtil.audience(sender).sendMessage(Component.text(ex.getMessage()).color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) { sendUsage(sender); return true; }

        DataLensServiceLocator svc = DataLensServiceLocator.get();
        SessionService sessions = svc.sessions();

        Optional<PlayerSession> sessionOpt = sessions.get(player.getUniqueId());
        if (sessionOpt.isEmpty()) {
            TextUtil.audience(sender).sendMessage(Component.text("No active inspection. Use /inspect first.")
                    .color(NamedTextColor.YELLOW));
            return true;
        }
        PlayerSession session = sessionOpt.get();

        switch (args[0].toLowerCase()) {
            case "set"    -> handleSet(player, session, svc.editor(), args);
            case "remove" -> handleRemove(player, session, svc.editor(), args);
            case "export" -> handleExport(player, session, svc.serializer(), args);
            case "diff"   -> handleDiff(player, session, svc.differ());
            default       -> sendUsage(sender);
        }
        return true;
    }

    // ── Subcommand handlers ───────────────────────────────────────────────────

    private void handleSet(Player player, PlayerSession session, EditService editor, String[] args) {
        try { PermissionGuard.require(player, PermissionGuard.EDIT); }
        catch (PermissionGuard.PermissionException ex) {
            TextUtil.audience(player).sendMessage(Component.text(ex.getMessage()).color(NamedTextColor.RED)); return;
        }
        if (args.length < 3) {
            TextUtil.audience(player).sendMessage(Component.text("Usage: /data set <path> <value>").color(NamedTextColor.RED));
            return;
        }
        String path = args[1];
        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        try {
            editor.setValue(session.getTarget(), path, value, player.getName());
            session.refresh();
            TextUtil.audience(player).sendMessage(Component.text("✔ Set ").color(NamedTextColor.GREEN)
                    .append(Component.text(path).color(NamedTextColor.AQUA))
                    .append(Component.text(" = " + value).color(NamedTextColor.WHITE)));
        } catch (EditService.EditException ex) {
            TextUtil.audience(player).sendMessage(Component.text("✘ " + ex.getMessage()).color(NamedTextColor.RED));
        }
    }

    private void handleRemove(Player player, PlayerSession session, EditService editor, String[] args) {
        try { PermissionGuard.require(player, PermissionGuard.EDIT); }
        catch (PermissionGuard.PermissionException ex) {
            TextUtil.audience(player).sendMessage(Component.text(ex.getMessage()).color(NamedTextColor.RED)); return;
        }
        if (args.length < 2) {
            TextUtil.audience(player).sendMessage(Component.text("Usage: /data remove <path>").color(NamedTextColor.RED));
            return;
        }
        try {
            editor.remove(session.getTarget(), args[1], player.getName());
            session.refresh();
            TextUtil.audience(player).sendMessage(Component.text("✔ Removed ").color(NamedTextColor.GREEN)
                    .append(Component.text(args[1]).color(NamedTextColor.AQUA)));
        } catch (EditService.EditException ex) {
            TextUtil.audience(player).sendMessage(Component.text("✘ " + ex.getMessage()).color(NamedTextColor.RED));
        }
    }

    private void handleExport(Player player, PlayerSession session,
                               SerializationService serializer, String[] args) {
        String format = args.length >= 2 ? args[1].toLowerCase() : "json";
        String output = switch (format) {
            case "yaml" -> serializer.toYaml(session.getTarget().getRoot());
            default     -> serializer.toJson(session.getTarget().getRoot());
        };
        TextUtil.audience(player).sendMessage(Component.text("── DataLens Export (" + format.toUpperCase() + ") ──")
                .color(NamedTextColor.DARK_AQUA));
        // Split large outputs into multiple messages
        for (String line : output.split("\n")) {
            TextUtil.audience(player).sendMessage(Component.text(line).color(NamedTextColor.WHITE));
        }
    }

    private void handleDiff(Player player, PlayerSession session, DiffService differ) {
        List<DataDiff> diffs = differ.diff(session.getTarget().getRoot(), session.getWorkingCopy());
        chatRenderer.renderDiff(player, diffs);
    }

    // ── TabComplete ───────────────────────────────────────────────────────────

    private static final List<String> SUBCOMMANDS = List.of("set", "remove", "export", "diff");
    private static final List<String> EXPORT_FORMATS = List.of("json", "yaml");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return List.of();

        // arg[0] — subcommand
        if (args.length == 1) {
            return PathCompleter.filter(SUBCOMMANDS, args[0]);
        }

        String sub = args[0].toLowerCase();

        // arg[1] — path (for set / remove) or format (for export)
        if (args.length == 2) {
            return switch (sub) {
                case "set", "remove" -> pathsForPlayer(player, args[1]);
                case "export"        -> PathCompleter.filter(EXPORT_FORMATS, args[1]);
                default              -> List.of();
            };
        }

        // arg[2] — value (for set <path> <value>)
        if (args.length == 3 && sub.equals("set")) {
            return valuesForPlayer(player, args[1], args[2]);
        }

        return List.of();
    }

    /**
     * Returns path completions from the player's active session.
     * Falls back to an empty list if the player has no session.
     */
    private List<String> pathsForPlayer(Player player, String prefix) {
        return DataLensServiceLocator.get()
                .sessions()
                .get(player.getUniqueId())
                .map(session -> PathCompleter.paths(session.getTarget().getRoot(), prefix))
                .orElse(List.of());
    }

    /**
     * Returns value completions based on the DataType of the node at {@code path}.
     */
    private List<String> valuesForPlayer(Player player, String path, String prefix) {
        return DataLensServiceLocator.get()
                .sessions()
                .get(player.getUniqueId())
                .map(session -> {
                    DataNode root = session.getTarget().getRoot();
                    return PathCompleter.values(root, path, prefix);
                })
                .orElse(List.of());
    }

    private void sendUsage(CommandSender sender) {
        TextUtil.audience(sender).sendMessage(Component.text("DataLens commands:").color(NamedTextColor.DARK_AQUA));
        TextUtil.audience(sender).sendMessage(Component.text("  /data set <path> <value>").color(NamedTextColor.AQUA));
        TextUtil.audience(sender).sendMessage(Component.text("  /data remove <path>").color(NamedTextColor.AQUA));
        TextUtil.audience(sender).sendMessage(Component.text("  /data export [json|yaml]").color(NamedTextColor.AQUA));
        TextUtil.audience(sender).sendMessage(Component.text("  /data diff").color(NamedTextColor.AQUA));
    }
}
