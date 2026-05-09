package dev.darkblade.datalens.model.diff;

import java.util.Objects;

/**
 * Records a single difference between two {@link dev.darkblade.datalens.model.DataNode} trees.
 */
public final class DataDiff {

    private final DiffType diffType;
    /** Dot-separated path to the changed node, e.g. {@code pdc.myplugin:owner}. */
    private final String path;
    private final Object oldValue;
    private final Object newValue;

    public DataDiff(DiffType diffType, String path, Object oldValue, Object newValue) {
        this.diffType = Objects.requireNonNull(diffType);
        this.path = Objects.requireNonNull(path);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public DiffType getDiffType() { return diffType; }
    public String getPath() { return path; }
    public Object getOldValue() { return oldValue; }
    public Object getNewValue() { return newValue; }

    @Override
    public String toString() {
        return "DataDiff{type=" + diffType + ", path='" + path + "', old=" + oldValue + ", new=" + newValue + "}";
    }
}
