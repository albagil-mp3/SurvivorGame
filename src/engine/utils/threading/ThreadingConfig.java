package engine.utils.threading;

/**
 * ThreadingConfig
 * ----------------
 * 
 * Configuration for threading and batching behavior in the engine.
 * 
 * This class centralizes threading configuration, particularly the batch size
 * used by MultiBodyRunner to group bodies into threads.
 * 
 * Key Configuration:
 * - BODIES_PER_THREAD: How many bodies execute in a single thread
 *   - Default: 8 (reduces 3000 bodies → 375 threads)
 *   - Lower values (4): More threads, less latency per body, higher memory
 *   - Higher values (16): Fewer threads, more latency per body, lower memory
 * 
 * - PLAYERS_EXCLUSIVE: Whether players get dedicated threads (N=1)
 *   - Default: true (players always responsive)
 *   - false: players share threads with other bodies (not recommended)
 * 
 * Performance Impact:
 * - With N=8 and 3000 bodies: 375 threads, ~375 MB stack memory
 * - With N=4 and 3000 bodies: 750 threads, ~750 MB stack memory
 * - With N=16 and 3000 bodies: ~188 threads, ~188 MB stack memory
 * 
 * Tuning Guidelines:
 * - Start with default (8)
 * - If input lag persists: increase to 16 (fewer threads)
 * - If FPS drops: decrease to 4 (less latency per body)
 * - Monitor with: ThreadPoolManager.getMetrics()
 * 
 * @see MultiBodyRunner
 * @see ThreadPoolManager
 */
public final class ThreadingConfig {

    // region Configuration constants
    /**
     * How many bodies execute in a single thread (batch size).
     * 
     * Default: 8
     * - Balances thread count vs per-body latency
     * - Reduces 3000 bodies from 3000 threads to 375 threads
     * - Reduces stack memory from ~3 GB to ~375 MB
     * 
     * Tuning:
     * - Increase if: Too many threads, GC pressure, input lag
     * - Decrease if: FPS drops, physics simulation quality drops
     */
    public static final int BODIES_PER_THREAD = 10;

    /**
     * Whether player bodies get exclusive threads (N=1).
     * 
     * Default: true
     * - Players are critical for user experience
     * - Dedicated thread ensures responsive input
     * - Minimal cost (typically 1-4 players max)
     * 
     * Set to false only for testing or if players are AI-controlled.
     */
    public static final boolean PLAYERS_EXCLUSIVE = true;
    // endregion

    // region Private constructor (utility class)
    private ThreadingConfig() {
        // Prevent instantiation
    }
    // endregion
}
