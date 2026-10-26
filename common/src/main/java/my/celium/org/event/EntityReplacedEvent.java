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

    /**
     * Created by {@code EntityReplacer}; mods only receive it.
     *
     * @param oldType     registry id of the removed entity type
     * @param x           its last x position
     * @param y           its last y position
     * @param z           its last z position
     * @param replacement the live replacement
     */
    public EntityReplacedEvent(String oldType, double x, double y, double z, Entity replacement) {
        this.oldType = oldType;
        this.x = x;
        this.y = y;
        this.z = z;
        this.replacement = replacement;
    }

    /**
     * Registry id of the removed entity type.
     *
     * @return e.g. {@code "minecraft:zombie"}
     */
    public String oldType() {
        return oldType;
    }

    /**
     * The removed entity's last x position.
     *
     * @return the x coordinate
     */
    public double x() {
        return x;
    }

    /**
     * The removed entity's last y position.
     *
     * @return the y coordinate
     */
    public double y() {
        return y;
    }

    /**
     * The removed entity's last z position.
     *
     * @return the z coordinate
     */
    public double z() {
        return z;
    }

    /**
     * The live replacement entity.
     *
     * @return the replacement
     */
    public Entity replacement() {
        return replacement;
    }
}
