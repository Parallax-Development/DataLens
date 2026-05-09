package dev.darkblade.datalens.core.edit;

/**
 * A single segment of a DataNode path, representing either a named key or a list index.
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code "pdc"} → {@code PathSegment.ofKey("pdc")}</li>
 *   <li>{@code "myplugin:owner"} → {@code PathSegment.ofKey("myplugin:owner")}</li>
 *   <li>{@code "[0]"} → {@code PathSegment.ofIndex(0)}</li>
 * </ul>
 */
public final class PathSegment {

    private final String key;
    private final int index;
    private final boolean isIndex;

    private PathSegment(String key, int index, boolean isIndex) {
        this.key = key;
        this.index = index;
        this.isIndex = isIndex;
    }

    public static PathSegment ofKey(String key) {
        return new PathSegment(key, -1, false);
    }

    public static PathSegment ofIndex(int index) {
        return new PathSegment(null, index, true);
    }

    public boolean isIndex() { return isIndex; }
    public boolean isKey() { return !isIndex; }

    public String getKey() {
        if (isIndex) throw new IllegalStateException("This segment is an index, not a key");
        return key;
    }

    public int getIndex() {
        if (!isIndex) throw new IllegalStateException("This segment is a key, not an index");
        return index;
    }

    @Override
    public String toString() {
        return isIndex ? "[" + index + "]" : key;
    }
}
