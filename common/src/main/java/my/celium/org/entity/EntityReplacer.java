package my.celium.org.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import my.celium.org.event.EntityReplacedEvent;
import my.celium.org.event.MycelEvents;
import my.celium.org.logging.MycelLog;
import my.celium.org.util.EntityUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Leashable.LeashData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Reusable entity replacement: swap one entity for another while preserving
 * the state callers ask for (see {@link ReplaceOptions}).
 *
 * <p>Must run on the server thread of the entity's level; any other thread
 * fails fast with a clear message instead of corrupting the world. Fires
 * {@link EntityReplacedEvent} after the swap.
 */
public final class EntityReplacer {
    private EntityReplacer() {
    }

    /**
     * Discards {@code oldEntity} and spawns {@code fresh} in its place.
     *
     * @param level the server level both entities belong to
     * @param oldEntity the entity to remove (must be alive and in {@code level})
     * @param fresh the replacement (must not be added to any level yet)
     * @param options what to carry over
     * @return the spawned replacement
     */
    public static <T extends Entity> T replace(ServerLevel level, Entity oldEntity, T fresh,
            ReplaceOptions options) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(oldEntity, "oldEntity");
        Objects.requireNonNull(fresh, "fresh");
        Objects.requireNonNull(options, "options");
        if (level.isClientSide()) {
            throw new IllegalArgumentException("EntityReplacer must run on the server level");
        }
        if (Thread.currentThread() != level.getServer().getRunningThread()) {
            throw new IllegalStateException("EntityReplacer must run on the server thread");
        }
        if (oldEntity.isRemoved()) {
            throw new IllegalArgumentException("Old entity is already removed");
        }
        if (!fresh.isRemoved() && fresh.level() != null && fresh.level() != level) {
            throw new IllegalArgumentException("Replacement entity belongs to another level");
        }

        String oldType = BuiltInRegistries.ENTITY_TYPE.getKey(oldEntity.getType()).toString();
        double x = oldEntity.getX();
        double y = oldEntity.getY();
        double z = oldEntity.getZ();

        if (options.preservePosition() && options.preserveRotation()) {
            fresh.snapTo(x, y, z, oldEntity.getYRot(), oldEntity.getXRot());
        } else if (options.preservePosition()) {
            fresh.snapTo(x, y, z);
        }
        if (options.preserveVelocity()) {
            fresh.setDeltaMovement(oldEntity.getDeltaMovement());
        }
        if (options.preserveName()) {
            EntityUtil.copyName(oldEntity, fresh);
        }
        if (options.preserveFlags()) {
            fresh.setPermanentlyInvulnerable(oldEntity.isPermanentlyInvulnerable());
            fresh.setNoGravity(oldEntity.isNoGravity());
            fresh.setSilent(oldEntity.isSilent());
            fresh.setGlowingTag(oldEntity.hasGlowingTag());
        }
        if (options.preserveEquipment() && oldEntity instanceof LivingEntity oldLiving && fresh instanceof LivingEntity freshLiving) {
            EntityUtil.copyEquipment(oldLiving, freshLiving);
        }
        if (options.preserveHealth() && oldEntity instanceof LivingEntity oldLiving && fresh instanceof LivingEntity freshLiving) {
            freshLiving.setHealth(Math.min(freshLiving.getMaxHealth(), oldLiving.getHealth()));
        }
        if (options.preservePersistence() && fresh instanceof Mob freshMob) {
            if (oldEntity instanceof Mob) {
                EntityUtil.persist(freshMob);
            } else {
                freshMob.setPersistenceRequired();
            }
        }

        Entity leashHolder = null;
        if (options.preserveLeash() && oldEntity instanceof Leashable oldLeashable) {
            LeashData data = oldLeashable.getLeashData();
            if (data != null) {
                leashHolder = data.leashHolder;
            }
        }

        List<Entity> passengers = new ArrayList<>();
        if (options.preservePassengers()) {
            passengers.addAll(oldEntity.getPassengers());
            for (Entity passenger : passengers) {
                passenger.stopRiding();
            }
        }

        level.addFreshEntity(fresh);

        for (Entity passenger : passengers) {
            if (!passenger.isRemoved()) {
                passenger.startRiding(fresh);
            }
        }
        if (leashHolder != null && fresh instanceof Leashable freshLeashable && !leashHolder.isRemoved()) {
            try {
                freshLeashable.setLeashedTo(leashHolder, true);
            } catch (RuntimeException e) {
                MycelLog.debug("Could not restore leash on replacement: {}", e.toString());
            }
        }

        oldEntity.discard();
        MycelEvents.fireEntityReplaced(new EntityReplacedEvent(oldType, x, y, z, fresh));
        return fresh;
    }
}
