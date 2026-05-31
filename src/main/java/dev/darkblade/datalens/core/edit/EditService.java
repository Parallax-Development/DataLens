package dev.darkblade.datalens.core.edit;

import dev.darkblade.datalens.adapter.common.Adapter;
import dev.darkblade.datalens.core.validate.ValidationService;
import dev.darkblade.datalens.model.DataNode;
import dev.darkblade.datalens.model.InspectableObject;
import dev.darkblade.datalens.model.validation.ValidationResult;
import dev.darkblade.datalens.repository.ChangeLogRepository;

import java.util.Objects;
import java.util.Optional;

/**
 * Handles safe editing of {@link DataNode} values within an {@link InspectableObject}.
 *
 * <p>Every edit follows the contract:
 * <ol>
 *   <li>Resolve target node by path</li>
 *   <li>Validate the new value against the node's type</li>
 *   <li>Create a deep-copy snapshot for rollback</li>
 *   <li>Apply the change in memory</li>
 *   <li>Persist via the adapter</li>
 *   <li>On failure → restore snapshot and rethrow</li>
 * </ol>
 *
 * <strong>Write operations must be called on the main server thread.</strong>
 */
public final class EditService {

    private final Adapter adapter;
    private final ValidationService validator;
    private final ChangeLogRepository changeLog;

    public EditService(Adapter adapter, ValidationService validator, ChangeLogRepository changeLog) {
        this.adapter = Objects.requireNonNull(adapter);
        this.validator = Objects.requireNonNull(validator);
        this.changeLog = Objects.requireNonNull(changeLog);
    }

    /**
     * Sets the value at {@code path} to {@code rawValue} (string representation).
     *
     * @param object   the target inspectable object
     * @param path     dot-notation path (e.g. {@code "pdc.myplugin:level"})
     * @param rawValue the new value as a string; will be coerced to the node's type
     * @param actor    player name for changelog
     * @throws EditException if path not found, type invalid, or persist fails
     */
    public void setValue(InspectableObject object, String path, String rawValue, String actor) {
        DataNode target = resolveOrThrow(object, path);
        setNodeValue(object, target, path, rawValue, actor);
    }

    /**
     * Sets the value of a specific node directly (used by GUI).
     */
    public void setNodeValue(InspectableObject object, DataNode target, String path, String rawValue, String actor) {
        ValidationResult validation = validator.validate(target.getType(), rawValue);
        if (!validation.isValid()) {
            throw new EditException("Validation failed: " + validation.getError().orElse("unknown error"));
        }

        Object coerced = validator.coerce(target.getType(), rawValue);
        DataNode snapshot = object.getRoot().deepCopy();
        Object oldValue = target.getValue();

        try {
            target.setValue(coerced);
            adapter.writeData(object, object.getRoot());
            changeLog.log(actor, object.getId(), "SET", path != null ? path : target.getKey(), String.valueOf(oldValue), rawValue);
        } catch (Exception ex) {
            object.setRoot(snapshot); // rollback
            throw new EditException("Persist failed — rolled back. Cause: " + ex.getMessage(), ex);
        }
    }

    /**
     * Removes the node at {@code path} from the data tree.
     *
     * @param object the target inspectable object
     * @param path   dot-notation path
     * @param actor  player name for changelog
     * @throws EditException if path not found or persist fails
     */
    public void remove(InspectableObject object, String path, String actor) {
        Optional<PathResolver.ParentContext> ctx = PathResolver.resolveParent(object.getRoot(), path);
        if (ctx.isEmpty()) {
            throw new EditException("Path not found: " + path);
        }

        DataNode snapshot = object.getRoot().deepCopy();
        PathResolver.ParentContext pc = ctx.get();

        try {
            boolean removed;
            if (pc.lastSegment().isIndex()) {
                removed = pc.parent().removeElement(pc.lastSegment().getIndex());
            } else {
                removed = pc.parent().removeChild(pc.lastSegment().getKey());
            }
            if (!removed) throw new EditException("Node not found at path: " + path);

            adapter.writeData(object, object.getRoot());
            changeLog.log(actor, object.getId(), "REMOVE", path, null, null);
        } catch (EditException e) {
            throw e;
        } catch (Exception ex) {
            object.setRoot(snapshot);
            throw new EditException("Persist failed — rolled back. Cause: " + ex.getMessage(), ex);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DataNode resolveOrThrow(InspectableObject object, String path) {
        return PathResolver.resolve(object.getRoot(), path)
                .orElseThrow(() -> new EditException("Path not found: " + path));
    }

    // ── Exception ─────────────────────────────────────────────────────────────

    public static final class EditException extends RuntimeException {
        public EditException(String msg) { super(msg); }
        public EditException(String msg, Throwable cause) { super(msg, cause); }
    }
}
