package dev.darkblade.datalens.adapter.common;

import dev.darkblade.datalens.model.DataNode;
import dev.darkblade.datalens.model.InspectableObject;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

/**
 * Abstraction layer for reading and writing Minecraft data.
 * <p>
 * All interactions with NBT, PDC, and other version-specific internals MUST go
 * through this interface. No code outside {@code adapter/versioned/} may access
 * NMS classes directly.
 * <p>
 * Implementations are loaded at runtime by {@link dev.darkblade.datalens.util.AdapterLoader}
 * based on the detected server version.
 */
public interface Adapter {

    /**
     * Reads all available data from a block (PDC, block state, etc.) into a DataNode tree.
     */
    DataNode readBlockData(Block block);

    /**
     * Reads all available data from an entity (PDC, attributes, effects, tags, etc.) into a DataNode tree.
     */
    DataNode readEntityData(Entity entity);

    /**
     * Reads all available data from an ItemStack (PDC, enchantments, meta, etc.) into a DataNode tree.
     */
    DataNode readItemData(ItemStack item);

    /**
     * Writes the modified DataNode tree back to the underlying Minecraft object.
     * Must be called on the main server thread for world-affecting writes.
     */
    void writeData(InspectableObject object, DataNode node);

    /**
     * Returns the version string this adapter targets (e.g. {@code "1.20"}).
     */
    String getSupportedVersion();
}
