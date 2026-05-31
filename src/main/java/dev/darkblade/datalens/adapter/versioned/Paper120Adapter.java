package dev.darkblade.datalens.adapter.versioned;

import de.tr7zw.changeme.nbtapi.NBTBlock;
import de.tr7zw.changeme.nbtapi.NBTEntity;
import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.NBTTileEntity;
import dev.darkblade.datalens.adapter.common.Adapter;
import dev.darkblade.datalens.model.DataNode;
import dev.darkblade.datalens.model.DataType;
import dev.darkblade.datalens.model.InspectableObject;
import dev.darkblade.datalens.model.InspectableType;
import dev.darkblade.datalens.util.NbtUtil;
import dev.darkblade.datalens.util.PdcUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataHolder;

/**
 * Adapter implementation for Paper 1.20.x.
 * Uses NBT-API for deep dynamic inspection of Items, Entities, and Blocks.
 */
public class Paper120Adapter implements Adapter {

    @Override
    public String getSupportedVersion() { return "1.20"; }

    // ── Block ─────────────────────────────────────────────────────────────────

    @Override
    public DataNode readBlockData(Block block) {
        BlockState state = block.getState();
        if (state instanceof org.bukkit.block.TileState) {
            try {
                NBTTileEntity nbtTile = new NBTTileEntity(state);
                DataNode root = NbtUtil.readCompound("block", nbtTile);
                
                // Keep some basic Bukkit context
                root.addChild(DataNode.ofPrimitive("bukkit_type", DataType.STRING, block.getType().name()));
                root.addChild(DataNode.ofPrimitive("bukkit_world", DataType.STRING, block.getWorld().getName()));
                root.addChild(DataNode.ofPrimitive("bukkit_x", DataType.INT, block.getX()));
                root.addChild(DataNode.ofPrimitive("bukkit_y", DataType.INT, block.getY()));
                root.addChild(DataNode.ofPrimitive("bukkit_z", DataType.INT, block.getZ()));
                return root;
            } catch (Exception ignored) {}
        }

        // Fallback for non-tile entities
        DataNode root = DataNode.ofCompound("block");
        root.addChild(DataNode.ofPrimitive("bukkit_type", DataType.STRING, block.getType().name()));
        root.addChild(DataNode.ofPrimitive("bukkit_world", DataType.STRING, block.getWorld().getName()));
        root.addChild(DataNode.ofPrimitive("bukkit_x", DataType.INT, block.getX()));
        root.addChild(DataNode.ofPrimitive("bukkit_y", DataType.INT, block.getY()));
        root.addChild(DataNode.ofPrimitive("bukkit_z", DataType.INT, block.getZ()));
        root.addChild(DataNode.ofPrimitive("blockData", DataType.STRING, block.getBlockData().getAsString()));

        if (state instanceof PersistentDataHolder holder) {
            root.addChild(PdcUtil.readPdc(holder.getPersistentDataContainer()));
        }

        return root;
    }

    // ── Entity ────────────────────────────────────────────────────────────────

    @Override
    public DataNode readEntityData(Entity entity) {
        try {
            NBTEntity nbtEntity = new NBTEntity(entity);
            DataNode root = NbtUtil.readCompound("entity", nbtEntity);
            root.addChild(DataNode.ofPrimitive("bukkit_type", DataType.STRING, entity.getType().name()));
            root.addChild(DataNode.ofPrimitive("bukkit_uuid", DataType.UUID, entity.getUniqueId()));
            return root;
        } catch (Exception e) {
            DataNode root = DataNode.ofCompound("entity");
            root.addChild(DataNode.ofPrimitive("bukkit_type", DataType.STRING, entity.getType().name()));
            root.addChild(DataNode.ofPrimitive("bukkit_uuid", DataType.UUID, entity.getUniqueId()));
            root.addChild(DataNode.ofPrimitive("name", DataType.STRING, entity.getName()));
            if (entity instanceof PersistentDataHolder holder) {
                root.addChild(PdcUtil.readPdc(holder.getPersistentDataContainer()));
            }
            return root;
        }
    }

    // ── ItemStack ─────────────────────────────────────────────────────────────

    @Override
    public DataNode readItemData(ItemStack item) {
        DataNode root = DataNode.ofCompound("item");

        // 1. Standard
        DataNode standard = DataNode.ofCompound("standard");
        standard.addChild(DataNode.ofPrimitive("type", DataType.STRING, item.getType().name()));
        standard.addChild(DataNode.ofPrimitive("amount", DataType.INT, item.getAmount()));
        
        if (item.hasItemMeta()) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                standard.addChild(DataNode.ofPrimitive("displayName", DataType.STRING, meta.getDisplayName()));
            }
            if (meta.hasLore()) {
                DataNode loreList = DataNode.ofList("lore");
                for (int i = 0; i < meta.getLore().size(); i++) {
                    loreList.addChild(DataNode.ofPrimitive("[" + i + "]", DataType.STRING, meta.getLore().get(i)));
                }
                standard.addChild(loreList);
            }
            if (meta.hasCustomModelData()) {
                standard.addChild(DataNode.ofPrimitive("customModelData", DataType.INT, meta.getCustomModelData()));
            }
            if (meta instanceof org.bukkit.inventory.meta.Damageable dmg) {
                standard.addChild(DataNode.ofPrimitive("damage", DataType.INT, dmg.getDamage()));
            }
            standard.addChild(DataNode.ofPrimitive("unbreakable", DataType.BOOLEAN, meta.isUnbreakable()));
        }
        root.addChild(standard);

        // 2. NBT
        try {
            NBTItem nbtItem = new NBTItem(item);
            DataNode nbtNode = NbtUtil.readCompound("nbt", nbtItem);
            root.addChild(nbtNode);
        } catch (Exception ignored) {
            root.addChild(DataNode.ofCompound("nbt"));
        }

        // 3. Raw Meta
        try {
            if (item.hasItemMeta()) {
                java.util.Map<String, Object> serializedMeta = item.getItemMeta().serialize();
                DataNode metaNode = dev.darkblade.datalens.util.MapUtil.toNode("meta", serializedMeta);
                root.addChild(metaNode);
            } else {
                root.addChild(DataNode.ofCompound("meta"));
            }
        } catch (Exception e) {
            root.addChild(DataNode.ofCompound("meta"));
        }

        return root;
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    @Override
    public void writeData(InspectableObject object, DataNode node) {
        if (object.getType() == InspectableType.BLOCK && object.getLiveReference() instanceof Block block) {
            BlockState state = block.getState();
            if (state instanceof org.bukkit.block.TileState) {
                try {
                    NBTTileEntity nbtTile = new NBTTileEntity(state);
                    NbtUtil.writeCompound(node, nbtTile);
                    return;
                } catch (Exception ignored) {}
            }
            
            // Fallback to PDC
            if (state instanceof PersistentDataHolder holder) {
                node.getChild("pdc").ifPresent(pdcNode -> writePdcNode(pdcNode, holder.getPersistentDataContainer(), state));
            }
            
        } else if (object.getType() == InspectableType.ITEM && object.getLiveReference() instanceof ItemStack item) {
            // Apply Raw Meta first (overwrites ItemMeta completely)
            node.getChild("meta").ifPresent(metaNode -> {
                try {
                    java.util.Map<String, Object> map = dev.darkblade.datalens.util.MapUtil.toMap(metaNode);
                    if (!map.isEmpty()) {
                        if (!map.containsKey("==")) {
                            map.put("==", "ItemMeta");
                        }
                        org.bukkit.inventory.meta.ItemMeta deserialized = (org.bukkit.inventory.meta.ItemMeta) org.bukkit.configuration.serialization.ConfigurationSerialization.deserializeObject(map);
                        if (deserialized != null) {
                            item.setItemMeta(deserialized);
                        }
                    }
                } catch (Exception ignored) {}
            });

            // Apply Standard (mutates ItemMeta and ItemStack)
            node.getChild("standard").ifPresent(std -> {
                std.getChild("type").ifPresent(t -> {
                    try { item.setType(org.bukkit.Material.valueOf((String) t.getValue())); } catch (Exception ignored) {}
                });
                std.getChild("amount").ifPresent(a -> item.setAmount((int) a.getValue()));

                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    std.getChild("displayName").ifPresent(dn -> meta.setDisplayName((String) dn.getValue()));
                    std.getChild("lore").ifPresent(l -> {
                        java.util.List<String> lore = new java.util.ArrayList<>();
                        for (DataNode child : l.getChildren()) {
                            lore.add((String) child.getValue());
                        }
                        meta.setLore(lore);
                    });
                    std.getChild("customModelData").ifPresent(cmd -> meta.setCustomModelData((int) cmd.getValue()));
                    std.getChild("damage").ifPresent(dmg -> {
                        if (meta instanceof org.bukkit.inventory.meta.Damageable d) d.setDamage((int) dmg.getValue());
                    });
                    std.getChild("unbreakable").ifPresent(ub -> meta.setUnbreakable((boolean) ub.getValue()));
                    item.setItemMeta(meta);
                }
            });

            // Apply NBT (Overrides some stuff, usually custom data)
            node.getChild("nbt").ifPresent(nbtNode -> {
                try {
                    NBTItem nbtItem = new NBTItem(item);
                    NbtUtil.writeCompound(nbtNode, nbtItem);
                    nbtItem.applyNBT(item);
                } catch (Exception ignored) {}
            });
            
        } else if (object.getType() == InspectableType.ENTITY && object.getLiveReference() instanceof Entity entity) {
            try {
                NBTEntity nbtEntity = new NBTEntity(entity);
                NbtUtil.writeCompound(node, nbtEntity);
            } catch (Exception e) {
                // Fallback to PDC
                node.getChild("pdc").ifPresent(pdcNode -> {
                    if (entity instanceof PersistentDataHolder holder) {
                        writePdcNode(pdcNode, holder.getPersistentDataContainer(), null);
                    }
                });
            }
        }
    }

    /** Writes all children of a COMPOUND pdc node back into a PersistentDataContainer. */
    private void writePdcNode(DataNode pdcNode, org.bukkit.persistence.PersistentDataContainer pdc, BlockState state) {
        for (DataNode child : pdcNode.getChildren()) {
            String[] parts = child.getKey().split(":", 2);
            if (parts.length != 2) continue;
            NamespacedKey key = new NamespacedKey(parts[0], parts[1]);
            PdcUtil.writeNode(pdc, key, child);
        }
        if (state != null) {
            state.update(true);
        }
    }
}
