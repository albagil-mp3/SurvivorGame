package engine.model.impl;

import engine.model.bodies.core.AbstractBody;
import engine.model.bodies.ports.BodyState;
import engine.utils.threading.ThreadPoolManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Executes multiple bodies sequentially in a single thread.
 * Reduces thread count from (bodies) to (bodies/N) where N is batch size.
 * Runner persists when empty for reuse when new bodies are added.
 */
public class MultiBodyRunner implements Runnable {

    // region Constants
    private static final int SLEEP_TIME_MS = 15;
    // endregion

    // region Fields
    private final List<AbstractBody> bodies;
    private final int maxBodiesPerRunner;
    private volatile boolean shouldStop = false;
    private volatile boolean isTerminated = false;
    private volatile boolean isAcceptingBodies = true;
    private final ThreadPoolManager ownerManager;
    // endregion

    // region Constructors
    public MultiBodyRunner(int maxBodiesPerRunner, ThreadPoolManager ownerManager) {
        if (maxBodiesPerRunner <= 0) {
            throw new IllegalArgumentException("maxBodiesPerRunner must be > 0, got: " + maxBodiesPerRunner);
        }

        this.maxBodiesPerRunner = maxBodiesPerRunner;
        this.ownerManager = ownerManager;
        this.bodies = new CopyOnWriteArrayList<>();
    }
    // endregion

    // *** PUBLICS ***

    public synchronized boolean addBody(AbstractBody body) {
        if (body == null) {
            throw new IllegalArgumentException("Body cannot be null");
        }

        if (!this.isAcceptingBodies || this.isTerminated) {
            return false;
        }

        if (this.bodies.size() >= this.maxBodiesPerRunner) {
            return false;
        }

        this.bodies.add(body);
        return true;
    }

    public int getBatchSize() {
        return this.maxBodiesPerRunner;
    }

    // region boolean checkers (is***)
    public boolean isFull() {
        return this.bodies.size() >= this.maxBodiesPerRunner;
    }

    public boolean isTerminated() {
        return this.isTerminated;
    }
    // endregion

    public void requestStop() {
        this.shouldStop = true;
    }

    // *** INTERFACE IMPLEMENTATIONS ***

    // region Runnable
    @Override
    public void run() {
        while (!this.shouldStop) {
            for (AbstractBody body : this.bodies) {
                if (body.getBodyState() == BodyState.DEAD) {
                    continue;
                }

                if (body.getBodyState() == BodyState.ALIVE) {
                    try {
                        executeBodyStep(body);
                    } catch (Exception ex) {
                        System.err.println("ERROR in MultiBodyRunner processing body "
                                + body.getBodyId() + ": " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }

            this.bodies.removeIf(body -> body.getBodyState() == BodyState.DEAD);

            try {
                Thread.sleep(SLEEP_TIME_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        this.isAcceptingBodies = false;
        this.isTerminated = true;

        if (this.ownerManager != null) {
            this.ownerManager.removeRunner(this);
        }
    }
    // endregion

    // *** PRIVATE ***

    private void executeBodyStep(AbstractBody body) {
        body.onTick();
    }
}
