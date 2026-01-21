package model.bodies.core;

import events.domain.ports.BodyToEmitDTO;
import model.bodies.ports.BodyEventProcessor;
import model.bodies.ports.BodyType;
import model.bodies.ports.PhysicsBody;
import model.emitter.implementations.BasicEmitter;
import model.emitter.ports.EmitterConfigDto;
import model.physics.ports.PhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;
import model.spatial.core.SpatialGrid;

public class AbstractPhysicsBody extends AbstractBody implements PhysicsBody {

    private Thread thread;
    private BasicEmitter emitter;

    public AbstractPhysicsBody(
            BodyEventProcessor bodyEventProcessor, SpatialGrid spatialGrid,
            PhysicsEngine phyEngine, BodyType bodyType, double maxLifeInSeconds) {

        super(bodyEventProcessor, spatialGrid, phyEngine, bodyType, maxLifeInSeconds);
    }

    public void decEmitterCooldown(double dtSeconds) {
        this.emitter.decCooldown(dtSeconds);
    }

    @Override
    public void doMovement(PhysicsValuesDTO phyValues) {
        PhysicsEngine engine = this.getPhysicsEngine();
        engine.setPhysicsValues(phyValues);
    }

    public BodyToEmitDTO getBodyToEmitConfig() {
        if (this.emitter == null) {
            return null;
        }
        return this.emitter.getBodyToEmitConfig();
    }

    public EmitterConfigDto getEmitterConfig() {
        if (this.emitter == null) {
            return null;
        }
        return this.emitter.getConfig();
    }

    @Override
    public PhysicsValuesDTO getPhysicsValues() {
        return this.getPhysicsEngine().getPhysicsValues();
    }

    @Override
    public boolean isThrusting() {
        return this.getPhysicsEngine().isThrusting();
    }

    public boolean mustEmitNow(double dtSeconds) {
        if (this.emitter == null) {
            return false;
        }

        // double dtNanos = newPhyValues.timeStamp - this.getPhysicsValues().timeStamp;
        // double dtSeconds = dtNanos / 1_000_000_000;

        return this.emitter.mustEmitNow(dtSeconds);
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

    public void registerBodyEmissionRequest() {
        if (this.emitter == null) {
            return; // No emitter attached ===========>
        }

        this.emitter.registerRequest();
    }

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
