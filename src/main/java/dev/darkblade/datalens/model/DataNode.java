package dev.darkblade.datalens.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A tree node representing a single piece of inspected data.
 *
 * <ul>
 *   <li>Primitive nodes ({@link DataType#isPrimitive()}) hold a scalar {@code value}.</li>
 *   <li>{@link DataType#COMPOUND} nodes hold a named list of child DataNodes.</li>
 *   <li>{@link DataType#LIST} nodes hold an ordered list of child DataNodes.</li>
 *   <li>Array nodes hold a raw array object as {@code value}.</li>
 * </ul>
 *
 * Designed to be mutable for in-place edits but supports {@link #deepCopy()} for
 * creating pre-edit snapshots used in rollback scenarios.
 */
public final class DataNode {

    private final String key;
    private DataType type;
    private Object value;
    private final List<DataNode> children;

    // ── Constructors ──────────────────────────────────────────────────────────

    private DataNode(String key, DataType type, Object value, List<DataNode> children) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.value = value;
        this.children = children != null ? new ArrayList<>(children) : new ArrayList<>();
    }

    // ── Factory methods ───────────────────────────────────────────────────────

    /** Creates a primitive (scalar) node. */
    public static DataNode ofPrimitive(String key, DataType type, Object value) {
        if (type.isContainer()) {
            throw new IllegalArgumentException("Cannot use container type for primitive node: " + type);
        }
        return new DataNode(key, type, value, null);
    }

    /** Creates a COMPOUND node with pre-populated children. */
    public static DataNode ofCompound(String key, List<DataNode> children) {
        return new DataNode(key, DataType.COMPOUND, null, children);
    }

    /** Creates an empty COMPOUND node. */
    public static DataNode ofCompound(String key) {
        return new DataNode(key, DataType.COMPOUND, null, new ArrayList<>());
    }

    /** Creates a LIST node with pre-populated elements. */
    public static DataNode ofList(String key, List<DataNode> elements) {
        return new DataNode(key, DataType.LIST, null, elements);
    }

    /** Creates an empty LIST node. */
    public static DataNode ofList(String key) {
        return new DataNode(key, DataType.LIST, null, new ArrayList<>());
    }

    // ── Read accessors ────────────────────────────────────────────────────────

    public String getKey() { return key; }
    public DataType getType() { return type; }
    public Object getValue() { return value; }

    /** Unmodifiable view of children (meaningful for COMPOUND and LIST types). */
    public List<DataNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /** Finds a direct child by key (COMPOUND nodes). */
    public Optional<DataNode> getChild(String childKey) {
        return children.stream()
                .filter(c -> c.key.equals(childKey))
                .findFirst();
    }

    /** Gets a list element by index (LIST nodes). */
    public Optional<DataNode> getElement(int index) {
        if (index < 0 || index >= children.size()) return Optional.empty();
        return Optional.of(children.get(index));
    }

    /** Number of children / list elements. */
    public int childCount() { return children.size(); }

    public boolean hasChildren() { return !children.isEmpty(); }

    // ── Mutation ──────────────────────────────────────────────────────────────

    /** Sets the scalar value. Only valid on primitive/array nodes. */
    public void setValue(Object value) {
        if (type.isContainer()) {
            throw new IllegalStateException("Cannot set scalar value on container node: " + key);
        }
        this.value = value;
    }

    /** Replaces the type annotation. Use with care. */
    public void setType(DataType type) { this.type = type; }

    /** Adds a child node (COMPOUND or LIST). */
    public void addChild(DataNode child) {
        if (!type.isContainer()) {
            throw new IllegalStateException("Cannot add child to non-container node: " + key);
        }
        children.add(child);
    }

    /** Removes a direct child by key (COMPOUND). Returns true if found and removed. */
    public boolean removeChild(String childKey) {
        return children.removeIf(c -> c.key.equals(childKey));
    }

    /** Removes a list element by index (LIST). */
    public boolean removeElement(int index) {
        if (index < 0 || index >= children.size()) return false;
        children.remove(index);
        return true;
    }

    // ── Deep copy (for pre-edit snapshots) ────────────────────────────────────

    /**
     * Creates a fully independent deep copy of this node and all descendants.
     * Used to create snapshots before edits so rollback is possible.
     */
    public DataNode deepCopy() {
        List<DataNode> copiedChildren = new ArrayList<>(children.size());
        for (DataNode child : children) {
            copiedChildren.add(child.deepCopy());
        }
        return new DataNode(key, type, copyValue(value), copiedChildren);
    }

    /** Copies array values to ensure true isolation. */
    private static Object copyValue(Object v) {
        if (v instanceof byte[] arr) return arr.clone();
        if (v instanceof int[] arr) return arr.clone();
        if (v instanceof long[] arr) return arr.clone();
        return v; // Strings, Numbers, Booleans, UUIDs are immutable
    }

    // ── Object overrides ──────────────────────────────────────────────────────

    @Override
    public String toString() {
        if (type.isContainer()) {
            return "DataNode{key='" + key + "', type=" + type + ", children=" + children.size() + "}";
        }
        return "DataNode{key='" + key + "', type=" + type + ", value=" + value + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataNode other)) return false;
        return key.equals(other.key) && type == other.type && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, type, value);
    }
}
