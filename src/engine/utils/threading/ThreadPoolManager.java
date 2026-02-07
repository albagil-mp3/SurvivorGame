package engine.utils.threading;

import engine.model.bodies.core.AbstractBody;
import engine.model.impl.MultiBodyRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * Minimal thread pool manager for body tasks.
 * Uses a synchronous queue and grows on demand.
 */
public final class ThreadPoolManager {

    // region Constants
    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final int DEFAULT_POOL_SIZE = 250;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;
    // endregion

    // region Fields
    private final ThreadPoolExecutor executor;
    private volatile boolean isShutdown = false;
    private final List<MultiBodyRunner> activeRunners = new ArrayList<>();
    private final Object runnersLock = new Object();
    // endregion

    // region Constructors
    public ThreadPoolManager() {
        this(DEFAULT_POOL_SIZE);
    }

    /**
     * Create a ThreadPoolManager with specified initial pool size.
     * 
     * The pool will start with 'initialPoolSize' core threads that are never
     * removed.
     * Uses a synchronous queue so tasks are handed off directly to threads:
     * - No buffering (queue size stays at 0)
     * - When all threads are busy, new threads are created up to Integer.MAX_VALUE
     * - This guarantees that every task gets executed on a thread
     * 
     * @param initialPoolSize number of core threads to create (must be > 0)
     */
    public ThreadPoolManager(int initialPoolSize) {
        if (initialPoolSize <= 0) {
            throw new IllegalArgumentException("initialPoolSize must be > 0, got: " + initialPoolSize);
        }

        // SynchronousQueue: no buffering, direct handoff
        BlockingQueue<Runnable> queue = new java.util.concurrent.SynchronousQueue<>();

        RejectedExecutionHandler handler = (r, executor) -> {
            throw new RejectedExecutionException("Task rejected - pool is shutdown or saturated");
        };

        this.executor = new ThreadPoolExecutor(
                initialPoolSize, // corePoolSize
                Integer.MAX_VALUE, // maximumPoolSize (unlimited growth)
                60, TimeUnit.SECONDS, // keepAliveTime (for non-core threads)
                queue, // SYNCHRONOUS queue (no buffering)
                r -> {
                    Thread t = new Thread(r);
                    t.setName("BodyThread-" + System.nanoTime());
                    t.setPriority(Thread.NORM_PRIORITY - 1);
                    t.setDaemon(false);

                    t.setUncaughtExceptionHandler((thread, throwable) -> {
                        throwable.printStackTrace();
                    });

                    return t;
                },
                handler);

    }
    // endregion

    // *** PUBLICS ***

    public boolean isShutdown() {
        return this.isShutdown;
    }

    public void prestartAllCoreThreads() {
        this.executor.prestartAllCoreThreads();
    }

    public void removeRunner(MultiBodyRunner runner) {
        if (runner == null) {
            return;
        }

        synchronized (this.runnersLock) {
            this.activeRunners.remove(runner);
        }
    }

    public boolean shutdown() {
        if (this.isShutdown) {
            return true;
        }

        this.isShutdown = true;

        synchronized (this.runnersLock) {
            for (MultiBodyRunner runner : this.activeRunners) {
                runner.requestStop();
            }
        }

        this.executor.shutdown();

        try {
            boolean terminated = this.executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!terminated) {
                this.executor.shutdownNow();
            }
            return terminated;
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void submitBatched(AbstractBody body) {
        submitBatched(body, DEFAULT_BATCH_SIZE);
    }

    public void submitBatched(AbstractBody body, int batchSize) {
        if (body == null) {
            throw new NullPointerException("Body cannot be null");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be > 0, got: " + batchSize);
        }

        synchronized (this.runnersLock) {
            for (MultiBodyRunner runner : this.activeRunners) {
                if (runner.getBatchSize() == batchSize
                        && !runner.isTerminated()
                        && !runner.isFull()
                        && runner.addBody(body)) {
                    return;
                }
            }

            MultiBodyRunner newRunner = new MultiBodyRunner(batchSize, this);
            boolean added = newRunner.addBody(body);

            if (!added) {
                // Should never happen (new runner should always have space)
                throw new IllegalStateException("Failed to add body to new runner");
            }

            this.activeRunners.add(newRunner);

            submit(newRunner);
        }
    }

    // *** PRIVATE ***

    private void submit(Runnable task) {
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }

        try {
            this.executor.submit(task);
        } catch (RejectedExecutionException e) {
            throw e;
        }
    }

}
