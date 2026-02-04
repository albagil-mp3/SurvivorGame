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
import engine.utils.spatial.core.SpatialGrid;

public class PlayerBody extends DynamicBody {

    // region Fields
    private final List<String> weaponIds = new java.util.ArrayList<>(4);
    private int currentWeaponIndex = -1; // -1 = sin arma
    private double damage = 0D;
    private double energye = 1D;
    private int temperature = 1;
    private double shield = 1D;
    private int score = 0;
    // endregion

    public PlayerBody(BodyEventProcessor bodyEventProcessor,
            SpatialGrid spatialGrid,
            PhysicsEngine physicsEngine,
            double maxLifeInSeconds,
            String emitterId) {

        super(bodyEventProcessor,
                spatialGrid,
                physicsEngine,
                BodyType.PLAYER,
                maxLifeInSeconds,
                emitterId);

        this.setMaxThrustForce(800);
        this.setMaxAngularAcceleration(1000);
        this.setAngularSpeed(30);
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

    public int getAmmoStatusPrimary() {
        return getAmmoStatus(0);
    }

    public int getAmmoStatusSecondary() {
        return getAmmoStatus(1);
    }

    public int getAmmoStatusMines() {
        return getAmmoStatus(2);
    }

    public int getAmmoStatusMissiles() {
        return getAmmoStatus(3);
    }

    private int getAmmoStatus(int weaponIndex) {
        if (weaponIndex < 0 || weaponIndex >= this.weaponIds.size()) {
            return 0;
        }

        BasicEmitter emitter = this.getEmitter(this.weaponIds.get(weaponIndex));
        if (emitter == null) {
            return 0;
        }

        return emitter.getBodiesRemaining();
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