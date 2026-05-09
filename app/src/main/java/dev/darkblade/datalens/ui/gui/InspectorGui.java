package dev.darkblade.datalens.ui.gui;

import dev.darkblade.datalens.core.session.PlayerSession;
import dev.darkblade.datalens.model.DataNode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * The main 54-slot inspector inventory GUI.
 *
 * <pre>
 * Layout (rows 0-5):
 *  Row 0: [Back] [filler x7] [Close]
 *  Rows 1-4: up to 36 DataNode items (paginated)
 *  Row 5: [Prev] [filler x3] [Page info] [filler x3] [Export] [Next]
 * </pre>
 */
public final class InspectorGui implements InventoryHolder {

    private static final int SIZE = 54;
    private static final int NODE_START_SLOT = 9;   // row 1
    private static final int NODE_END_SLOT   = 44;  // row 4 (inclusive)
    private static final int NODES_PER_PAGE  = 36;

    // Navbar slots
    private static final int SLOT_BACK   = 0;
    private static final int SLOT_CLOSE  = 8;
    private static final int SLOT_PREV   = 45;
    private static final int SLOT_EXPORT = 49;
    private static final int SLOT_NEXT   = 53;

    private final Inventory inventory;
    private final PlayerSession session;

    public InspectorGui(PlayerSession session) {
        this.session = session;
        String title = "DataLens — " + session.getTarget().getId();
        this.inventory = Bukkit.createInventory(this, SIZE,
                Component.text(title).color(NamedTextColor.DARK_AQUA)
                        .decoration(TextDecoration.BOLD, true));
        render();
    }

    // ── Inventory holder ──────────────────────────────────────────────────────

    @Override
    public Inventory getInventory() { return inventory; }

    // ── Public helpers ────────────────────────────────────────────────────────

    public PlayerSession getSession() { return session; }

    /** Opens this GUI for the given player. */
    public void open(Player player) { player.openInventory(inventory); }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /** Clears and redraws the entire GUI based on current session state. */
    public void render() {
        inventory.clear();

        // Row 0 — top nav
        inventory.setItem(SLOT_BACK, NodeRenderer.backButton());
        inventory.setItem(SLOT_CLOSE, NodeRenderer.closeButton());
        for (int i = 1; i < 8; i++) inventory.setItem(i, NodeRenderer.filler());

        // Node area
        DataNode current = session.current();
        List<DataNode> children = current.getChildren();
        int page = session.getPage();
        int totalPages = Math.max(1, (int) Math.ceil((double) children.size() / NODES_PER_PAGE));
        if (page >= totalPages) { session.setPage(0); page = 0; }

        int from = page * NODES_PER_PAGE;
        int to   = Math.min(from + NODES_PER_PAGE, children.size());

        for (int i = from; i < to; i++) {
            int slot = NODE_START_SLOT + (i - from);
            if (slot > NODE_END_SLOT) break;
            inventory.setItem(slot, NodeRenderer.render(children.get(i)));
        }

        // Row 5 — bottom nav
        for (int i = 45; i < 54; i++) inventory.setItem(i, NodeRenderer.filler());
        if (page > 0) inventory.setItem(SLOT_PREV, NodeRenderer.prevPage());
        if (page < totalPages - 1) inventory.setItem(SLOT_NEXT, NodeRenderer.nextPage());
        inventory.setItem(SLOT_EXPORT, NodeRenderer.exportButton());

        // Page info in the centre of row 5
        inventory.setItem(49, pageInfoItem(page + 1, totalPages));
    }

    private ItemStack pageInfoItem(int current, int total) {
        return NodeRenderer.namedPane(
                Component.text("Page " + current + " / " + total)
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
    }

    // ── Slot queries ──────────────────────────────────────────────────────────

    public boolean isNodeSlot(int slot) { return slot >= NODE_START_SLOT && slot <= NODE_END_SLOT; }
    public boolean isBackSlot(int slot) { return slot == SLOT_BACK; }
    public boolean isCloseSlot(int slot) { return slot == SLOT_CLOSE; }
    public boolean isPrevSlot(int slot) { return slot == SLOT_PREV; }
    public boolean isNextSlot(int slot) { return slot == SLOT_NEXT; }
    public boolean isExportSlot(int slot) { return slot == SLOT_EXPORT; }

    /** Returns the DataNode at the clicked slot, or null. */
    public DataNode nodeAt(int slot) {
        if (!isNodeSlot(slot)) return null;
        List<DataNode> children = session.current().getChildren();
        int index = session.getPage() * NODES_PER_PAGE + (slot - NODE_START_SLOT);
        if (index < 0 || index >= children.size()) return null;
        return children.get(index);
    }
}
