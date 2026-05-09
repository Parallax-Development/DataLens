package dev.darkblade.datalens.command;

import dev.darkblade.datalens.core.inspect.InspectorService;
import dev.darkblade.datalens.core.session.PlayerSession;
import dev.darkblade.datalens.core.session.SessionService;
import dev.darkblade.datalens.model.InspectableObject;
import dev.darkblade.datalens.security.PermissionGuard;
import dev.darkblade.datalens.service.DataLensServiceLocator;
import dev.darkblade.datalens.ui.gui.InspectorGui;
import dev.darkblade.datalens.util.PathCompleter;
import dev.darkblade.datalens.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.List;

/**
 * {@code /inspect [player]} — detects the block or entity the player is looking at
 * and opens the DataLens GUI.
 *
 * <p>Modes:
 * <ul>
 *   <li><b>No arguments</b> — visual targeting via {@link org.bukkit.World#rayTraceEntities} /
 *       {@link Player#getTargetBlockExact}. Works for any entity including other players.</li>
 *   <li><b>{@code /inspect <name>}</b> — looks up the named online player directly and
 *       opens their inspection without needing line-of-sight.</li>
 * </ul>
 *
 * <p>Detection priority (no-arg mode):
 * <ol>
 *   <li>{@link org.bukkit.World#rayTraceEntities} — Bukkit-standard raytrace that correctly
 *       handles entity bounding boxes and respects solid-block occlusion.</li>
 *   <li>{@link Player#getTargetBlockExact(int)} — first non-transparent block hit.</li>
 * </ol>
 */
public final class InspectCommand implements CommandExecutor, TabCompleter {

    /** Maximum look distance in blocks (from config, default 5). */
    private final int maxDistance;

    public InspectCommand(double maxDistance) {
        this.maxDistance = (int) Math.ceil(maxDistance);
    }

    // ── Command ───────────────────────────────────────────────────────────────

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

        DataLensServiceLocator svc = DataLensServiceLocator.get();
        InspectorService inspector = svc.inspector();
        SessionService sessions = svc.sessions();

        // ── Mode A: /inspect <playername> ─────────────────────────────────────
        if (args.length >= 1) {
            String targetName = args[0];
            Player targetPlayer = Bukkit.getPlayerExact(targetName);

            if (targetPlayer == null) {
                // Try case-insensitive partial match
                targetPlayer = Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.getName().equalsIgnoreCase(targetName))
                        .findFirst()
                        .orElse(null);
            }

            if (targetPlayer == null) {
                TextUtil.audience(sender).sendMessage(Component.text("Player not found or offline: ")
                        .color(NamedTextColor.RED)
                        .append(Component.text(targetName).color(NamedTextColor.WHITE)));
                return true;
            }

            InspectableObject obj = inspector.inspect(targetPlayer);
            openGui(player, obj, sessions);
            return true;
        }

        // ── Mode B: /inspect (visual targeting) ───────────────────────────────

        // 1. Entity check — Bukkit-standard rayTrace (compatible with Spigot + Paper).
        //    Excludes the player themselves from the trace.
        RayTraceResult rayResult = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                maxDistance,
                e -> !e.equals(player)
        );
        Entity entityTarget = rayResult != null ? rayResult.getHitEntity() : null;
        if (entityTarget != null) {
            InspectableObject obj = inspector.inspect(entityTarget);
            openGui(player, obj, sessions);
            return true;
        }

        // 2. Block fallback
        Block blockTarget = player.getTargetBlockExact(maxDistance);
        if (blockTarget != null && blockTarget.getType() != Material.AIR) {
            InspectableObject obj = inspector.inspect(blockTarget);
            openGui(player, obj, sessions);
            return true;
        }

        TextUtil.audience(player).sendMessage(Component.text("No block or entity found within ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(maxDistance + " blocks. ").color(NamedTextColor.WHITE))
                .append(Component.text("Tip: use /inspect <player> to inspect by name.")
                        .color(NamedTextColor.GRAY)));
        return true;
    }

    // ── Tab completion ────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return List.of();

        // arg[0] — suggest online player names
        if (args.length == 1) {
            return PathCompleter.filter(
                    Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toList(),
                    args[0]
            );
        }

        return List.of();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void openGui(Player player, InspectableObject obj, SessionService sessions) {
        PlayerSession session = sessions.open(player.getUniqueId(), obj);
        new InspectorGui(session).open(player);
        TextUtil.audience(player).sendMessage(Component.text("[DataLens] ").color(NamedTextColor.DARK_AQUA)
                .append(Component.text("Inspecting: ").color(NamedTextColor.GRAY))
                .append(Component.text(obj.getType().name()).color(NamedTextColor.DARK_GRAY))
                .append(Component.text(" — ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(obj.getId()).color(NamedTextColor.AQUA)));
    }
}
