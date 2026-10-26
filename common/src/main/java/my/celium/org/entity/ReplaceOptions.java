package my.celium.org.entity;

/**
 * What {@code EntityReplacer} preserves when swapping entities.
 * {@link #DEFAULT} preserves everything it safely can.
 */
public final class ReplaceOptions {
    /** Preserves everything: what {@link #builder()} builds unchanged. */
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

    /**
     * Whether the replacement takes over its position.
     *
     * @return {@code true} to carry it over
     */
    public boolean preservePosition() {
        return position;
    }

    /**
     * Whether the replacement takes over its yaw and pitch (only together with its position).
     *
     * @return {@code true} to carry it over
     */
    public boolean preserveRotation() {
        return rotation;
    }

    /**
     * Whether the replacement takes over its movement.
     *
     * @return {@code true} to carry it over
     */
    public boolean preserveVelocity() {
        return velocity;
    }

    /**
     * Whether the replacement takes over its custom name.
     *
     * @return {@code true} to carry it over
     */
    public boolean preserveName() {
        return name;
    }

    /**
     * Whether the replacement takes over its armor and held items.
     *
     * @return {@code true} to carry it over
     */
    public boolean preserveEquipment() {
        return equipment;
    }

    /**
     * Whether the replacement takes over its health.
     *
     * @return {@code true} to carry it over
     */
    public boolean preserveHealth() {
        return health;
    }

    /**
     * Whether the replacement is kept from despawning.
     *
     * @return {@code true} to carry it over
     */
    public boolean preservePersistence() {
        return persistence;
    }

    /**
     * Whether the replacement takes over its passengers.
     *
     * @return {@code true} to carry it over
     */
    public boolean preservePassengers() {
        return passengers;
    }

    /**
     * Whether the replacement takes over its leash.
     *
     * @return {@code true} to carry it over
     */
    public boolean preserveLeash() {
        return leash;
    }

    /**
     * Permanent invulnerability, no-gravity, silence and the glow tag.
     *
     * @return {@code true} to carry them over
     */
    public boolean preserveFlags() {
        return flags;
    }

    /**
     * Starts a set of options with everything preserved.
     *
     * @return a new builder
     */
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

        /** Starts with everything preserved; {@link ReplaceOptions#builder()} does the same. */
        public Builder() {
        }

        /**
         * Sets whether the replacement takes over its position.
         *
         * @param preserve {@code true} to carry it over
         * @return this builder
         */
        public Builder position(boolean preserve) {
            this.position = preserve;
            return this;
        }

        /**
         * Sets whether the replacement takes over its yaw and pitch (only together with its position).
         *
         * @param preserve {@code true} to carry it over
         * @return this builder
         */
        public Builder rotation(boolean preserve) {
            this.rotation = preserve;
            return this;
        }

        /**
         * Sets whether the replacement takes over its movement.
         *
         * @param preserve {@code true} to carry it over
         * @return this builder
         */
        public Builder velocity(boolean preserve) {
            this.velocity = preserve;
            return this;
        }

        /**
         * Sets whether the replacement takes over its custom name.
         *
         * @param preserve {@code true} to carry it over
         * @return this builder
         */
        public Builder name(boolean preserve) {
            this.name = preserve;
            return this;
        }

        /**
         * Sets whether the replacement takes over its armor and held items.
         *
         * @param preserve {@code true} to carry it over
         * @return this builder
         */
        public Builder equipment(boolean preserve) {
            this.equipment = preserve;
            return this;
        }

        /**
         * Sets whether the replacement takes over its health.
         *
         * @param preserve {@code true} to carry it over
         * @return this builder
         */
        public Builder health(boolean preserve) {
            this.health = preserve;
            return this;
        }

        /**
         * Sets whether the replacement is kept from despawning.
         *
         * @param preserve {@code true} to carry it over
         * @return this builder
         */
        public Builder persistence(boolean preserve) {
            this.persistence = preserve;
            return this;
        }

        /**
         * Sets whether the replacement takes over its passengers.
         *
         * @param preserve {@code true} to carry it over
         * @return this builder
         */
        public Builder passengers(boolean preserve) {
            this.passengers = preserve;
            return this;
        }

        /**
         * Sets whether the replacement takes over its leash.
         *
         * @param preserve {@code true} to carry it over
         * @return this builder
         */
        public Builder leash(boolean preserve) {
            this.leash = preserve;
            return this;
        }

        /**
         * Sets whether the replacement takes over permanent invulnerability, no-gravity, silence and the glow tag.
         *
         * @param preserve {@code true} to carry them over
         * @return this builder
         */
        public Builder flags(boolean preserve) {
            this.flags = preserve;
            return this;
        }

        /**
         * Finishes the options.
         *
         * @return the options
         */
        public ReplaceOptions build() {
            return new ReplaceOptions(this);
        }
    }
}
