package engine.model.bodies.impl;

import engine.model.bodies.core.AbstractBody;
import engine.model.bodies.ports.BodyEventProcessor;
import engine.model.bodies.ports.BodyState;
import engine.model.bodies.ports.BodyType;
import engine.model.physics.ports.PhysicsEngine;
import engine.model.physics.ports.PhysicsValuesDTO;
import engine.utils.spatial.core.SpatialGrid;
import engine.utils.threading.ThreadPoolManager;

/**
 * StaticBody 
 * ----------
 *
 * Represents a single static entity in the simulation model.
 *
 * Each StaticBody maintains:
 * - A unique identifier and visual attributes (assetId, size)
 * - A NullPhysicsEngine instance with fixed position and angle
 * - No dedicated thread (static bodies do not move or update)
 *
 * Static bodies are used for non-moving world elements such as obstacles,
 * platforms, or decorative elements that have physical presence but no
 * dynamic behavior.
 *
 * The view layer accesses static bodies through EntityInfoDTO snapshots,
 * following the same pattern as dynamic bodies but without the time-varying
 * physics data.
 *
 * Lifecycle control (STARTING → ALIVE → DEAD) is managed internally, and static
 * counters (inherited from AbstractEntity) track global quantities of created,
 * active and dead entities.
 *
 * Static vs. Dynamic
 * ------------------
 * Unlike DynamicBody, StaticBody:
 * - Uses NullPhysicsEngine (no physics updates)
 * - Has no thread (no run() loop)
 * - Returns EntityInfoDTO instead of DBodyInfoDTO (no velocity/acceleration)
 * - Is intended for fixed-position world elements
 *
 * This separation keeps the codebase clean and prevents unnecessary overhead
 * for entities that never move.
 */
public class StaticBody extends AbstractBody implements Runnable {

    //
    // CONSTRUCTORS
    //

    public StaticBody(
            BodyEventProcessor bodyEventProcessor, SpatialGrid spatialGrid,
            PhysicsEngine phyEngine, BodyType bodyType,
            double maxLifeInSeconds, String emitterId, ThreadPoolManager threadPoolManager) {

        super(
                bodyEventProcessor, spatialGrid,
                phyEngine,
                bodyType,
                maxLifeInSeconds, emitterId, threadPoolManager);
    }

    //
    // PUBLICS
    //

    @Override
    public synchronized void activate() {
        super.activate();

        this.setState(BodyState.ALIVE);
        
        // Use batched execution for static bodies
        // Static bodies have minimal overhead (mostly lifetime checks)
        this.getThreadPoolManager().submitBatched(this);    
    }

    // region AbstractBody
    @Override
    public void onTick() {
        if (this.isLifeOver()) {
            PhysicsValuesDTO phyValues = this.getPhysicsValues();
            this.processBodyEvents(this, phyValues, phyValues);
        }
    }
    // endregionexit
    

    // region Runnable
    @Override
    public void run() {
        while (this.getBodyState() != BodyState.DEAD) {
            if (this.getBodyState() == BodyState.ALIVE) {
                onTick();
            }

            try {
                Thread.sleep(30);
            } catch (InterruptedException ex) {
                System.err.println("ERROR Sleeping in StaticBody thread! (StaticBody) · " + ex.getMessage());
            }
        }
    }
    // endregion
}
