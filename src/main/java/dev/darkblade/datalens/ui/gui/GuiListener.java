package dev.darkblade.datalens.ui.gui;

import dev.darkblade.datalens.core.edit.EditService;
import dev.darkblade.datalens.core.serialize.SerializationService;
import dev.darkblade.datalens.core.session.PlayerSession;
import dev.darkblade.datalens.core.session.SessionService;
import dev.darkblade.datalens.model.DataNode;
import dev.darkblade.datalens.model.DataType;
import dev.darkblade.datalens.security.PermissionGuard;
import dev.darkblade.datalens.util.TextUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.Bukkit;

/**
 * Handles all click events inside {@link InspectorGui} inventories.
 *
 * <p>Controls:
 * <ul>
 *   <li>Left-click on COMPOUND/LIST node → navigate deeper</li>
 *   <li>Shift+Left-click on primitive → prompt for edit in chat</li>
 *   <li>Right-click on primitive → delete confirmation in chat</li>
 *   <li>Back button → navigate up one level</li>
 *   <li>Prev / Next → page navigation</li>
 *   <li>Export → print JSON to chat</li>
 *   <li>Close → close inventory</li>
 * </ul>
 */
public final class GuiListener implements Listener {

    private static class EditContext {
        final PlayerSession session;
        final DataNode node;
        final InspectorGui gui;

        EditContext(PlayerSession session, DataNode node, InspectorGui gui) {
            this.session = session;
            this.node = node;
            this.gui = gui;
        }
    }

    /** Players currently waiting for a chat-based edit input. */
    private final Map<UUID, EditContext> awaitingEdit = new ConcurrentHashMap<>();

    private final SessionService sessions;
    private final EditService editService;
    private final SerializationService serializer;

    public GuiListener(SessionService sessions, EditService editService, SerializationService serializer) {
        this.sessions = sessions;
        this.editService = editService;
        this.serializer = serializer;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof InspectorGui gui)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getSlot();
        PlayerSession session = gui.getSession();

        // ── Navigation bar ────────────────────────────────────────────────────
        if (gui.isCloseSlot(slot)) { player.closeInventory(); return; }
        if (gui.isBackSlot(slot)) { session.pop(); gui.render(); return; }
        if (gui.isPrevSlot(slot)) { session.setPage(session.getPage() - 1); gui.render(); return; }
        if (gui.isNextSlot(slot)) { session.setPage(session.getPage() + 1); gui.render(); return; }

        if (gui.isExportSlot(slot)) {
            player.closeInventory();
            String json = serializer.toJson(session.getTarget().getRoot());
            Audience audience = TextUtil.audience(player);
            audience.sendMessage(Component.text("[DataLens] JSON Export:")
                    .color(NamedTextColor.AQUA));
            audience.sendMessage(Component.text(json).color(NamedTextColor.WHITE));
            return;
        }

        // ── Node slots ────────────────────────────────────────────────────────
        if (!gui.isNodeSlot(slot)) return;
        DataNode node = gui.nodeAt(slot);
        if (node == null) return;

        boolean isShift = event.isShiftClick();
        boolean isRight = event.isRightClick();

        // Container → expand
        if (node.getType().isContainer() && !isShift && !isRight) {
            session.push(node);
            gui.render();
            return;
        }

        // Shift+Left → edit primitive
        if (isShift && !isRight && node.getType().isPrimitive()) {
            if (!PermissionGuard.has(player, PermissionGuard.EDIT)) {
                TextUtil.audience(player).sendMessage(Component.text("You lack datalens.edit permission.")
                        .color(NamedTextColor.RED));
                return;
            }
            player.closeInventory();
            startChatEdit(player, session, node, gui);
            return;
        }

        // Right-click → delete
        if (isRight && !isShift) {
            if (!PermissionGuard.has(player, PermissionGuard.EDIT)) {
                TextUtil.audience(player).sendMessage(Component.text("You lack datalens.edit permission.")
                        .color(NamedTextColor.RED));
                return;
            }
            player.closeInventory();
            confirmDelete(player, session, node, gui);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Don't evict session on close — player may reopen
    }

    // ── Chat-based edit prompt ────────────────────────────────────────────────

    private void startChatEdit(Player player, PlayerSession session, DataNode node, InspectorGui gui) {
        Audience audience = TextUtil.audience(player);
        audience.sendMessage(Component.text("┌── DataLens Edit ──────────────────────────").color(NamedTextColor.DARK_AQUA));
        audience.sendMessage(Component.text("│ Key: ").color(NamedTextColor.GRAY)
                .append(Component.text(node.getKey()).color(NodeRenderer.colorFor(node.getType()))));
        audience.sendMessage(Component.text("│ Type: ").color(NamedTextColor.GRAY)
                .append(Component.text(node.getType().label()).color(NamedTextColor.WHITE)));
        audience.sendMessage(Component.text("│ Current: ").color(NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(node.getValue())).color(NamedTextColor.YELLOW)));
        audience.sendMessage(Component.text("└── Type new value in chat (or 'cancel'):").color(NamedTextColor.DARK_AQUA));
        awaitingEdit.put(player.getUniqueId(), new EditContext(session, node, gui));
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        EditContext ctx = awaitingEdit.remove(player.getUniqueId());
        if (ctx == null) return;

        event.setCancelled(true);
        String message = event.getMessage();

        Bukkit.getScheduler().runTask(org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), () -> {
            if (message.equalsIgnoreCase("cancel")) {
                TextUtil.audience(player).sendMessage(Component.text("Edit cancelled.").color(NamedTextColor.YELLOW));
                ctx.gui.render();
                player.openInventory(ctx.gui.getInventory());
                return;
            }

            try {
                editService.setNodeValue(ctx.session.getTarget(), ctx.node, null, message, player.getName());
                TextUtil.audience(player).sendMessage(Component.text("Value updated successfully!").color(NamedTextColor.GREEN));
                ctx.session.refresh();
                ctx.gui.render();
                player.openInventory(ctx.gui.getInventory());
            } catch (Exception e) {
                TextUtil.audience(player).sendMessage(Component.text("Edit failed: " + e.getMessage()).color(NamedTextColor.RED));
                ctx.gui.render();
                player.openInventory(ctx.gui.getInventory());
            }
        });
    }

    private void confirmDelete(Player player, PlayerSession session, DataNode node, InspectorGui gui) {
        TextUtil.audience(player).sendMessage(Component.text("Delete ").color(NamedTextColor.RED)
                .append(Component.text(node.getKey()).color(NamedTextColor.YELLOW))
                .append(Component.text("? Use: /data remove " + node.getKey()).color(NamedTextColor.RED)));
    }
}
