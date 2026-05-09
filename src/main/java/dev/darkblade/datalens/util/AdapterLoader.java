package dev.darkblade.datalens.util;

import dev.darkblade.datalens.adapter.common.Adapter;
import dev.darkblade.datalens.adapter.versioned.Paper120Adapter;
import dev.darkblade.datalens.adapter.versioned.Paper121Adapter;

/**
 * Detects the running server version and instantiates the appropriate {@link Adapter}.
 * <p>
 * To add support for a new Minecraft version, create a new adapter in
 * {@code adapter/versioned/} and register it here.
 */
public final class AdapterLoader {

    private AdapterLoader() {}

    /**
     * Loads the best-fit adapter for the current server.
     *
     * @throws IllegalStateException if no compatible adapter is found
     */
    public static Adapter load() {
        int[] v = VersionUtil.parseVersion();
        int major = v[0];
        int minor = v[1];

        if (major == 1 && minor >= 21) {
            return new Paper121Adapter();
        }
        if (major == 1 && minor == 20) {
            return new Paper120Adapter();
        }

        throw new IllegalStateException(
            "DataLens: No adapter available for server version " + VersionUtil.getBukkitVersion()
            + ". Supported: 1.20.x, 1.21.x"
        );
    }
}
