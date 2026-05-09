package dev.darkblade.datalens.adapter.versioned;

import dev.darkblade.datalens.adapter.common.Adapter;
import dev.darkblade.datalens.model.DataNode;
import dev.darkblade.datalens.model.DataType;
import dev.darkblade.datalens.model.InspectableObject;
import dev.darkblade.datalens.model.InspectableType;
import dev.darkblade.datalens.util.PdcUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.potion.PotionEffect;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.Map;

/**
 * Adapter implementation for Paper 1.20.x.
 * Uses only the public Paper API — no NMS access.
 * To add deep NBT support later, extend this class or create a parallel NMS-backed adapter.
 */
public class Paper120Adapter implements Adapter {

    @Override
    public String getSupportedVersion() { return "1.20"; }

    // ── Block ─────────────────────────────────────────────────────────────────

    @Override
    public DataNode readBlockData(Block block) {
        DataNode root = DataNode.ofCompound("block");

        // Basic metadata
        root.addChild(DataNode.ofPrimitive("type", DataType.STRING, block.getType().name()));
        root.addChild(DataNode.ofPrimitive("world", DataType.STRING, block.getWorld().getName()));
        root.addChild(DataNode.ofPrimitive("x", DataType.INT, block.getX()));
        root.addChild(DataNode.ofPrimitive("y", DataType.INT, block.getY()));
        root.addChild(DataNode.ofPrimitive("z", DataType.INT, block.getZ()));

        // BlockData state string (e.g. "minecraft:chest[facing=north,waterlogged=false]")
        root.addChild(DataNode.ofPrimitive("blockData", DataType.STRING, block.getBlockData().getAsString()));

        // PDC (only if the BlockState is a PersistentDataHolder — e.g. chests, signs)
        BlockState state = block.getState();
        if (state instanceof PersistentDataHolder holder) {
            root.addChild(PdcUtil.readPdc(holder.getPersistentDataContainer()));
        }

        return root;
    }

    // ── Entity ────────────────────────────────────────────────────────────────

    @Override
    public DataNode readEntityData(Entity entity) {
        DataNode root = DataNode.ofCompound("entity");

        root.addChild(DataNode.ofPrimitive("type", DataType.STRING, entity.getType().name()));
        root.addChild(DataNode.ofPrimitive("uuid", DataType.UUID, entity.getUniqueId()));
        root.addChild(DataNode.ofPrimitive("name", DataType.STRING, entity.getName()));
        root.addChild(DataNode.ofPrimitive("world", DataType.STRING, entity.getWorld().getName()));
        root.addChild(DataNode.ofPrimitive("x", DataType.DOUBLE, entity.getLocation().getX()));
        root.addChild(DataNode.ofPrimitive("y", DataType.DOUBLE, entity.getLocation().getY()));
        root.addChild(DataNode.ofPrimitive("z", DataType.DOUBLE, entity.getLocation().getZ()));
        root.addChild(DataNode.ofPrimitive("customName",
                DataType.STRING, entity.customName() != null ? entity.customName().toString() : ""));

        // Scoreboard tags
        DataNode tags = DataNode.ofList("tags");
        for (String tag : entity.getScoreboardTags()) {
            tags.addChild(DataNode.ofPrimitive(tag, DataType.STRING, tag));
        }
        root.addChild(tags);

        // PDC
        root.addChild(PdcUtil.readPdc(entity.getPersistentDataContainer()));

        // Living entity extras
        if (entity instanceof LivingEntity living) {
            root.addChild(readAttributes(living));
            root.addChild(readEffects(living));
            root.addChild(DataNode.ofPrimitive("health", DataType.DOUBLE, living.getHealth()));
        }

        // Player-specific extras
        if (entity instanceof Player p) {
            root.addChild(readPlayerData(p));
        }

        return root;
    }

    private DataNode readAttributes(LivingEntity entity) {
        DataNode attrs = DataNode.ofCompound("attributes");
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null) {
                String key = attribute.getKey().toString();
                attrs.addChild(DataNode.ofPrimitive(key, DataType.DOUBLE, instance.getValue()));
            }
        }
        return attrs;
    }

    private DataNode readEffects(LivingEntity entity) {
        DataNode effects = DataNode.ofList("effects");
        for (PotionEffect effect : entity.getActivePotionEffects()) {
            DataNode effectNode = DataNode.ofCompound(effect.getType().getName());
            effectNode.addChild(DataNode.ofPrimitive("amplifier", DataType.INT, effect.getAmplifier()));
            effectNode.addChild(DataNode.ofPrimitive("duration", DataType.INT, effect.getDuration()));
            effectNode.addChild(DataNode.ofPrimitive("ambient", DataType.BOOLEAN, effect.isAmbient()));
            effects.addChild(effectNode);
        }
        return effects;
    }

    private DataNode readPlayerData(Player p) {
        DataNode node = DataNode.ofCompound("player");

        // Identity & session
        node.addChild(DataNode.ofPrimitive("gameMode",     DataType.STRING,  p.getGameMode().name()));
        node.addChild(DataNode.ofPrimitive("ping",         DataType.INT,     p.getPing()));
        node.addChild(DataNode.ofPrimitive("locale",       DataType.STRING,  p.locale().toString()));
        node.addChild(DataNode.ofPrimitive("isOp",         DataType.BOOLEAN, p.isOp()));

        // Address (may be null for offline/proxy scenarios)
        InetAddress addr = p.getAddress() != null ? p.getAddress().getAddress() : null;
        node.addChild(DataNode.ofPrimitive("ip",           DataType.STRING,  addr != null ? addr.getHostAddress() : "unknown"));

        // Experience & levels
        node.addChild(DataNode.ofPrimitive("expLevel",     DataType.INT,     p.getLevel()));
        node.addChild(DataNode.ofPrimitive("expProgress",  DataType.FLOAT,   p.getExp()));
        node.addChild(DataNode.ofPrimitive("totalExp",     DataType.INT,     p.getTotalExperience()));

        // Survival stats
        node.addChild(DataNode.ofPrimitive("foodLevel",    DataType.INT,     p.getFoodLevel()));
        node.addChild(DataNode.ofPrimitive("saturation",   DataType.FLOAT,   p.getSaturation()));
        node.addChild(DataNode.ofPrimitive("exhaustion",   DataType.FLOAT,   p.getExhaustion()));
        node.addChild(DataNode.ofPrimitive("airTicks",     DataType.INT,     p.getRemainingAir()));
        node.addChild(DataNode.ofPrimitive("fireTicks",    DataType.INT,     p.getFireTicks()));

        // Flight
        node.addChild(DataNode.ofPrimitive("allowFlight",  DataType.BOOLEAN, p.getAllowFlight()));
        node.addChild(DataNode.ofPrimitive("isFlying",     DataType.BOOLEAN, p.isFlying()));
        node.addChild(DataNode.ofPrimitive("flySpeed",     DataType.FLOAT,   p.getFlySpeed()));
        node.addChild(DataNode.ofPrimitive("walkSpeed",    DataType.FLOAT,   p.getWalkSpeed()));

        // Hand items (non-null only)
        if (p.getInventory().getItemInMainHand().getType().isItem()) {
            node.addChild(DataNode.ofPrimitive("mainHand",
                    DataType.STRING, p.getInventory().getItemInMainHand().getType().name()));
        }
        if (p.getInventory().getItemInOffHand().getType().isItem()) {
            node.addChild(DataNode.ofPrimitive("offHand",
                    DataType.STRING, p.getInventory().getItemInOffHand().getType().name()));
        }

        // Armour slots
        DataNode armour = DataNode.ofCompound("armour");
        org.bukkit.inventory.ItemStack[] armourContents = p.getInventory().getArmorContents();
        String[] slots = {"boots", "leggings", "chestplate", "helmet"};
        for (int i = 0; i < armourContents.length; i++) {
            org.bukkit.inventory.ItemStack piece = armourContents[i];
            String material = (piece != null) ? piece.getType().name() : "AIR";
            armour.addChild(DataNode.ofPrimitive(slots[i], DataType.STRING, material));
        }
        node.addChild(armour);

        return node;
    }

    // ── ItemStack ─────────────────────────────────────────────────────────────

    @Override
    public DataNode readItemData(ItemStack item) {
        DataNode root = DataNode.ofCompound("item");

        root.addChild(DataNode.ofPrimitive("type", DataType.STRING, item.getType().name()));
        root.addChild(DataNode.ofPrimitive("amount", DataType.INT, item.getAmount()));

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return root;

        // Display name
        if (meta.hasDisplayName()) {
            root.addChild(DataNode.ofPrimitive("displayName", DataType.STRING,
                    meta.displayName() != null ? meta.displayName().toString() : ""));
        }

        // Custom model data
        if (meta.hasCustomModelData()) {
            root.addChild(DataNode.ofPrimitive("customModelData", DataType.INT, meta.getCustomModelData()));
        }

        // Durability
        if (meta instanceof Damageable damageable) {
            root.addChild(DataNode.ofPrimitive("damage", DataType.INT, damageable.getDamage()));
        }

        // Enchantments
        DataNode enchants = DataNode.ofCompound("enchantments");
        meta.getEnchants().forEach((ench, level) ->
                enchants.addChild(DataNode.ofPrimitive(ench.getKey().toString(), DataType.INT, level)));
        root.addChild(enchants);

        // Lore
        if (meta.hasLore() && meta.lore() != null) {
            DataNode lore = DataNode.ofList("lore");
            for (int i = 0; i < meta.lore().size(); i++) {
                lore.addChild(DataNode.ofPrimitive("[" + i + "]", DataType.STRING, meta.lore().get(i).toString()));
            }
            root.addChild(lore);
        }

        // Item flags
        DataNode flags = DataNode.ofList("itemFlags");
        meta.getItemFlags().forEach(f ->
                flags.addChild(DataNode.ofPrimitive(f.name(), DataType.STRING, f.name())));
        root.addChild(flags);

        // PDC
        root.addChild(PdcUtil.readPdc(meta.getPersistentDataContainer()));

        return root;
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    @Override
    public void writeData(InspectableObject object, DataNode node) {
        // Locate the "pdc" compound node and write it back
        node.getChild("pdc").ifPresent(pdcNode -> {
            if (object.getType() == InspectableType.BLOCK && object.getLocation() != null) {
                BlockState state = object.getLocation().getBlock().getState();
                if (state instanceof PersistentDataHolder holder) {
                    writePdcNode(pdcNode, holder.getPersistentDataContainer(), state);
                }
            }
            // Entity and Item write-back is handled by EditService after retrieving the live object.
        });
    }

    /** Writes all children of a COMPOUND pdc node back into a PersistentDataContainer. */
    private void writePdcNode(DataNode pdcNode, org.bukkit.persistence.PersistentDataContainer pdc, BlockState state) {
        for (DataNode child : pdcNode.getChildren()) {
            String[] parts = child.getKey().split(":", 2);
            if (parts.length != 2) continue;
            NamespacedKey key = new NamespacedKey(parts[0], parts[1]);
            PdcUtil.writeNode(pdc, key, child);
        }
        state.update(true);
    }
}
