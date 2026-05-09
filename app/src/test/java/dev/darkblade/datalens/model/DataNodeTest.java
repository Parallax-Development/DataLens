package dev.darkblade.datalens.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DataNodeTest {

    @Test
    void primitivePNode_storesValueAndType() {
        DataNode node = DataNode.ofPrimitive("level", DataType.INT, 42);
        assertEquals("level", node.getKey());
        assertEquals(DataType.INT, node.getType());
        assertEquals(42, node.getValue());
        assertTrue(node.getType().isPrimitive());
        assertFalse(node.getType().isContainer());
    }

    @Test
    void compoundNode_addsAndFindsChild() {
        DataNode compound = DataNode.ofCompound("pdc");
        compound.addChild(DataNode.ofPrimitive("myplugin:owner", DataType.STRING, "Steve"));

        Optional<DataNode> child = compound.getChild("myplugin:owner");
        assertTrue(child.isPresent());
        assertEquals("Steve", child.get().getValue());
    }

    @Test
    void listNode_addsAndFindsElement() {
        DataNode list = DataNode.ofList("effects");
        list.addChild(DataNode.ofPrimitive("[0]", DataType.STRING, "speed"));
        list.addChild(DataNode.ofPrimitive("[1]", DataType.STRING, "haste"));

        assertEquals(2, list.childCount());
        assertEquals("speed", list.getElement(0).map(DataNode::getValue).orElse(null));
    }

    @Test
    void deepCopy_isFullyIndependent() {
        DataNode original = DataNode.ofCompound("root");
        original.addChild(DataNode.ofPrimitive("key", DataType.STRING, "original"));

        DataNode copy = original.deepCopy();
        // Mutate original — copy must not be affected
        original.getChild("key").ifPresent(n -> n.setValue("mutated"));

        assertEquals("mutated", original.getChild("key").map(DataNode::getValue).orElse(null));
        assertEquals("original", copy.getChild("key").map(DataNode::getValue).orElse(null));
    }

    @Test
    void deepCopy_byteArrayIsCloned() {
        byte[] arr = {1, 2, 3};
        DataNode node = DataNode.ofPrimitive("data", DataType.BYTE_ARRAY, arr);
        DataNode copy = node.deepCopy();

        arr[0] = 99;
        byte[] copiedArr = (byte[]) copy.getValue();
        assertEquals(1, copiedArr[0], "Deep copy must not share array reference");
    }

    @Test
    void removeChild_returnsTrue_whenFound() {
        DataNode compound = DataNode.ofCompound("pdc");
        compound.addChild(DataNode.ofPrimitive("ns:key", DataType.INT, 1));

        assertTrue(compound.removeChild("ns:key"));
        assertEquals(0, compound.childCount());
    }

    @Test
    void removeChild_returnsFalse_whenNotFound() {
        DataNode compound = DataNode.ofCompound("pdc");
        assertFalse(compound.removeChild("nonexistent"));
    }

    @Test
    void cannotSetValueOnContainerNode() {
        DataNode compound = DataNode.ofCompound("root");
        assertThrows(IllegalStateException.class, () -> compound.setValue("foo"));
    }

    @Test
    void cannotAddChildToNonContainerNode() {
        DataNode primitive = DataNode.ofPrimitive("key", DataType.STRING, "value");
        assertThrows(IllegalStateException.class,
                () -> primitive.addChild(DataNode.ofPrimitive("child", DataType.INT, 1)));
    }
}
