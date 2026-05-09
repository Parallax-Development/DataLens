package dev.darkblade.datalens.api;

import dev.darkblade.datalens.core.inspect.InspectorService;
import dev.darkblade.datalens.core.session.SessionService;
import dev.darkblade.datalens.model.schema.Schema;

/**
 * Public API surface exposed by DataLens to other plugins.
 * Obtain an instance via {@code DataLensPlugin.getAPI()}.
 */
public interface DataLensAPI {

    /**
     * Registers a data schema for a given PDC namespace.
     * DataLens will use the schema to validate data and improve GUI labelling.
     *
     * @param namespace the PDC namespace string (e.g. {@code "myplugin"})
     * @param schema    the schema definition
     */
    void registerSchema(String namespace, Schema schema);

    /**
     * Returns the active {@link InspectorService} for programmatic inspection.
     */
    InspectorService getInspectorService();

    /**
     * Returns the active {@link SessionService} for session management.
     */
    SessionService getSessionService();
}
