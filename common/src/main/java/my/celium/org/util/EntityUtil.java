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

    public static <T extends Entity> List<T> getNearbyEntities(Level level, BlockPos center, double radius,
            Class<T> type) {
        double r = Math.max(0, radius);
        AABB box = new AABB(center).inflate(r);
        return level.getEntitiesOfClass(type, box);
    }

    public static <T extends Entity> List<T> getNearbyEntities(Level level, Entity center, double radius,
            Class<T> type) {
        double r = Math.max(0, radius);
        AABB box = center.getBoundingBox().inflate(r);
        return level.getEntitiesOfClass(type, box, entity -> entity != center);
    }

    public static List<Player> getNearbyPlayers(Level level, BlockPos center, double radius) {
        return getNearbyEntities(level, center, radius, Player.class);
    }

    public static boolean isMob(Entity entity) {
        return entity instanceof Mob;
    }

    /** Copies every equipment slot (armour + hands), duplicating the stacks. */
    public static void copyEquipment(LivingEntity from, LivingEntity to) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            to.setItemSlot(slot, from.getItemBySlot(slot).copy());
        }
    }

    /** Copies the custom name and its visibility flag. */
    public static void copyName(Entity from, Entity to) {
        if (from.hasCustomName()) {
            to.setCustomName(from.getCustomName());
        }
        to.setCustomNameVisible(from.isCustomNameVisible());
    }

    /** Marks a mob persistent so it never despawns. */
    public static void persist(Mob mob) {
        mob.setPersistenceRequired();
    }
}
