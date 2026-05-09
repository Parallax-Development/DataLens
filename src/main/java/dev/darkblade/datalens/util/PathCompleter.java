package dev.darkblade.datalens.util;

import dev.darkblade.datalens.core.edit.PathResolver;
import dev.darkblade.datalens.model.DataNode;
import dev.darkblade.datalens.model.DataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates tab-completion suggestions for DataNode paths and values.
 *
 * <p>Used by {@link dev.darkblade.datalens.command.DataCommand} to provide
 * context-aware completion from the player's active inspection session.
 */
public final class PathCompleter {

    private PathCompleter() {}

    // ── Path completion ───────────────────────────────────────────────────────

    /**
     * Returns all navigable dot-notation paths within the given DataNode tree,
     * filtered by the supplied prefix string.
     *
     * <p>Example output for a PDC node containing {@code myplugin:level} and {@code myplugin:owner}:
     * <pre>
     *   pdc
     *   pdc.myplugin:level
     *   pdc.myplugin:owner
     *   attributes
     *   attributes.generic.max_health
     *   ...
     * </pre>
     *
     * @param root   the root DataNode of the inspected object
     * @param prefix the current partially-typed string (may be empty)
     * @return filtered list of matching path strings
     */
    public static List<String> paths(DataNode root, String prefix) {
        List<String> all = new ArrayList<>();
        collectPaths(root, "", all);
        return filter(all, prefix);
    }

    /** Recursively collects all navigable paths. */
    private static void collectPaths(DataNode node, String parentPath, List<String> out) {
        for (DataNode child : node.getChildren()) {
            String path = parentPath.isEmpty() ? child.getKey() : parentPath + "." + child.getKey();
            out.add(path);
            if (child.getType().isContainer()) {
                collectPaths(child, path, out);
            }
        }
    }

    // ── Value suggestions ─────────────────────────────────────────────────────

    /**
     * Returns a list of suggested values for {@code /data set <path> <tab>},
     * based on the type of the node at {@code path} and its current value.
     *
     * @param root   root DataNode of the inspection target
     * @param path   the path string already typed by the player
     * @param prefix the partial value string typed so far
     * @return context-aware value suggestions, possibly empty
     */
    public static List<String> values(DataNode root, String path, String prefix) {
        return PathResolver.resolve(root, path)
                .map(node -> valueSuggestionsFor(node, prefix))
                .orElse(List.of());
    }

    private static List<String> valueSuggestionsFor(DataNode node, String prefix) {
        DataType type = node.getType();

        // Container types cannot be set directly
        if (type.isContainer() || type.isArray()) return List.of();

        List<String> suggestions = new ArrayList<>();

        switch (type) {
            case BOOLEAN -> {
                suggestions.add("true");
                suggestions.add("false");
            }
            case BYTE -> {
                addCurrentValue(suggestions, node);
                suggestions.add("0");
                suggestions.add("1");
                suggestions.add("127");
                suggestions.add("-128");
            }
            case SHORT -> {
                addCurrentValue(suggestions, node);
                suggestions.add("0");
                suggestions.add("1");
            }
            case INT -> {
                addCurrentValue(suggestions, node);
                suggestions.add("0");
                suggestions.add("1");
                suggestions.add("-1");
                suggestions.add("100");
            }
            case LONG -> {
                addCurrentValue(suggestions, node);
                suggestions.add("0");
                suggestions.add("1");
            }
            case FLOAT, DOUBLE -> {
                addCurrentValue(suggestions, node);
                suggestions.add("0.0");
                suggestions.add("1.0");
                suggestions.add("0.5");
            }
            case STRING -> {
                // Offer the current value so the player can see and modify it
                addCurrentValue(suggestions, node);
            }
            case UUID -> {
                // Offer the current UUID; the player replaces it entirely
                addCurrentValue(suggestions, node);
                suggestions.add("00000000-0000-0000-0000-000000000000");
            }
            case UNKNOWN -> {
                // No safe suggestions
            }
            default -> {}
        }

        return filter(suggestions, prefix);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    /**
     * Adds the node's current value to the suggestions list (as string).
     * Skips null values.
     */
    private static void addCurrentValue(List<String> suggestions, DataNode node) {
        if (node.getValue() != null) {
            suggestions.add(node.getValue().toString());
        }
    }

    /**
     * Filters a list of candidates, keeping only those that start with
     * {@code prefix} (case-insensitive). Returns all candidates if prefix is empty.
     */
    public static List<String> filter(List<String> candidates, String prefix) {
        if (prefix == null || prefix.isEmpty()) return candidates;
        String lower = prefix.toLowerCase();
        return candidates.stream()
                .filter(c -> c.toLowerCase().startsWith(lower))
                .toList();
    }
}
