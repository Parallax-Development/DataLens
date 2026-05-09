package dev.darkblade.datalens.model.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the expected structure of data for a given namespace key.
 * External plugins can register schemas via {@link dev.darkblade.datalens.api.DataLensAPI}
 * to improve DataLens visualisation and enable structural validation.
 */
public final class Schema {

    private final String namespace;
    private final List<SchemaField> fields;

    public Schema(String namespace, List<SchemaField> fields) {
        this.namespace = Objects.requireNonNull(namespace);
        this.fields = new ArrayList<>(Objects.requireNonNull(fields));
    }

    public String getNamespace() { return namespace; }

    public List<SchemaField> getFields() { return Collections.unmodifiableList(fields); }
}
