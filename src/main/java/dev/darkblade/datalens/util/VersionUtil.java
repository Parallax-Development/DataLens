package dev.darkblade.datalens.util;

import org.bukkit.Bukkit;

/**
 * Utilities for detecting the running server version.
 */
public final class VersionUtil {

    private VersionUtil() {}

    /**
     * Returns the Bukkit version string, e.g. {@code "1.20.4-R0.1-SNAPSHOT"}.
     */
    public static String getBukkitVersion() {
        return Bukkit.getBukkitVersion();
    }

    /**
     * Returns the major.minor version as an integer pair for switch-style comparisons.
     * E.g. "1.20.4-R0.1-SNAPSHOT" → [1, 20, 4].
     */
    public static int[] parseVersion() {
        String raw = getBukkitVersion(); // e.g. "1.20.4-R0.1-SNAPSHOT"
        String[] parts = raw.split("-")[0].split("\\.");
        int[] result = new int[3];
        for (int i = 0; i < Math.min(parts.length, 3); i++) {
            try { result[i] = Integer.parseInt(parts[i]); }
            catch (NumberFormatException ignored) {}
        }
        return result;
    }

    /** Returns true if the server is running at least the given version. */
    public static boolean isAtLeast(int major, int minor) {
        int[] v = parseVersion();
        return v[0] > major || (v[0] == major && v[1] >= minor);
    }
}
