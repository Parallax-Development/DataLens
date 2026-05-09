package dev.darkblade.datalens.model;

/**
 * Represents the type of data stored in a {@link DataNode}.
 * Covers all NBT-compatible types plus UUID for PDC convenience.
 */
public enum DataType {

    STRING(false, false),
    INT(false, false),
    LONG(false, false),
    DOUBLE(false, false),
    FLOAT(false, false),
    BOOLEAN(false, false),
    BYTE(false, false),
    SHORT(false, false),
    BYTE_ARRAY(false, true),
    INT_ARRAY(false, true),
    LONG_ARRAY(false, true),
    LIST(true, false),
    COMPOUND(true, false),
    UUID(false, false),
    UNKNOWN(false, false);

    private final boolean container;
    private final boolean array;

    DataType(boolean container, boolean array) {
        this.container = container;
        this.array = array;
    }

    /** True if this type holds child {@link DataNode}s. */
    public boolean isContainer() { return container; }

    /** True if this type is a primitive array. */
    public boolean isArray() { return array; }

    /** True if this type stores a single scalar value. */
    public boolean isPrimitive() { return !container && !array; }

    /** Display label used in GUI and chat renderers. */
    public String label() {
        return switch (this) {
            case STRING -> "String";
            case INT -> "Int";
            case LONG -> "Long";
            case DOUBLE -> "Double";
            case FLOAT -> "Float";
            case BOOLEAN -> "Boolean";
            case BYTE -> "Byte";
            case SHORT -> "Short";
            case BYTE_ARRAY -> "Byte[]";
            case INT_ARRAY -> "Int[]";
            case LONG_ARRAY -> "Long[]";
            case LIST -> "List";
            case COMPOUND -> "Compound";
            case UUID -> "UUID";
            case UNKNOWN -> "Unknown";
        };
    }
}
