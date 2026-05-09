package dev.darkblade.datalens.model.schema;

import dev.darkblade.datalens.model.DataType;

import java.util.Objects;

/**
 * Describes a single expected field within a {@link Schema}.
 */
public final class SchemaField {

    private final String key;
    private final DataType expectedType;
    private final boolean required;

    public SchemaField(String key, DataType expectedType, boolean required) {
        this.key = Objects.requireNonNull(key);
        this.expectedType = Objects.requireNonNull(expectedType);
        this.required = required;
    }

    public String getKey() { return key; }
    public DataType getExpectedType() { return expectedType; }
    public boolean isRequired() { return required; }
}
