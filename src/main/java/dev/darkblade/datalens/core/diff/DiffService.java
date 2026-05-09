package dev.darkblade.datalens.core.diff;

import dev.darkblade.datalens.model.DataNode;
import dev.darkblade.datalens.model.diff.DataDiff;
import dev.darkblade.datalens.model.diff.DiffType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Compares two {@link DataNode} trees and returns a flat list of differences.
 * Differences are expressed as path-annotated {@link DataDiff} records.
 */
public final class DiffService {

    /**
     * Computes the diff between {@code a} (old) and {@code b} (new).
     *
     * @return ordered list of differences; empty if the trees are structurally identical
     */
    public List<DataDiff> diff(DataNode a, DataNode b) {
        List<DataDiff> results = new ArrayList<>();
        compare(a, b, "", results);
        return results;
    }

    // ── Recursive comparison ──────────────────────────────────────────────────

    private void compare(DataNode a, DataNode b, String pathPrefix, List<DataDiff> out) {
        String currentPath = pathPrefix.isEmpty() ? a.getKey() : pathPrefix + "." + a.getKey();

        // Both are containers — recurse on children
        if (a.getType().isContainer() && b.getType().isContainer()) {
            compareChildren(a, b, currentPath, out);
            return;
        }

        // Type changed
        if (a.getType() != b.getType()) {
            out.add(new DataDiff(DiffType.CHANGED, currentPath,
                    a.getType().label() + ":" + a.getValue(),
                    b.getType().label() + ":" + b.getValue()));
            return;
        }

        // Same primitive type — compare values
        if (!valuesEqual(a.getValue(), b.getValue())) {
            out.add(new DataDiff(DiffType.CHANGED, currentPath, a.getValue(), b.getValue()));
        }
    }

    private void compareChildren(DataNode a, DataNode b, String path, List<DataDiff> out) {
        Map<String, DataNode> aChildren = indexByKey(a.getChildren());
        Map<String, DataNode> bChildren = indexByKey(b.getChildren());

        // Keys in A — check for removed or changed
        for (Map.Entry<String, DataNode> entry : aChildren.entrySet()) {
            String key = entry.getKey();
            if (!bChildren.containsKey(key)) {
                String childPath = path + "." + key;
                out.add(new DataDiff(DiffType.REMOVED, childPath, entry.getValue().getValue(), null));
            } else {
                compare(entry.getValue(), bChildren.get(key), path, out);
            }
        }

        // Keys in B but not in A — added
        for (Map.Entry<String, DataNode> entry : bChildren.entrySet()) {
            if (!aChildren.containsKey(entry.getKey())) {
                String childPath = path + "." + entry.getKey();
                out.add(new DataDiff(DiffType.ADDED, childPath, null, entry.getValue().getValue()));
            }
        }
    }

    private Map<String, DataNode> indexByKey(List<DataNode> nodes) {
        return nodes.stream().collect(Collectors.toMap(DataNode::getKey, n -> n, (x, y) -> x));
    }

    private boolean valuesEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        // Array equality
        if (a instanceof byte[] aa && b instanceof byte[] bb) return java.util.Arrays.equals(aa, bb);
        if (a instanceof int[] aa && b instanceof int[] bb) return java.util.Arrays.equals(aa, bb);
        if (a instanceof long[] aa && b instanceof long[] bb) return java.util.Arrays.equals(aa, bb);
        return a.equals(b);
    }
}
