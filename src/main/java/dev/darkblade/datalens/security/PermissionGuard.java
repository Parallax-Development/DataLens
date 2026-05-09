package dev.darkblade.datalens.security;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Centralised permission gate for all DataLens actions.
 * All permission checks MUST go through this class.
 */
public final class PermissionGuard {

    public static final String INSPECT = "datalens.inspect";
    public static final String EDIT    = "datalens.edit";
    public static final String ADMIN   = "datalens.admin";

    private PermissionGuard() {}

    /**
     * Returns true if the sender has the given permission.
     */
    public static boolean has(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }

    /**
     * Checks the permission and throws {@link PermissionException} if denied.
     */
    public static void require(CommandSender sender, String permission) {
        if (!has(sender, permission)) {
            throw new PermissionException("You do not have permission: " + permission);
        }
    }

    /**
     * Convenience: require that the sender is a {@link Player}.
     */
    public static Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            throw new PermissionException("This command can only be run by a player.");
        }
        return player;
    }

    public static final class PermissionException extends RuntimeException {
        public PermissionException(String msg) { super(msg); }
    }
}
