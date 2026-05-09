package dev.darkblade.datalens.core.edit;

import dev.darkblade.datalens.model.DataNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses dot-separated path strings and resolves them against a {@link DataNode} tree.
 *
 * <p>Path format:
 * <pre>
 *   pdc.myplugin:owner
 *   entity.attributes.generic.max_health
 *   item.enchantments
 *   pdc.myns:list[2].value
 * </pre>
 *
 * Segments separated by {@code '.'} except when inside {@code '[...]'} index notation.
 */
public final class PathResolver {

    private PathResolver() {}

    /**
     * Parses a path string into an ordered list of {@link PathSegment}s.
     *
     * @throws IllegalArgumentException on malformed paths
     */
    public static List<PathSegment> parse(String path) {
        List<PathSegment> segments = new ArrayList<>();
        // Tokenise: split on '.' but keep index tokens attached to the preceding key
        String[] parts = path.split("\\.");

        for (String part : parts) {
            if (part.isEmpty()) {
                throw new IllegalArgumentException("Empty segment in path: '" + path + "'");
            }
            // Check for inline index at end of segment: e.g. "list[2]"
            if (part.contains("[")) {
                int bracketStart = part.indexOf('[');
                String keyPart = part.substring(0, bracketStart);
                if (!keyPart.isEmpty()) {
                    segments.add(PathSegment.ofKey(keyPart));
                }
                // Parse all consecutive [n] tokens
                String remainder = part.substring(bracketStart);
                while (remainder.startsWith("[")) {
                    int close = remainder.indexOf(']');
                    if (close < 0) {
                        throw new IllegalArgumentException("Unclosed '[' in path: '" + path + "'");
                    }
                    String idxStr = remainder.substring(1, close);
                    try {
                        segments.add(PathSegment.ofIndex(Integer.parseInt(idxStr)));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid index '" + idxStr + "' in path: '" + path + "'");
                    }
                    remainder = remainder.substring(close + 1);
                }
            } else {
                segments.add(PathSegment.ofKey(part));
            }
        }

        return segments;
    }

    /**
     * Resolves a path against a root DataNode.
     *
     * @return the node at the given path, or {@link Optional#empty()} if not found
     */
    public static Optional<DataNode> resolve(DataNode root, String path) {
        List<PathSegment> segments = parse(path);
        DataNode current = root;

        for (PathSegment segment : segments) {
            if (segment.isIndex()) {
                Optional<DataNode> el = current.getElement(segment.getIndex());
                if (el.isEmpty()) return Optional.empty();
                current = el.get();
            } else {
                Optional<DataNode> child = current.getChild(segment.getKey());
                if (child.isEmpty()) return Optional.empty();
                current = child.get();
            }
        }

        return Optional.of(current);
    }

    /**
     * Resolves the parent of the target path and returns both parent and the final segment.
     * Useful when you need to modify or remove the target node.
     *
     * @return a {@link ParentContext} or {@link Optional#empty()} if the parent cannot be found
     */
    public static Optional<ParentContext> resolveParent(DataNode root, String path) {
        List<PathSegment> segments = parse(path);
        if (segments.isEmpty()) return Optional.empty();

        DataNode current = root;
        for (int i = 0; i < segments.size() - 1; i++) {
            PathSegment seg = segments.get(i);
            Optional<DataNode> next = seg.isIndex()
                    ? current.getElement(seg.getIndex())
                    : current.getChild(seg.getKey());
            if (next.isEmpty()) return Optional.empty();
            current = next.get();
        }

        return Optional.of(new ParentContext(current, segments.get(segments.size() - 1)));
    }

    /**
     * Holds a reference to the parent node and the final path segment,
     * enabling the caller to perform targeted mutations.
     */
    public record ParentContext(DataNode parent, PathSegment lastSegment) {}
}
