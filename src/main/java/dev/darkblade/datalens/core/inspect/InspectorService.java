package dev.darkblade.datalens.core.inspect;

import dev.darkblade.datalens.adapter.common.Adapter;
import dev.darkblade.datalens.model.DataNode;
import dev.darkblade.datalens.model.InspectableObject;
import dev.darkblade.datalens.model.InspectableType;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Converts live Minecraft objects into {@link InspectableObject} trees
 * by delegating data reading to the active {@link Adapter}.
 * <p>
 * All methods are safe to call from async threads — reading does not mutate world state.
 */
public final class InspectorService {

    private final Adapter adapter;

    public InspectorService(Adapter adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    /**
     * Inspects a block and returns a full data snapshot.
     */
    public InspectableObject inspect(Block block) {
        DataNode root = adapter.readBlockData(block);
        String id = block.getType().name() + "@" + block.getX() + "," + block.getY() + "," + block.getZ();
        return new InspectableObject(InspectableType.BLOCK, id, block.getLocation(), root, block);
    }

    /**
     * Inspects an entity and returns a full data snapshot.
     */
    public InspectableObject inspect(Entity entity) {
        DataNode root = adapter.readEntityData(entity);
        String id = entity.getType().name() + "#" + entity.getUniqueId().toString().substring(0, 8);
        return new InspectableObject(InspectableType.ENTITY, id, entity.getLocation(), root, entity);
    }

    /**
     * Inspects an ItemStack and returns a data snapshot.
     * No location is associated with item inspections.
     */
    public InspectableObject inspect(ItemStack item) {
        DataNode root = adapter.readItemData(item);
        String id = item.getType().name() + "x" + item.getAmount();
        return new InspectableObject(InspectableType.ITEM, id, null, root, item);
    }
}
