package dev.darkblade.datalens.util;

import dev.darkblade.datalens.model.DataNode;
import dev.darkblade.datalens.model.DataType;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;

/**
 * Utility methods for reading {@link PersistentDataContainer} entries into {@link DataNode} trees.
 * <p>
 * Because the PDC API does not expose the stored type directly, we probe candidate
 * types in order of specificity. This is the authoritative PDC-reading logic shared
 * by all versioned adapters.
 */
public final class PdcUtil {

    private PdcUtil() {}

    /**
     * Reads all entries from {@code pdc} into a COMPOUND DataNode named {@code "pdc"}.
     */
    public static DataNode readPdc(PersistentDataContainer pdc) {
        DataNode compound = DataNode.ofCompound("pdc");
        Set<NamespacedKey> keys = pdc.getKeys();
        for (NamespacedKey key : keys) {
            DataNode child = probeKey(pdc, key);
            if (child != null) {
                compound.addChild(child);
            }
        }
        return compound;
    }

    /**
     * Probes the type of a PDC entry and converts it to a DataNode.
     * Returns null if the key cannot be read (unsupported custom type).
     */
    private static DataNode probeKey(PersistentDataContainer pdc, NamespacedKey key) {
        String nodeKey = key.toString(); // "namespace:key"

        // Nested container (COMPOUND)
        if (pdc.has(key, PersistentDataType.TAG_CONTAINER)) {
            PersistentDataContainer nested = pdc.get(key, PersistentDataType.TAG_CONTAINER);
            if (nested == null) return DataNode.ofCompound(nodeKey);
            DataNode child = DataNode.ofCompound(nodeKey);
            for (DataNode grandChild : readPdc(nested).getChildren()) {
                child.addChild(grandChild);
            }
            return child;
        }

        // Container array (LIST of COMPOUND)
        if (pdc.has(key, PersistentDataType.TAG_CONTAINER_ARRAY)) {
            PersistentDataContainer[] arr = pdc.get(key, PersistentDataType.TAG_CONTAINER_ARRAY);
            DataNode list = DataNode.ofList(nodeKey);
            if (arr != null) {
                for (int i = 0; i < arr.length; i++) {
                    DataNode element = DataNode.ofCompound("[" + i + "]");
                    for (DataNode grandChild : readPdc(arr[i]).getChildren()) {
                        element.addChild(grandChild);
                    }
                    list.addChild(element);
                }
            }
            return list;
        }

        // Primitive types — ordered from most to least specific to avoid false positives
        if (pdc.has(key, PersistentDataType.STRING)) {
            return DataNode.ofPrimitive(nodeKey, DataType.STRING, pdc.get(key, PersistentDataType.STRING));
        }
        if (pdc.has(key, PersistentDataType.LONG)) {
            return DataNode.ofPrimitive(nodeKey, DataType.LONG, pdc.get(key, PersistentDataType.LONG));
        }
        if (pdc.has(key, PersistentDataType.INTEGER)) {
            return DataNode.ofPrimitive(nodeKey, DataType.INT, pdc.get(key, PersistentDataType.INTEGER));
        }
        if (pdc.has(key, PersistentDataType.DOUBLE)) {
            return DataNode.ofPrimitive(nodeKey, DataType.DOUBLE, pdc.get(key, PersistentDataType.DOUBLE));
        }
        if (pdc.has(key, PersistentDataType.FLOAT)) {
            return DataNode.ofPrimitive(nodeKey, DataType.FLOAT, pdc.get(key, PersistentDataType.FLOAT));
        }
        if (pdc.has(key, PersistentDataType.SHORT)) {
            return DataNode.ofPrimitive(nodeKey, DataType.SHORT, pdc.get(key, PersistentDataType.SHORT));
        }
        if (pdc.has(key, PersistentDataType.BYTE)) {
            return DataNode.ofPrimitive(nodeKey, DataType.BYTE, pdc.get(key, PersistentDataType.BYTE));
        }
        if (pdc.has(key, PersistentDataType.BOOLEAN)) {
            return DataNode.ofPrimitive(nodeKey, DataType.BOOLEAN, pdc.get(key, PersistentDataType.BOOLEAN));
        }
        if (pdc.has(key, PersistentDataType.BYTE_ARRAY)) {
            return DataNode.ofPrimitive(nodeKey, DataType.BYTE_ARRAY, pdc.get(key, PersistentDataType.BYTE_ARRAY));
        }
        if (pdc.has(key, PersistentDataType.INTEGER_ARRAY)) {
            return DataNode.ofPrimitive(nodeKey, DataType.INT_ARRAY, pdc.get(key, PersistentDataType.INTEGER_ARRAY));
        }
        if (pdc.has(key, PersistentDataType.LONG_ARRAY)) {
            return DataNode.ofPrimitive(nodeKey, DataType.LONG_ARRAY, pdc.get(key, PersistentDataType.LONG_ARRAY));
        }

        // Unknown / custom PersistentDataType — record as UNKNOWN
        return DataNode.ofPrimitive(nodeKey, DataType.UNKNOWN, "<custom type>");
    }

    /**
     * Writes a single DataNode back into a PersistentDataContainer, resolving the
     * correct PersistentDataType from the node's DataType.
     */
    @SuppressWarnings("unchecked")
    public static void writeNode(PersistentDataContainer pdc, NamespacedKey key, DataNode node) {
        switch (node.getType()) {
            case STRING   -> pdc.set(key, PersistentDataType.STRING, (String) node.getValue());
            case INT      -> pdc.set(key, PersistentDataType.INTEGER, (Integer) node.getValue());
            case LONG     -> pdc.set(key, PersistentDataType.LONG, (Long) node.getValue());
            case DOUBLE   -> pdc.set(key, PersistentDataType.DOUBLE, (Double) node.getValue());
            case FLOAT    -> pdc.set(key, PersistentDataType.FLOAT, (Float) node.getValue());
            case SHORT    -> pdc.set(key, PersistentDataType.SHORT, (Short) node.getValue());
            case BYTE     -> pdc.set(key, PersistentDataType.BYTE, (Byte) node.getValue());
            case BOOLEAN  -> pdc.set(key, PersistentDataType.BOOLEAN, (Boolean) node.getValue());
            case BYTE_ARRAY -> pdc.set(key, PersistentDataType.BYTE_ARRAY, (byte[]) node.getValue());
            case INT_ARRAY  -> pdc.set(key, PersistentDataType.INTEGER_ARRAY, (int[]) node.getValue());
            case LONG_ARRAY -> pdc.set(key, PersistentDataType.LONG_ARRAY, (long[]) node.getValue());
            default -> {} // COMPOUND, LIST, UNKNOWN — handled by caller
        }
    }
}
