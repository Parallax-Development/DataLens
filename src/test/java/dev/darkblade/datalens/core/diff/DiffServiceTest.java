package dev.darkblade.datalens.core.diff;

import dev.darkblade.datalens.model.DataNode;
import dev.darkblade.datalens.model.DataType;
import dev.darkblade.datalens.model.diff.DataDiff;
import dev.darkblade.datalens.model.diff.DiffType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiffServiceTest {

    private DiffService service;

    @BeforeEach
    void setUp() {
        service = new DiffService();
    }

    @Test
    void noDiff_whenTreesIdentical() {
        DataNode a = buildTree("level", 5, "owner", "Steve");
        DataNode b = buildTree("level", 5, "owner", "Steve");
        List<DataDiff> diffs = service.diff(a, b);
        assertTrue(diffs.isEmpty(), "Identical trees must produce no diffs");
    }

    @Test
    void detectsChanged() {
        DataNode a = buildTree("level", 5, "owner", "Steve");
        DataNode b = buildTree("level", 99, "owner", "Steve");
        List<DataDiff> diffs = service.diff(a, b);
        assertEquals(1, diffs.size());
        assertEquals(DiffType.CHANGED, diffs.get(0).getDiffType());
        assertEquals(5, diffs.get(0).getOldValue());
        assertEquals(99, diffs.get(0).getNewValue());
    }

    @Test
    void detectsAdded() {
        DataNode a = DataNode.ofCompound("root");
        DataNode b = DataNode.ofCompound("root");
        b.addChild(DataNode.ofPrimitive("newKey", DataType.STRING, "hello"));

        List<DataDiff> diffs = service.diff(a, b);
        assertEquals(1, diffs.size());
        assertEquals(DiffType.ADDED, diffs.get(0).getDiffType());
    }

    @Test
    void detectsRemoved() {
        DataNode a = DataNode.ofCompound("root");
        a.addChild(DataNode.ofPrimitive("gone", DataType.INT, 1));
        DataNode b = DataNode.ofCompound("root");

        List<DataDiff> diffs = service.diff(a, b);
        assertEquals(1, diffs.size());
        assertEquals(DiffType.REMOVED, diffs.get(0).getDiffType());
    }

    @Test
    void detectsMultipleChanges() {
        DataNode a = buildTree("level", 1, "owner", "Alice");
        DataNode b = buildTree("level", 2, "owner", "Bob");

        List<DataDiff> diffs = service.diff(a, b);
        assertEquals(2, diffs.size());
        assertTrue(diffs.stream().allMatch(d -> d.getDiffType() == DiffType.CHANGED));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Creates a compound "pdc" node with two primitive children. */
    private DataNode buildTree(String key1, Object val1, String key2, Object val2) {
        DataNode pdc = DataNode.ofCompound("pdc");
        pdc.addChild(DataNode.ofPrimitive(key1, DataType.INT, val1));
        pdc.addChild(DataNode.ofPrimitive(key2, DataType.STRING, val2));
        return pdc;
    }
}
