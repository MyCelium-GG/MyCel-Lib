package my.celium.org.event;

import net.minecraft.world.entity.Entity;

/**
 * Fired after {@code EntityReplacer} swapped one entity for another.
 *
 * <p>The old entity has already been discarded when this fires; its type and
 * last known position are captured so handlers can react (particles, sounds)
 * without touching a dead entity.
 */
public final class EntityReplacedEvent {
    private final String oldType;
    private final double x;
    private final double y;
    private final double z;
    private final Entity replacement;

    public EntityReplacedEvent(String oldType, double x, double y, double z, Entity replacement) {
        this.oldType = oldType;
        this.x = x;
        this.y = y;
        this.z = z;
        this.replacement = replacement;
    }

    /** Registry id of the removed entity type. */
    public String oldType() {
        return oldType;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    /** The live replacement entity. */
    public Entity replacement() {
        return replacement;
    }
}
