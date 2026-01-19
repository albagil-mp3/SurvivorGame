package model.bodies.core;

import model.bodies.ports.BodyEventProcessor;
import model.bodies.ports.BodyType;
import model.bodies.ports.PhysicsBody;
import model.emitter.implementations.BasicEmitter;
import model.emitter.ports.BodyEmittedDTO;
import model.emitter.ports.Emitter;
import model.physics.ports.PhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;
import model.spatial.core.SpatialGrid;

public class AbstractPhysicsBody extends AbstractBody implements PhysicsBody, Emitter {

    private Thread thread;
    private BasicEmitter emitter;

    public AbstractPhysicsBody(
            BodyEventProcessor bodyEventProcessor, SpatialGrid spatialGrid,
            PhysicsEngine phyEngine, BodyType bodyType, double maxLifeInSeconds) {

        super(bodyEventProcessor, spatialGrid, phyEngine, bodyType, maxLifeInSeconds);
    }

    @Override
    public void doMovement(PhysicsValuesDTO phyValues) {
        PhysicsEngine engine = this.getPhysicsEngine();
        engine.setPhysicsValues(phyValues);
    }

    @Override
    public BodyEmittedDTO getBodyEmittedConfig() {
        if (this.emitter == null) {
            return null;
        }
        return this.emitter.getBodyConfig();
    }

    @Override
    public BasicEmitter getEmitter() {
        return this.emitter;
    }

    @Override
    public PhysicsValuesDTO getPhysicsValues() {
        return this.getPhysicsEngine().getPhysicsValues();
    }

    @Override
    public boolean isThrusting() {
        return this.getPhysicsEngine().isThrusting();
    }

    @Override
    public boolean mustEmitNow(PhysicsValuesDTO newPhyValues) {
        if (this.getEmitter() == null) {
            return false;
        }

        double dtNanos = newPhyValues.timeStamp - this.getPhysicsValues().timeStamp;
        double dtSeconds = dtNanos / 1_000_000_000;

        return this.getEmitter().mustEmitNow(dtSeconds);
    }

    @Override
    public void reboundInEast(PhysicsValuesDTO newVals, PhysicsValuesDTO oldVals,
            double worldWidth, double worldHeight) {

        PhysicsEngine engine = this.getPhysicsEngine();
        engine.reboundInEast(newVals, oldVals, worldWidth, worldHeight);
    }

    @Override
    public void reboundInWest(PhysicsValuesDTO newVals, PhysicsValuesDTO oldVals,
            double worldWidth, double worldHeight) {

        PhysicsEngine engine = this.getPhysicsEngine();
        engine.reboundInWest(newVals, oldVals, worldWidth, worldHeight);
    }

    @Override
    public void reboundInNorth(PhysicsValuesDTO newVals, PhysicsValuesDTO oldVals,
            double worldWidth, double worldHeight) {

        PhysicsEngine engine = this.getPhysicsEngine();
        engine.reboundInNorth(newVals, oldVals, worldWidth, worldHeight);
    }

    @Override
    public void reboundInSouth(PhysicsValuesDTO newVals, PhysicsValuesDTO oldVals,
            double worldWidth, double worldHeight) {
        PhysicsEngine engine = this.getPhysicsEngine();
        engine.reboundInSouth(newVals, oldVals, worldWidth, worldHeight);
    }


    @Override
    public void registerEmmitRequest() {
        if (this.emitter == null) {
            return; // No emitter attached ===========>
        }

        this.emitter.registerRequest();
    }

    @Override
    public void setEmitter(BasicEmitter emitter) {
        if (emitter == null) {
            throw new IllegalStateException("Emitter is null. Cannot add to player body.");
        }
        this.emitter = emitter;
    }

    @Override
    public void setThread(Thread thread) {
        this.thread = thread;
    }
}