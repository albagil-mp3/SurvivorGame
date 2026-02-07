package engine.utils.threading;

import engine.model.bodies.core.AbstractBody;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ThreadPoolManager
 * -----------------
 *
 * Manages a dynamically-growing pool of threads for body physics computation tasks.
 * This is an instantiable class that should be created and managed by the Model.
 * 
 * Key Strategy:
 * - Core threads are prestarted to guarantee immediate availability
 * - Additional threads are created on-demand if all core threads are busy
 * - Pool grows dynmamically (no upper limit) to handle unexpected load spikes
 * - Core threads NEVER shrink (no reaper timeout)
 * - Extra threads are kept alive indefinitely to avoid shutdown churn
 * 
 * This ensures:
 * ✓ No task is ever rejected due to thread unavailability
 * ✓ Pool grows monotonically (never shrinks)
 * ✓ Predictable performance with prestarted core threads
 * 
 * Features:
 * - Configurable initial pool size (defaults to 250 threads)
 * - Flexible preallocation (prestart 1, N, or all threads as needed)
 * - On-demand thread creation for spikes beyond core capacity
 * - Graceful shutdown support with timeout
 * - Uncaught exception handling for better error diagnostics
 * - Monitoring metrics (active threads, queue size, task counts)
 * - Automatic shutdown hook registration for cleanup
 * 
 * Usage:
 * 1. Create instance: new ThreadPoolManager(maxBodies)
 * 2. Prestart initial threads: threadPoolManager.prestartAllCoreThreads()
 *    Or more selective: threadPoolManager.prestartCoreThread() or prestartN(count)
 * 3. Submit tasks: threadPoolManager.submit(runnableTask)
 *    Tasks will use existing threads or create new ones as needed
 * 4. Cleanup on shutdown: threadPoolManager.shutdown() or shutdownNow()
 */
public final class ThreadPoolManager {

    // region Instance fields
    private final ThreadPoolExecutor executor;
    private final AtomicLong submittedTasks = new AtomicLong(0);
    private final AtomicLong rejectedTasks = new AtomicLong(0);
    private volatile boolean isShutdown = false;
    private final int poolSize;
    
    // Batching support
    private final List<MultiBodyRunner> activeRunners = new ArrayList<>();
    private final Object runnersLock = new Object();

    private static final int DEFAULT_POOL_SIZE = 250;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;
    // endregion

    // region Constructors
    /**
     * Create a ThreadPoolManager with default pool size
     */
    public ThreadPoolManager() {
        this(DEFAULT_POOL_SIZE);
    }

    /**
     * Create a ThreadPoolManager with specified initial pool size.
     * 
     * The pool will start with 'initialPoolSize' core threads that are never removed.
     * Uses a bounded queue that forces thread creation when the queue fills:
     * - Queue capacity = 2 * initialPoolSize (buffer for pending tasks)
     * - When queue fills, new threads are created up to Integer.MAX_VALUE
     * - This guarantees that every task gets executed on a thread
     * 
     * @param initialPoolSize number of core threads to create (must be > 0)
     */
    public ThreadPoolManager(int initialPoolSize) {
        if (initialPoolSize <= 0) {
            throw new IllegalArgumentException("initialPoolSize must be > 0, got: " + initialPoolSize);
        }
        
        this.poolSize = initialPoolSize;
        
        // Use a synchronous queue to force immediate thread creation on demand
        // This ensures tasks are never queued (queue size stays at 0)
        BlockingQueue<Runnable> queue = new java.util.concurrent.SynchronousQueue<>();
        
        // Create a rejection handler for shutdown or max thread limits
        RejectedExecutionHandler handler = (r, executor) -> {
            System.err.println("[ThreadPoolManager] ERROR: Task rejected - pool is shutdown or saturated");
            throw new RejectedExecutionException("Task rejected - pool is shutdown or saturated");
        };
        
        // Create ThreadPoolExecutor with synchronous queue strategy:
        // - corePoolSize = initialPoolSize (never removed)
        // - maximumPoolSize = Integer.MAX_VALUE (unlimited growth)
        // - keepAliveTime = 60s (extra threads die after 60s of inactivity)
        // - queue = SYNCHRONOUS (forces new thread creation instead of queuing)
        // - handler = rejection handler (shutdown/saturation)
        this.executor = new ThreadPoolExecutor(
            initialPoolSize,                      // corePoolSize
            Integer.MAX_VALUE,                    // maximumPoolSize (unlimited growth)
            60, TimeUnit.SECONDS,                 // keepAliveTime (for non-core threads)
            queue,                                // SYNCHRONOUS queue (no buffering)
            r -> {
                Thread t = new Thread(r);
                t.setName("BodyThread-" + System.nanoTime());
                t.setPriority(Thread.NORM_PRIORITY - 1);
                t.setDaemon(false);
                
                t.setUncaughtExceptionHandler((thread, throwable) -> {
                    System.err.println("[ThreadPoolManager] Uncaught exception in thread " 
                        + thread.getName() + ": " + throwable.getMessage());
                    throwable.printStackTrace();
                });
                
                return t;
            },
            handler);  // Use bounded queue rejection handler
        
        System.out.println("[ThreadPoolManager] Created with " + initialPoolSize
            + " core threads, synchronous queue (grows unlimited on demand)");
    }
    // endregion

    /**
     * Submit a body's Runnable task to the thread pool.
     * 
     * Behavior:
     * - If a core thread is available, task executes immediately
     * - If all core threads are busy, task queues temporarily
     * - If queue grows (e.g., during spike), new threads are created automatically
     * - Created threads are kept alive indefinitely (pool never shrinks)
     *
     * @param task the Runnable body to execute
     * @throws RejectedExecutionException if the pool is shutdown and cannot accept tasks
     * @throws NullPointerException if task is null
     */
    public void submit(Runnable task) {
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }
        
        try {
            this.executor.submit(task);
            this.submittedTasks.incrementAndGet();
        } catch (RejectedExecutionException e) {
            this.rejectedTasks.incrementAndGet();
            System.err.println("[ThreadPoolManager] ❌ Task rejected - pool is shutdown or cannot accept tasks");
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Submit a body using batched execution (MultiBodyRunner).
     * 
     * This method groups bodies into MultiBodyRunners to reduce thread count.
     * Instead of creating one thread per body, N bodies share a single thread.
     * 
     * Behavior:
     * - Tries to add body to an existing runner with space
     * - If all runners are full, creates a new runner and submits it
     * - Each runner processes up to BODIES_PER_THREAD bodies sequentially
     * 
     * Benefits:
     * - Reduces thread count from N bodies to (N / BODIES_PER_THREAD) threads
     * - Reduces stack memory consumption by ~90% (with default N=8)
     * - Reduces OS scheduler pressure and context switching
     * - Improves cache locality and GC behavior
     * 
     * Trade-offs:
     * - Slight increase in per-body latency (waits for other bodies in batch)
     * - Bodies in same batch execute sequentially, not parallel
     * 
     * @param body the body to execute in batched mode
     */
    public void submitBatched(AbstractBody body) {
        submitBatched(body, ThreadingConfig.BODIES_PER_THREAD);
    }

    /**
     * Submit a body using batched execution with custom batch size.
     * 
     * This allows overriding the default BODIES_PER_THREAD configuration.
     * Useful for special cases like player bodies (batchSize=1 for exclusive thread).
     * 
     * @param body the body to execute in batched mode
     * @param batchSize number of bodies per thread (1 = exclusive thread)
     */
    public void submitBatched(AbstractBody body, int batchSize) {
        if (body == null) {
            throw new NullPointerException("Body cannot be null");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be > 0, got: " + batchSize);
        }

        synchronized (this.runnersLock) {
            // Try to add to existing runner with space
            for (MultiBodyRunner runner : this.activeRunners) {
                if (runner.getBodiesCount() < batchSize && runner.addBody(body)) {
                    // Successfully added to existing runner
                    return;
                }
            }

            // No existing runner has space - create new one
            MultiBodyRunner newRunner = new MultiBodyRunner(batchSize);
            boolean added = newRunner.addBody(body);
            
            if (!added) {
                // Should never happen (new runner should always have space)
                throw new IllegalStateException("Failed to add body to new runner");
            }

            this.activeRunners.add(newRunner);
            
            // Submit the new runner to executor
            submit(newRunner);
        }
    }

    /**
     * Get the number of active MultiBodyRunners.
     * 
     * This is useful for debugging and monitoring the batching efficiency.
     * 
     * @return number of runners currently managing bodies
     */
    public int getActiveRunnersCount() {
        synchronized (this.runnersLock) {
            return this.activeRunners.size();
        }
    }

    /**
     * Clean up finished runners (those with no alive bodies).
     * 
     * Call periodically to free memory from completed runners.
     * Note: Runners are automatically removed when their thread terminates,
     * but this method can force cleanup if needed.
     * 
     * @return number of runners removed
     */
    public int cleanupFinishedRunners() {
        synchronized (this.runnersLock) {
            int sizeBefore = this.activeRunners.size();
            this.activeRunners.removeIf(runner -> runner.getBodiesCount() == 0);
            int removed = sizeBefore - this.activeRunners.size();
            
            if (removed > 0) {
                System.out.println("[ThreadPoolManager] Cleaned up " + removed + " finished runners");
            }
            
            return removed;
        }
    }

    /**
     * Prestart one core thread to avoid startup jitter on first task.
     * 
     * @return true if a core thread was started, false if already prestarted
     */
    public boolean prestartCoreThread() {
        return this.executor.prestartCoreThread();
    }

    /**
     * Prestart N core threads to prepare for expected load.
     * Useful for gradual warmup or when you know approximate expected thread count.
     * 
     * @param count number of core threads to prestart (will be clamped to available core threads)
     * @return number of threads actually prestarted
     */
    public int prestartCoreThreads(int count) {
        if (count <= 0) {
            return 0;
        }
        
        int started = 0;
        for (int i = 0; i < count && i < this.executor.getCorePoolSize(); i++) {
            if (this.executor.prestartCoreThread()) {
                started++;
            }
        }
        
        if (started > 0) {
            System.out.println("[ThreadPoolManager] Prestarted " + started + " core threads");
        }
        return started;
    }

    /**
     * Prestart all core threads to eliminate startup jitter and ensure
     * immediate thread availability from the start.
     * 
     * Recommended to call after creating the ThreadPoolManager if you want
     * the pool to be ready immediately.
     */
    public void prestartAllCoreThreads() {
        int started = this.executor.prestartAllCoreThreads();
        System.out.println("[ThreadPoolManager] Prestarted " + started + "/" + this.executor.getCorePoolSize() + " core threads");
    }

    /**
     * Initiate graceful shutdown of the thread pool.
     * Waits for currently executing tasks to complete within timeout.
     * 
     * @return true if all tasks completed within timeout, false otherwise
     */
    public boolean shutdown() {
        return shutdown(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Initiate graceful shutdown with custom timeout.
     * 
     * @param timeout maximum wait time for tasks to complete
     * @param unit time unit for the timeout
     * @return true if all tasks completed within timeout, false otherwise
     */
    public boolean shutdown(long timeout, TimeUnit unit) {
        if (this.isShutdown) {
            System.out.println("[ThreadPoolManager] Already shutdown");
            return true;
        }
        
        this.isShutdown = true;
        this.executor.shutdown();
        
        try {
            boolean terminated = this.executor.awaitTermination(timeout, unit);
            if (terminated) {
                System.out.println("[ThreadPoolManager] Shutdown completed successfully");
                this.printStatistics();
            } else {
                System.err.println("[ThreadPoolManager] Shutdown timeout - forcing termination");
                this.executor.shutdownNow();
            }
            return terminated;
        } catch (InterruptedException e) {
            System.err.println("[ThreadPoolManager] Shutdown interrupted - forcing termination");
            this.executor.shutdownNow();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Force immediate shutdown, attempting to stop actively executing tasks.
     * 
     * @return list of tasks that never commenced execution
     */
    public java.util.List<Runnable> shutdownNow() {
        this.isShutdown = true;
        System.out.println("[ThreadPoolManager] Forcing immediate shutdown");
        java.util.List<Runnable> pendingTasks = this.executor.shutdownNow();
        System.out.println("[ThreadPoolManager] Shutdown now completed - " + pendingTasks.size() + " pending tasks cancelled");
        this.printStatistics();
        return pendingTasks;
    }

    /**
     * Get current queue size (for debugging/monitoring).
     *
     * @return approximate number of pending tasks in the thread pool queue
     */
    public int getQueueSize() {
        return this.executor.getQueue().size();
    }

    /**
     * Get number of currently active threads executing tasks.
     *
     * @return approximate number of active threads
     */
    public int getActiveThreadCount() {
        return this.executor.getActiveCount();
    }

    /**
     * Get the current number of threads in the pool (core + extra).
     * This can be higher than initialPoolSize if load spikes caused growth.
     * 
     * @return current thread count (grows with demand)
     */
    public int getCurrentThreadCount() {
        return this.executor.getPoolSize();
    }

    /**
     * Get the core pool size (initial thread count that never shrinks).
     * 
     * @return number of core threads that are permanently allocated
     */
    public int getCoreThreadCount() {
        return this.executor.getCorePoolSize();
    }

    /**
     * Get maximum possible thread count (currently unlimited).
     * 
     * @return Integer.MAX_VALUE (effectively unlimited growth)
     */
    public int getMaximumThreadCount() {
        return this.executor.getMaximumPoolSize();
    }

    /**
     * Get total number of tasks that have been submitted.
     *
     * @return total submitted task count
     */
    public long getSubmittedTaskCount() {
        return this.submittedTasks.get();
    }

    /**
     * Get total number of tasks that have been rejected.
     *
     * @return total rejected task count
     */
    public long getRejectedTaskCount() {
        return this.rejectedTasks.get();
    }

    /**
     * Get total number of tasks that have completed execution.
     *
     * @return approximate total of completed tasks
     */
    public long getCompletedTaskCount() {
        return this.executor.getCompletedTaskCount();
    }

    /**
     * Check if the thread pool is shutdown.
     *
     * @return true if shutdown has been initiated
     */
    public boolean isShutdown() {
        return this.isShutdown;
    }

    /**
     * Get the configured pool size.
     *
     * @return the maximum number of threads in the pool
     */
    public int getPoolSize() {
        return this.poolSize;
    }

    /**
     * Print comprehensive statistics about the thread pool.
     */
    public void printStatistics() {
        int currentThreads = this.executor.getPoolSize();
        int coreThreads = this.executor.getCorePoolSize();
        int extraThreads = currentThreads - coreThreads;
        int queueSize = this.executor.getQueue().size();
        int queueCapacity = this.executor.getQueue().remainingCapacity() + queueSize;
        int activeRunnersCount;
        
        synchronized (this.runnersLock) {
            activeRunnersCount = this.activeRunners.size();
        }
        
        System.out.println("\n╔═══════════════════════════════════════════════════════╗");
        System.out.println("║      ThreadPoolManager Statistics (Dynamic Pool)       ║");
        System.out.println("╠═══════════════════════════════════════════════════════╣");
        System.out.printf("║ Core Threads:           %6d (never reclaimed)      ║%n", coreThreads);
        System.out.printf("║ Extra Threads:          %6d (created on demand)   ║%n", extraThreads);
        System.out.printf("║ Total Threads:          %6d                        ║%n", currentThreads);
        System.out.printf("║ Active Threads:         %6d (currently working)   ║%n", this.executor.getActiveCount());
        System.out.printf("║ Active Runners:         %6d (batching)            ║%n", activeRunnersCount);
        System.out.printf("║ Queue Size:             %6d / %-6d (pending/capacity)  ║%n", queueSize, queueCapacity);
        System.out.printf("║ Submitted Tasks:        %6d                        ║%n", this.submittedTasks.get());
        System.out.printf("║ Completed Tasks:        %6d                        ║%n", this.executor.getCompletedTaskCount());
        System.out.printf("║ Rejected Tasks:         %6d                        ║%n", this.rejectedTasks.get());
        System.out.printf("║ Is Shutdown:            %6s                        ║%n", this.isShutdown);
        System.out.printf("║ Is Terminated:          %6s                        ║%n", this.executor.isTerminated());
        System.out.println("╚═══════════════════════════════════════════════════════╝\n");
    }
}
