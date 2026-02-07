package engine.utils.threading;

import engine.model.bodies.core.AbstractBody;
import engine.model.bodies.impl.DynamicBody;
import engine.model.bodies.ports.BodyState;
import engine.model.physics.ports.PhysicsValuesDTO;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MultiBodyRunner
 * ---------------
 * 
 * Executes multiple bodies sequentially in a single thread to reduce thread pressure.
 * 
 * This class groups N bodies together and processes them in sequence within one thread,
 * reducing the total number of active threads from (bodies) to (bodies / N).
 * 
 * Key Features:
 * - Processes N bodies per thread (configurable batch size)
 * - Each body executes its physics step sequentially
 * - Bodies are removed automatically when they die
 * - Thread terminates when all bodies are dead
 * - Thread-safe body removal during iteration
 * 
 * Design Goals:
 * - Reduce thread count from 3000-5000 to 375-625 (with N=8)
 * - Reduce stack memory from 3-5 GB to 375-625 MB
 * - Reduce OS scheduler pressure and context switching
 * - Reduce GC pause frequency by better memory locality
 * - Maintain physics simulation quality
 * 
 * Threading Model:
 * - One thread runs this Runnable
 * - Bodies execute sequentially in run() loop
 * - Each body gets its physics step every 15ms cycle
 * - Dead bodies are pruned automatically
 * 
 * Usage:
 * ```java
 * MultiBodyRunner runner = new MultiBodyRunner(8); // batch size
 * runner.addBody(body1);
 * runner.addBody(body2);
 * // ... up to N bodies
 * threadPoolManager.submit(runner);
 * ```
 * 
 * Performance Characteristics:
 * - With N=8: 8x fewer threads, ~8x less stack memory
 * - Latency: Each body waits for (N-1) other bodies before next cycle
 * - Throughput: Same total physics steps per second
 * - Cache locality: Better (sequential access pattern)
 * 
 * @see ThreadPoolManager
 * @see AbstractBody
 */
public class MultiBodyRunner implements Runnable {

    // region Constants
    private static final int SLEEP_TIME_MS = 15;
    // endregion

    // region Fields
    private final List<AbstractBody> bodies;
    private final int maxBodiesPerRunner;
    private volatile boolean shouldStop = false;
    // endregion

    // region Constructors
    /**
     * Create a MultiBodyRunner with specified batch size.
     * 
     * @param maxBodiesPerRunner maximum number of bodies to process in this runner
     */
    public MultiBodyRunner(int maxBodiesPerRunner) {
        if (maxBodiesPerRunner <= 0) {
            throw new IllegalArgumentException("maxBodiesPerRunner must be > 0, got: " + maxBodiesPerRunner);
        }
        
        this.maxBodiesPerRunner = maxBodiesPerRunner;
        // Use CopyOnWriteArrayList for thread-safe removal during iteration
        this.bodies = new CopyOnWriteArrayList<>();
    }
    // endregion

    // region Public methods
    /**
     * Add a body to this runner's batch.
     * 
     * @param body the body to add
     * @return true if added successfully, false if batch is full
     */
    public synchronized boolean addBody(AbstractBody body) {
        if (body == null) {
            throw new IllegalArgumentException("Body cannot be null");
        }
        
        if (this.bodies.size() >= this.maxBodiesPerRunner) {
            return false; // Batch is full
        }
        
        this.bodies.add(body);
        return true;
    }

    /**
     * Get current number of bodies in this runner.
     * 
     * @return number of bodies currently assigned
     */
    public int getBodiesCount() {
        return this.bodies.size();
    }

    /**
     * Check if this runner is at capacity.
     * 
     * @return true if no more bodies can be added
     */
    public boolean isFull() {
        return this.bodies.size() >= this.maxBodiesPerRunner;
    }

    /**
     * Request this runner to stop at next cycle.
     * Thread will terminate after current iteration completes.
     */
    public void requestStop() {
        this.shouldStop = true;
    }
    // endregion

    // region Runnable implementation
    /**
     * Main execution loop: processes all bodies sequentially until all are dead.
     * 
     * Loop structure:
     * 1. Iterate through all bodies
     * 2. For each ALIVE body:
     *    - Calculate new physics values
     *    - Update spatial grid
     *    - Process emitters (if applicable)
     *    - Process events (collisions, limits, etc.)
     * 3. Remove DEAD bodies
     * 4. Sleep 15ms
     * 5. Repeat until all bodies dead or stop requested
     */
    @Override
    public void run() {
        while (!this.shouldStop && hasAliveBodies()) {
            
            // Process each body sequentially
            for (AbstractBody body : this.bodies) {
                
                // Skip dead bodies (will be removed after iteration)
                if (body.getBodyState() == BodyState.DEAD) {
                    continue;
                }
                
                if (body.getBodyState() == BodyState.ALIVE) {
                    try {
                        // Execute single physics step for this body
                        executeBodyStep(body);
                        
                    } catch (Exception ex) {
                        System.err.println("ERROR in MultiBodyRunner processing body " 
                            + body.getBodyId() + ": " + ex.getMessage());
                        ex.printStackTrace();
                        // Don't kill entire batch if one body fails
                    }
                }
            }
            
            // Remove dead bodies after iteration (thread-safe with CopyOnWriteArrayList)
            this.bodies.removeIf(body -> body.getBodyState() == BodyState.DEAD);
            
            // Sleep to maintain ~60 Hz physics rate
            try {
                Thread.sleep(SLEEP_TIME_MS);
            } catch (InterruptedException ex) {
                // Exit gracefully on interrupt
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    // endregion

    // region Private methods
    /**
     * Execute one physics step for a body.
     * This mirrors the logic in DynamicBody.run() but for a single iteration.
     * 
     * @param body the body to process
     */
    private void executeBodyStep(AbstractBody body) {
        // Calculate new physics values
        PhysicsValuesDTO newPhyValues = body.getPhysicsEngine().calcNewPhysicsValues();
        
        // Update spatial grid
        if (body.getSpatialGrid() != null) {
            double r = newPhyValues.size * 0.5;
            body.getSpatialGrid().upsert(
                body.getBodyId(),
                newPhyValues.posX - r, newPhyValues.posX + r,
                newPhyValues.posY - r, newPhyValues.posY + r,
                body.getScratchIdxs()
            );
        }
        
        // Handle DynamicBody-specific trail emitters
        if (body instanceof DynamicBody) {
            DynamicBody dynBody = (DynamicBody) body;
            String trailId = dynBody.trailGetId();
            
            if (body.isThrusting() && trailId != null) {
                dynBody.emitterRequest(trailId);
            }
        }
        
        // Process events (collisions, rebounds, lifetime, etc.)
        body.processBodyEvents(body, newPhyValues, body.getPhysicsEngine().getPhysicsValues());
    }

    /**
     * Check if any bodies in this runner are still alive.
     * 
     * @return true if at least one body is not DEAD
     */
    private boolean hasAliveBodies() {
        for (AbstractBody body : this.bodies) {
            if (body.getBodyState() != BodyState.DEAD) {
                return true;
            }
        }
        return false;
    }
    // endregion
}
