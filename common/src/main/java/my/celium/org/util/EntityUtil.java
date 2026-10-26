package my.celium.org.util;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Small cohesive entity helpers. */
public final class EntityUtil {
    private EntityUtil() {
    }

    /**
     * Entities of {@code type} in a box reaching {@code radius} blocks out from the block at {@code center}.
     *
     * @param <T>    the entity type
     * @param level  the level to search
     * @param center the block in the middle
     * @param radius how far out, in blocks (negative counts as {@code 0})
     * @param type   the entity class, e.g. {@code Wolf.class}
     * @return the entities found; a new list
     */
    public static <T extends Entity> List<T> getNearbyEntities(Level level, BlockPos center, double radius,
            Class<T> type) {
        double r = Math.max(0, radius);
        AABB box = new AABB(center).inflate(r);
        return level.getEntitiesOfClass(type, box);
    }

    /**
     * Entities of {@code type} within {@code radius} blocks of {@code center}'s bounding box, not counting {@code center} itself.
     *
     * @param <T>    the entity type
     * @param level  the level to search
     * @param center the entity in the middle
     * @param radius how far out, in blocks (negative counts as {@code 0})
     * @param type   the entity class, e.g. {@code Wolf.class}
     * @return the entities found; a new list
     */
    public static <T extends Entity> List<T> getNearbyEntities(Level level, Entity center, double radius,
            Class<T> type) {
        double r = Math.max(0, radius);
        AABB box = center.getBoundingBox().inflate(r);
        return level.getEntitiesOfClass(type, box, entity -> entity != center);
    }

    /**
     * Players in a box reaching {@code radius} blocks out from the block at {@code center}.
     *
     * @param level  the level to search
     * @param center the block in the middle
     * @param radius how far out, in blocks
     * @return the players found; a new list
     */
    public static List<Player> getNearbyPlayers(Level level, BlockPos center, double radius) {
        return getNearbyEntities(level, center, radius, Player.class);
    }

    /**
     * Whether {@code entity} is a {@link Mob} (has AI: animals, monsters, villagers).
     *
     * @param entity the entity
     * @return {@code true} for a mob
     */
    public static boolean isMob(Entity entity) {
        return entity instanceof Mob;
    }

    /**
     * Copies every equipment slot (armour + hands), duplicating the stacks.
     *
     * @param from the entity to copy from
     * @param to   the entity to copy to
     */
    public static void copyEquipment(LivingEntity from, LivingEntity to) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            to.setItemSlot(slot, from.getItemBySlot(slot).copy());
        }
    }

    /**
     * Copies the custom name and its visibility flag.
     *
     * @param from the entity to copy from
     * @param to   the entity to copy to
     */
    public static void copyName(Entity from, Entity to) {
        if (from.hasCustomName()) {
            to.setCustomName(from.getCustomName());
        }
        to.setCustomNameVisible(from.isCustomNameVisible());
    }

    /**
     * Marks a mob persistent so it never despawns.
     *
     * @param mob the mob
     */
    public static void persist(Mob mob) {
        mob.setPersistenceRequired();
    }
}
