package dev.darkblade.datalens.adapter.versioned;

/**
 * Adapter implementation for Paper 1.21.x.
 * <p>
 * 1.21 introduced no breaking changes to PDC or Attribute APIs used here,
 * so this adapter extends Paper120Adapter directly.
 * Override specific methods here as the 1.21 API diverges.
 */
public class Paper121Adapter extends Paper120Adapter {

    @Override
    public String getSupportedVersion() { return "1.21"; }

    // Future: override readBlockData / readEntityData / readItemData as needed
    // when 1.21-specific APIs (e.g. new entity types, attribute changes) require it.
}
