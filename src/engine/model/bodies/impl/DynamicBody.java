package engine.model.bodies.impl;

import engine.model.bodies.core.AbstractBody;
import engine.model.bodies.ports.BodyEventProcessor;
import engine.model.bodies.ports.BodyState;
import engine.model.bodies.ports.BodyType;
import engine.model.emitter.impl.BasicEmitter;
import engine.model.physics.ports.PhysicsEngine;
import engine.model.physics.ports.PhysicsValuesDTO;
import engine.utils.spatial.core.SpatialGrid;

/**
 * DynamicBody
 * -----------
 *
 * Represents a single dynamic entity in the simulation model.
 *
 * Each DynamicBody maintains:
 * - A unique identifier and visual attributes (assetId, size)
 * - Its own PhysicsEngine instance, which stores and updates the immutable
 * PhysicsValues snapshot (position, speed, acceleration, angle, etc.)
 * - A dedicated thread responsible for advancing its physics state over time
 *
 * Dynamic bodies interact exclusively with the Model, reporting physics updates
 * and requesting event processing (collisions, rebounds, etc.). The view layer
 * never reads mutable state directly; instead, DynamicBody produces a
 * DBodyInfoDTO snapshot encapsulating all visual and physical data required
 * for rendering.
 *
 * Lifecycle control (STARTING → ALIVE → DEAD) is managed internally, and static
 * counters (inherited from AbstractEntity) track global quantities of created,
 * active and dead entities.
 *
 * Threading model
 * ---------------
 * Each DynamicBody runs on its own thread (implements Runnable). The physics
 * engine is updated continuously in the run() loop, with the entity checking
 * for events and processing actions based on game rules determined by the
 * Controller.
 *
 * The goal of this class is to isolate per-object behavior and physics
 * evolution
 * while keeping the simulation thread-safe through immutable snapshots and a
 * clearly separated rendering pipeline.
 */
public class DynamicBody extends AbstractBody implements Runnable {

    // region Constants
    private static final int SLEEP_TIME_MS = 15;
    // endregion

    // region Fields
    private double maxThrustForce; //
    private double maxAngularAcc; // degrees*s^-2
    private double angularSpeed; // degrees*s^-1
    private String trailId;
    // endregion


    // region Constructors
    public DynamicBody(BodyEventProcessor bodyEventProcessor, SpatialGrid spatialGrid,
            PhysicsEngine phyEngine, BodyType bodyType, double maxLifeInSeconds, String emitterId) {

        super(bodyEventProcessor, spatialGrid,
                phyEngine,
                bodyType,
                maxLifeInSeconds, 
                emitterId);
    }
    // endregion

    // *** PUBLICS ***

    @Override // AbstractBody
    public synchronized void activate() {
        super.activate();

        Thread thread = new Thread(this);
        thread.setName("Body " + this.getBodyId());
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        thread.start();
        this.setThread(thread);
        this.setState(BodyState.ALIVE);
    }

    // region Acceleration control (acceleration***)
    public void accelerationAngularInc(double angularAcceleration) {
        this.getPhysicsEngine().angularAccelerationInc(angularAcceleration);
    }

    public void accelerationReset() {
        this.getPhysicsEngine().resetAcceleration();
    }
    // endregion

    // region Trail management (trail***)
    public String trailEquip(BasicEmitter trailEmitter) {
        this.trailId = this.emitterEquip(trailEmitter);

        return this.trailId;
    }

    public String trailGetId() {
        return this.trailId;
    }

    // endregion

    // region Getters (get***)
    public double getAngularSpeed() {
        return this.angularSpeed;
    }

    public double getMaxThrustForce() {
        return this.maxThrustForce;
    }

    public double getMaxAngularAcceleration() {
        return this.maxAngularAcc;
    }
    // endregion

    // region setters (set***)
    public void setAngularAcceleration(double angularAcc) {
        this.getPhysicsEngine().setAngularAcceleration(angularAcc);
    }

    public void setAngularSpeed(double angularSpeed) {
        this.getPhysicsEngine().setAngularSpeed(angularSpeed);
    }

    public void setMaxThrustForce(double maxThrust) {
        this.maxThrustForce = maxThrust;
    }

    public void setMaxAngularAcceleration(double maxAngularAcc) {
        this.setAngularSpeed(this.angularSpeed);
        this.maxAngularAcc = maxAngularAcc;
    }
    // endregion

    // region Thrust control (thrust***)
    public void thrustMaxOn() {
        this.thurstNow(this.maxThrustForce);
    }

    public void thurstNow(double thrust) {
        this.getPhysicsEngine().setThrust(thrust);
    }

    public void thrustOff() {
        this.getPhysicsEngine().stopPushing();
    }
    // endregion

    // *** INTERFACE IMPLEMENTATIONS ***

    // regions Runnable
    @Override
    public void run() {
        PhysicsValuesDTO newPhyValues;

        while (this.getBodyState() != BodyState.DEAD) {

            if (this.getBodyState() == BodyState.ALIVE) {
                newPhyValues = this.getPhysicsEngine().calcNewPhysicsValues();

                double r = newPhyValues.size * 0.5;
                double minX = newPhyValues.posX - r;
                double maxX = newPhyValues.posX + r;
                double minY = newPhyValues.posY - r;
                double maxY = newPhyValues.posY + r;

                this.getSpatialGrid().upsert(
                        this.getBodyId(), minX, maxX, minY, maxY, this.getScratchIdxs());

                if (this.isThrusting()) {
                    if (this.trailId != null)
                        this.emitterRequest(this.trailId);
                }

                this.processBodyEvents(this, newPhyValues, this.getPhysicsEngine().getPhysicsValues());
            }

            try {
                Thread.sleep(SLEEP_TIME_MS);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    // endregion

}
