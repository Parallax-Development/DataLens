package dev.darkblade.datalens.util;

import de.tr7zw.changeme.nbtapi.NBTType;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import dev.darkblade.datalens.model.DataNode;
import dev.darkblade.datalens.model.DataType;

public final class NbtUtil {

    private NbtUtil() {}

    /**
     * Reads an entire NBT compound into a DataNode compound.
     */
    public static DataNode readCompound(String name, ReadableNBT nbt) {
        DataNode root = DataNode.ofCompound(name);
        if (nbt == null) return root;

        for (String key : nbt.getKeys()) {
            root.addChild(readTag(key, nbt));
        }
        return root;
    }

    private static DataNode readTag(String key, ReadableNBT nbt) {
        NBTType type = nbt.getType(key);
        if (type == null) type = NBTType.NBTTagEnd;

        return switch (type) {
            case NBTTagCompound -> readCompound(key, nbt.getCompound(key));
            case NBTTagList     -> readList(key, nbt);
            case NBTTagByte     -> DataNode.ofPrimitive(key, DataType.BYTE, nbt.getByte(key));
            case NBTTagShort    -> DataNode.ofPrimitive(key, DataType.SHORT, nbt.getShort(key));
            case NBTTagInt      -> DataNode.ofPrimitive(key, DataType.INT, nbt.getInteger(key));
            case NBTTagLong     -> DataNode.ofPrimitive(key, DataType.LONG, nbt.getLong(key));
            case NBTTagFloat    -> DataNode.ofPrimitive(key, DataType.FLOAT, nbt.getFloat(key));
            case NBTTagDouble   -> DataNode.ofPrimitive(key, DataType.DOUBLE, nbt.getDouble(key));
            case NBTTagString   -> DataNode.ofPrimitive(key, DataType.STRING, nbt.getString(key));
            case NBTTagByteArray -> DataNode.ofPrimitive(key, DataType.BYTE_ARRAY, nbt.getByteArray(key));
            case NBTTagIntArray -> DataNode.ofPrimitive(key, DataType.INT_ARRAY, nbt.getIntArray(key));
            default -> {
                try {
                    long[] lArr = nbt.getLongArray(key);
                    if (lArr != null && lArr.length > 0) {
                        yield DataNode.ofPrimitive(key, DataType.LONG_ARRAY, lArr);
                    }
                } catch (Exception ignored) {}
                yield DataNode.ofPrimitive(key, DataType.STRING, nbt.getString(key));
            }
        };
    }

    private static DataNode readList(String key, ReadableNBT parent) {
        DataNode listNode = DataNode.ofList(key);
        NBTType listType = parent.getListType(key);

        if (listType == NBTType.NBTTagCompound) {
            var compList = parent.getCompoundList(key);
            for (int i = 0; i < compList.size(); i++) {
                listNode.addChild(readCompound("[" + i + "]", compList.get(i)));
            }
        } else if (listType == NBTType.NBTTagString) {
            var strList = parent.getStringList(key);
            for (int i = 0; i < strList.size(); i++) {
                listNode.addChild(DataNode.ofPrimitive("[" + i + "]", DataType.STRING, strList.get(i)));
            }
        } else if (listType == NBTType.NBTTagInt) {
            var intList = parent.getIntegerList(key);
            for (int i = 0; i < intList.size(); i++) {
                listNode.addChild(DataNode.ofPrimitive("[" + i + "]", DataType.INT, intList.get(i)));
            }
        } else if (listType == NBTType.NBTTagFloat) {
            var floatList = parent.getFloatList(key);
            for (int i = 0; i < floatList.size(); i++) {
                listNode.addChild(DataNode.ofPrimitive("[" + i + "]", DataType.FLOAT, floatList.get(i)));
            }
        } else if (listType == NBTType.NBTTagDouble) {
            var doubleList = parent.getDoubleList(key);
            for (int i = 0; i < doubleList.size(); i++) {
                listNode.addChild(DataNode.ofPrimitive("[" + i + "]", DataType.DOUBLE, doubleList.get(i)));
            }
        } else if (listType == NBTType.NBTTagLong) {
            var longList = parent.getLongList(key);
            for (int i = 0; i < longList.size(); i++) {
                listNode.addChild(DataNode.ofPrimitive("[" + i + "]", DataType.LONG, longList.get(i)));
            }
        }
        return listNode;
    }

    /**
     * Writes an entire DataNode compound back into an NBT structure.
     */
    public static void writeCompound(DataNode root, ReadWriteNBT target) {
        if (!root.getType().isContainer()) return;

        // Remove keys from target that are no longer in root
        for (String key : target.getKeys()) {
            if (root.getChild(key).isEmpty()) {
                target.removeKey(key);
            }
        }

        for (DataNode child : root.getChildren()) {
            writeTag(child, target);
        }
    }

    private static void writeTag(DataNode node, ReadWriteNBT target) {
        String key = node.getKey();
        Object val = node.getValue();
        if (val == null && !node.getType().isContainer()) {
            target.removeKey(key);
            return;
        }

        switch (node.getType()) {
            case COMPOUND -> writeCompound(node, target.getOrCreateCompound(key));
            case LIST -> writeList(node, target);
            case STRING -> target.setString(key, (String) val);
            case INT -> target.setInteger(key, (Integer) val);
            case LONG -> target.setLong(key, (Long) val);
            case DOUBLE -> target.setDouble(key, (Double) val);
            case FLOAT -> target.setFloat(key, (Float) val);
            case BOOLEAN -> target.setBoolean(key, (Boolean) val);
            case BYTE -> target.setByte(key, (Byte) val);
            case SHORT -> target.setShort(key, (Short) val);
            case BYTE_ARRAY -> target.setByteArray(key, (byte[]) val);
            case INT_ARRAY -> target.setIntArray(key, (int[]) val);
            case LONG_ARRAY -> target.setLongArray(key, (long[]) val);
            default -> {}
        }
    }

    private static void writeList(DataNode listNode, ReadWriteNBT parent) {
        String key = listNode.getKey();
        if (listNode.getChildren().isEmpty()) {
            parent.removeKey(key);
            return;
        }

        DataType firstChildType = listNode.getChildren().get(0).getType();

        if (firstChildType == DataType.COMPOUND) {
            var compList = parent.getCompoundList(key);
            compList.clear();
            for (DataNode child : listNode.getChildren()) {
                ReadWriteNBT comp = compList.addCompound();
                writeCompound(child, comp);
            }
        } else if (firstChildType == DataType.STRING) {
            var strList = parent.getStringList(key);
            strList.clear();
            for (DataNode child : listNode.getChildren()) {
                strList.add((String) child.getValue());
            }
        } else if (firstChildType == DataType.INT) {
            var intList = parent.getIntegerList(key);
            intList.clear();
            for (DataNode child : listNode.getChildren()) {
                intList.add((Integer) child.getValue());
            }
        } else if (firstChildType == DataType.FLOAT) {
            var floatList = parent.getFloatList(key);
            floatList.clear();
            for (DataNode child : listNode.getChildren()) {
                floatList.add((Float) child.getValue());
            }
        } else if (firstChildType == DataType.DOUBLE) {
            var doubleList = parent.getDoubleList(key);
            doubleList.clear();
            for (DataNode child : listNode.getChildren()) {
                doubleList.add((Double) child.getValue());
            }
        } else if (firstChildType == DataType.LONG) {
            var longList = parent.getLongList(key);
            longList.clear();
            for (DataNode child : listNode.getChildren()) {
                longList.add((Long) child.getValue());
            }
        }
    }
}
