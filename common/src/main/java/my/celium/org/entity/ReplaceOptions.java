package my.celium.org.entity;

/**
 * What {@code EntityReplacer} preserves when swapping entities.
 * {@link #DEFAULT} preserves everything it safely can.
 */
public final class ReplaceOptions {
    public static final ReplaceOptions DEFAULT = builder().build();

    private final boolean position;
    private final boolean rotation;
    private final boolean velocity;
    private final boolean name;
    private final boolean equipment;
    private final boolean health;
    private final boolean persistence;
    private final boolean passengers;
    private final boolean leash;
    private final boolean flags;

    private ReplaceOptions(Builder builder) {
        this.position = builder.position;
        this.rotation = builder.rotation;
        this.velocity = builder.velocity;
        this.name = builder.name;
        this.equipment = builder.equipment;
        this.health = builder.health;
        this.persistence = builder.persistence;
        this.passengers = builder.passengers;
        this.leash = builder.leash;
        this.flags = builder.flags;
    }

    public boolean preservePosition() {
        return position;
    }

    public boolean preserveRotation() {
        return rotation;
    }

    public boolean preserveVelocity() {
        return velocity;
    }

    public boolean preserveName() {
        return name;
    }

    public boolean preserveEquipment() {
        return equipment;
    }

    public boolean preserveHealth() {
        return health;
    }

    public boolean preservePersistence() {
        return persistence;
    }

    public boolean preservePassengers() {
        return passengers;
    }

    public boolean preserveLeash() {
        return leash;
    }

    /** Permanent invulnerability, no-gravity, silence and the glow tag. */
    public boolean preserveFlags() {
        return flags;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** All flags default to {@code true}; disable what a feature must not carry over. */
    public static final class Builder {
        private boolean position = true;
        private boolean rotation = true;
        private boolean velocity = true;
        private boolean name = true;
        private boolean equipment = true;
        private boolean health = true;
        private boolean persistence = true;
        private boolean passengers = true;
        private boolean leash = true;
        private boolean flags = true;

        public Builder position(boolean preserve) {
            this.position = preserve;
            return this;
        }

        public Builder rotation(boolean preserve) {
            this.rotation = preserve;
            return this;
        }

        public Builder velocity(boolean preserve) {
            this.velocity = preserve;
            return this;
        }

        public Builder name(boolean preserve) {
            this.name = preserve;
            return this;
        }

        public Builder equipment(boolean preserve) {
            this.equipment = preserve;
            return this;
        }

        public Builder health(boolean preserve) {
            this.health = preserve;
            return this;
        }

        public Builder persistence(boolean preserve) {
            this.persistence = preserve;
            return this;
        }

        public Builder passengers(boolean preserve) {
            this.passengers = preserve;
            return this;
        }

        public Builder leash(boolean preserve) {
            this.leash = preserve;
            return this;
        }

        public Builder flags(boolean preserve) {
            this.flags = preserve;
            return this;
        }

        public ReplaceOptions build() {
            return new ReplaceOptions(this);
        }
    }
}
