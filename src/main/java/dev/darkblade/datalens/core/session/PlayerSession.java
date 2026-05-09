package dev.darkblade.datalens.core.session;

import dev.darkblade.datalens.model.DataNode;
import dev.darkblade.datalens.model.InspectableObject;

import java.util.Objects;

/**
 * Holds a single player's inspection state.
 * The {@code workingCopy} is a deep copy of the root that can be mutated
 * independently of the committed data.
 */
public final class PlayerSession {

    private final InspectableObject target;
    private DataNode workingCopy;
    /** Current GUI page index. */
    private int page = 0;
    /** Path of nodes the player has navigated into (GUI breadcrumb). */
    private final java.util.Deque<DataNode> navigationStack = new java.util.ArrayDeque<>();

    public PlayerSession(InspectableObject target) {
        this.target = Objects.requireNonNull(target);
        this.workingCopy = target.getRoot().deepCopy();
        this.navigationStack.push(target.getRoot());
    }

    public InspectableObject getTarget() { return target; }
    public DataNode getWorkingCopy() { return workingCopy; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(0, page); }

    /** Navigates one level deeper into the given compound/list node. */
    public void push(DataNode node) { navigationStack.push(node); page = 0; }

    /** Navigates back up one level. Returns false if already at root. */
    public boolean pop() {
        if (navigationStack.size() <= 1) return false;
        navigationStack.pop();
        page = 0;
        return true;
    }

    /** The node currently being viewed in the GUI. */
    public DataNode current() {
        return navigationStack.isEmpty() ? target.getRoot() : navigationStack.peek();
    }

    /** Refreshes the working copy from the committed state. */
    public void refresh() {
        this.workingCopy = target.getRoot().deepCopy();
    }
}
