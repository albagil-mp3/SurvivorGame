package engine.model.bodies.impl;

import java.util.List;

import engine.events.domain.ports.BodyToEmitDTO;
import engine.model.bodies.ports.BodyEventProcessor;
import engine.model.bodies.ports.BodyType;
import engine.model.bodies.ports.PlayerDTO;
import engine.model.emitter.impl.BasicEmitter;
import engine.model.emitter.ports.EmitterConfigDto;
import engine.model.physics.ports.PhysicsEngine;
import engine.model.physics.ports.PhysicsValuesDTO;
import engine.utils.profiling.impl.BodyProfiler;
import engine.utils.spatial.core.SpatialGrid;

public class PlayerBody extends DynamicBody {

    private static final boolean PLAYERS_EXCLUSIVE = true;

    // region Fields
    private final List<String> weaponIds = new java.util.ArrayList<>(4);
    private int currentWeaponIndex = -1; // -1 = sin arma
    private double damage = 0D;
    private double energye = 1D;
    private int temperature = 1;
    private double shield = 1D;
    private int score = 0;
    
    // Directional movement state
    private boolean movingUp = false;
    private boolean movingDown = false;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    // endregion

    public PlayerBody(BodyEventProcessor bodyEventProcessor,
            SpatialGrid spatialGrid,
            PhysicsEngine physicsEngine,
            double maxLifeInSeconds,
            String emitterId,
            BodyProfiler profiler) {

        super(bodyEventProcessor,
                spatialGrid,
                physicsEngine,
                BodyType.PLAYER,
                maxLifeInSeconds,
                emitterId,
                profiler);

        this.setMaxThrustForce(1000); // Increased for more responsive movement
        this.setMaxAngularAcceleration(1000);
        this.setAngularSpeed(30);
    }

    @Override
    public synchronized void activate() {
        super.activate(); // Calls AbstractBody.activate()

        this.setState(engine.model.bodies.ports.BodyState.ALIVE);
        // Threading is now handled by Model/BodyBatchManager
        // Players will be assigned to batch size 1 (exclusive) by Model
    }

    public void addWeapon(String emitterId) {
        this.weaponIds.add(emitterId);

        if (this.currentWeaponIndex < 0) {
            // Signaling existence of weapon in the spaceship
            this.currentWeaponIndex = 0;
        }
    }

    // region Getters (get***)
    public BasicEmitter getActiveWeapon() {
        if (this.currentWeaponIndex < 0 || this.currentWeaponIndex >= this.weaponIds.size()) {
            return null;
        }

        // return this.weapons.get(this.currentWeaponIndex);
        return this.getEmitter(this.weaponIds.get(this.currentWeaponIndex));

    }

    public int getActiveWeaponIndex() {
        if (this.currentWeaponIndex < 0 || this.currentWeaponIndex >= this.weaponIds.size()) {
            return -1;
        }

        return this.currentWeaponIndex;
    }

    public EmitterConfigDto getActiveWeaponConfig() {
        BasicEmitter emitter = getActiveWeapon();

        return (emitter != null) ? emitter.getConfig() : null;
    }

    public double getAmmoStatusPrimary() {
        return getAmmoStatus(0);
    }

    public double getAmmoStatusSecondary() {
        return getAmmoStatus(1);
    }

    public double getAmmoStatusMines() {
        return getAmmoStatus(2);
    }

    public double getAmmoStatusMissiles() {
        return getAmmoStatus(3);
    }

    private double getAmmoStatus(int weaponIndex) {
        if (weaponIndex < 0 || weaponIndex >= this.weaponIds.size()) {
            return 0.0d;
        }

        BasicEmitter emitter = this.getEmitter(this.weaponIds.get(weaponIndex));
        if (emitter == null) {
            return 0.0d;
        }

        if (emitter.getConfig().unlimitedBodies) {
            return 1.0d;
        }

        int maxBodies = emitter.getConfig().maxBodiesEmitted;
        if (maxBodies <= 0) {
            return 0.0d;
        }

        double ratio = emitter.getBodiesRemaining() / (double) maxBodies;
        return Math.max(0.0d, Math.min(1.0d, ratio));
    }

    public double getDamage() {
        return damage;
    }

    public PlayerDTO getData() {
        PlayerDTO playerData = new PlayerDTO(
                this.getBodyId(),
                "",
                this.damage,
                this.energye,
                this.shield,
                this.temperature,
                this.getActiveWeaponIndex(),
                this.getAmmoStatusPrimary(),
                this.getAmmoStatusSecondary(),
                this.getAmmoStatusMines(),
                this.getAmmoStatusMissiles(),
                this.score);
        return playerData;
    }

    public double getEnergy() {
        return energye;
    }

    public BodyToEmitDTO getProjectileConfig() {
        if (this.currentWeaponIndex < 0 || this.currentWeaponIndex >= this.weaponIds.size()) {
            return null;
        }

        BasicEmitter emitter = this.getEmitter(this.weaponIds.get(this.currentWeaponIndex));
        if (emitter == null) {
            return null;
        }

        return emitter.getBodyToEmitConfig();
    }

    public double getShield() {
        return shield;
    }

    public int getTemperature() {
        return this.temperature;
    }
    // endregion

    public void registerFireRequest() {
        if (this.currentWeaponIndex < 0 || this.currentWeaponIndex >= this.weaponIds.size()) {
            System.out.println("> No weapon active or no weapons!");
            return;
        }

        BasicEmitter emitter = this.getEmitter(this.weaponIds.get(this.currentWeaponIndex));
        if (emitter == null) {
            // There is no weapon in this slot
            return;
        }

        emitter.registerRequest();
    }

    public void reverseThrust() {
        this.thurstNow(-this.getMaxThrustForce());
    }

    public void rotateLeftOn() {
        PhysicsValuesDTO phyValues = this.getPhysicsValues();

        if (phyValues.angularSpeed == 0) {
            this.setAngularSpeed(-this.getAngularSpeed());
        }

        this.accelerationAngularInc(-this.getMaxAngularAcceleration());
    }

    public void rotateRightOn() {
        PhysicsValuesDTO phyValues = this.getPhysicsValues();
        if (phyValues.angularSpeed == 0) {
            this.setAngularSpeed(this.getAngularSpeed());
        }

        this.accelerationAngularInc(this.getMaxAngularAcceleration());
    }

    public void rotateOff() {
        this.setAngularAcceleration(0.0d);
        this.setAngularSpeed(0.0d);
    }

    // endregion

    // region Directional Movement (directional***)
    /**
     * Move directly upward (North) regardless of ship rotation.
     * For fluid directional movement.
     */
    public void moveUpOn() {
        this.movingUp = true;
        this.updateDirectionalMovement();
    }

    /**
     * Move directly downward (South) regardless of ship rotation.
     */
    public void moveDownOn() {
        this.movingDown = true;
        this.updateDirectionalMovement();
    }

    /**
     * Move directly leftward (West) regardless of ship rotation.
     */
    public void moveLeftOn() {
        this.movingLeft = true;
        this.updateDirectionalMovement();
    }

    /**
     * Move directly rightward (East) regardless of ship rotation.
     */
    public void moveRightOn() {
        this.movingRight = true;
        this.updateDirectionalMovement();
    }
    
    /**
     * Stop moving upward.
     */
    public void moveUpOff() {
        this.movingUp = false;
        this.updateDirectionalMovement();
    }
    
    /**
     * Stop moving downward.
     */
    public void moveDownOff() {
        this.movingDown = false;
        this.updateDirectionalMovement();
    }
    
    /**
     * Stop moving leftward.
     */
    public void moveLeftOff() {
        this.movingLeft = false;
        this.updateDirectionalMovement();
    }
    
    /**
     * Stop moving rightward.
     */
    public void moveRightOff() {
        this.movingRight = false;
        this.updateDirectionalMovement();
    }

    /**
     * Stop all directional movement.
     */
    public void moveOff() {
        this.movingUp = false;
        this.movingDown = false;
        this.movingLeft = false;
        this.movingRight = false;
        this.updateDirectionalMovement();
    }
    
    /**
     * Update directional movement based on active direction flags.
     * Supports diagonal movement by combining multiple directions.
     */
    private void updateDirectionalMovement() {
        double accX = 0;
        double accY = 0;
        double force = this.getMaxThrustForce();
        
        // Calculate net acceleration from all active directions
        if (this.movingUp) {
            accY -= force;
        }
        if (this.movingDown) {
            accY += force;
        }
        if (this.movingLeft) {
            accX -= force;
        }
        if (this.movingRight) {
            accX += force;
        }
        
        // Normalize diagonal movement to prevent faster diagonal speed
        if ((this.movingUp || this.movingDown) && (this.movingLeft || this.movingRight)) {
            double magnitude = Math.sqrt(accX * accX + accY * accY);
            if (magnitude > 0) {
                accX = (accX / magnitude) * force;
                accY = (accY / magnitude) * force;
            }
        }
        
        this.setDirectionalAcceleration(accX, accY);
    }

    /**
     * Set directional acceleration for fluid movement.
     * This bypasses the angle-based thrust system for direct movement control.
     */
    private void setDirectionalAcceleration(double accX, double accY) {
        this.getPhysicsEngine().setDirectAcceleration(accX, accY);
    }
    // endregion

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public void setEnergye(double energye) {
        this.energye = energye;
    }

    public void selectNextWeapon() {
        if (this.weaponIds.size() <= 0) {
            return;
        }

        this.currentWeaponIndex++;
        this.currentWeaponIndex = this.currentWeaponIndex % this.weaponIds.size();
    }

    public void selectWeapon(int weaponIndex) {
        if (weaponIndex >= 0 && weaponIndex < this.weaponIds.size()) {
            this.currentWeaponIndex = weaponIndex;
        }
    }

    public void setShield(double shield) {
        this.shield = shield;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public boolean mustFireNow(PhysicsValuesDTO newPhyValues) {
        if (this.currentWeaponIndex < 0 || this.currentWeaponIndex >= this.weaponIds.size()) {
            return false;
        }

        BasicEmitter emitter = this.getEmitter(this.weaponIds.get(this.currentWeaponIndex));
        if (emitter == null) {
            return false;
        }

        double dtNanos = newPhyValues.timeStamp - this.getPhysicsValues().timeStamp;
        double dtSeconds = ((double) dtNanos) / 1_000_000_0000.0d;

        return emitter.mustEmitNow(dtSeconds);
    }
}