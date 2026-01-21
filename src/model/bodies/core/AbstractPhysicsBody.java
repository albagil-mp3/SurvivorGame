package model.bodies.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import events.domain.ports.BodyToEmitDTO;
import model.bodies.ports.BodyEventProcessor;
import model.bodies.ports.BodyType;
import model.bodies.ports.PhysicsBody;
import model.emitter.implementations.BasicEmitter;
import model.emitter.ports.Emitter;
import model.emitter.ports.EmitterConfigDto;
import model.physics.ports.PhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;
import model.spatial.core.SpatialGrid;

public class AbstractPhysicsBody extends AbstractBody implements PhysicsBody {

    private Thread thread;
    private BasicEmitter emitter;

    // New emitter map based
    private final Map<String, Emitter> emitters = new ConcurrentHashMap<>();

    public AbstractPhysicsBody(
            BodyEventProcessor bodyEventProcessor, SpatialGrid spatialGrid,
            PhysicsEngine phyEngine, BodyType bodyType, double maxLifeInSeconds) {

        super(bodyEventProcessor, spatialGrid, phyEngine, bodyType, maxLifeInSeconds);
    }

    // why is public? and why is it here?
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

    //
    // NEW EMITTER MAP BASED METHODS
    //

    public void addEmitter(Emitter emitter) {
        if (emitter == null) {
            throw new IllegalArgumentException("Emitter cannot be null");
        }
        this.emitters.put(emitter.getId(), emitter);
    }

    public void removeEmitter(String emitterId) {
        this.emitters.remove(emitterId);
    }

    public Emitter getEmitter(String emitterId) {
        return this.emitters.get(emitterId);
    }

    public boolean hasEmitters() {
        return !this.emitters.isEmpty();
    }

    public int getEmitterCount() {
        return this.emitters.size();
    }

    public void requestEmit(String emitterId) {
        Emitter emitter = emitters.get(emitterId);
        if (emitter != null) {
            emitter.registerRequest();
        }
    }

    public List<Emitter> checkActiveEmitters(double dtSeconds) {
        List<Emitter> active = new ArrayList<>();

        for (Emitter emitter : emitters.values()) {
            if (emitter.mustEmitNow(dtSeconds)) {
                active.add(emitter);
            }
        }

        return active;
    }
}
