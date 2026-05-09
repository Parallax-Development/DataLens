package dev.darkblade.datalens.model.diff;

/** The kind of change detected between two {@link dev.darkblade.datalens.model.DataNode} trees. */
public enum DiffType {
    /** A key present in {@code b} that was absent in {@code a}. */
    ADDED,
    /** A key present in {@code a} that is absent in {@code b}. */
    REMOVED,
    /** A key present in both but with a different value or type. */
    CHANGED
}
