package dev.darkblade.datalens.util;

import dev.darkblade.datalens.model.DataNode;
import dev.darkblade.datalens.model.DataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for converting between Bukkit's configuration Map<String, Object> and DataNode.
 */
public final class MapUtil {

    private MapUtil() {}

    @SuppressWarnings("unchecked")
    public static DataNode toNode(String name, Map<String, Object> map) {
        DataNode root = DataNode.ofCompound(name);
        if (map == null) return root;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            root.addChild(toNodeValue(entry.getKey(), entry.getValue()));
        }
        return root;
    }

    @SuppressWarnings("unchecked")
    private static DataNode toNodeValue(String key, Object value) {
        if (value == null) {
            return DataNode.ofPrimitive(key, DataType.STRING, "null");
        } else if (value instanceof Map) {
            return toNode(key, (Map<String, Object>) value);
        } else if (value instanceof List<?> list) {
            DataNode listNode = DataNode.ofList(key);
            for (int i = 0; i < list.size(); i++) {
                listNode.addChild(toNodeValue("[" + i + "]", list.get(i)));
            }
            return listNode;
        } else if (value instanceof Integer i) {
            return DataNode.ofPrimitive(key, DataType.INT, i);
        } else if (value instanceof Double d) {
            return DataNode.ofPrimitive(key, DataType.DOUBLE, d);
        } else if (value instanceof Float f) {
            return DataNode.ofPrimitive(key, DataType.FLOAT, f);
        } else if (value instanceof Long l) {
            return DataNode.ofPrimitive(key, DataType.LONG, l);
        } else if (value instanceof Boolean b) {
            return DataNode.ofPrimitive(key, DataType.BOOLEAN, b);
        } else if (value instanceof Byte b) {
            return DataNode.ofPrimitive(key, DataType.BYTE, b);
        } else if (value instanceof Short s) {
            return DataNode.ofPrimitive(key, DataType.SHORT, s);
        } else {
            return DataNode.ofPrimitive(key, DataType.STRING, String.valueOf(value));
        }
    }

    public static Map<String, Object> toMap(DataNode compoundNode) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (!compoundNode.getType().isContainer()) return map;

        for (DataNode child : compoundNode.getChildren()) {
            map.put(child.getKey(), toRawValue(child));
        }
        return map;
    }

    private static Object toRawValue(DataNode node) {
        if (node.getType() == DataType.COMPOUND) {
            return toMap(node);
        } else if (node.getType() == DataType.LIST) {
            List<Object> list = new ArrayList<>();
            for (DataNode child : node.getChildren()) {
                list.add(toRawValue(child));
            }
            return list;
        } else {
            return node.getValue();
        }
    }
}
