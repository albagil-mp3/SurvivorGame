package engine.utils.threading;

import java.util.Timer;
import java.util.TimerTask;

/**
 * ThreadPoolMonitor
 * ----------------
 * 
 * Optional monitoring utility for ThreadPoolManager instances that provides
 * periodic statistics logging and alerts for potential issues.
 * 
 * This is an optional component that can be used for debugging
 * and production monitoring.
 * 
 * Usage:
 * ```java
 * ThreadPoolManager poolManager = new ThreadPoolManager(maxBodies);
 * ThreadPoolMonitor monitor = new ThreadPoolMonitor(poolManager);
 * monitor.start(30000); // Monitor every 30 seconds
 * // ... run application ...
 * monitor.stop();
 * ```
 */
public class ThreadPoolMonitor {
    
    private final ThreadPoolManager threadPoolManager;
    private Timer monitorTimer;
    private volatile boolean isRunning = false;
    
    // Thresholds for alerts
    private int queueSizeWarningThreshold = 200;
    private int queueSizeCriticalThreshold = 500;
    private double poolUtilizationWarningThreshold = 0.85; // 85%
    
    /**
     * Create a monitor for the given ThreadPoolManager instance
     * 
     * @param threadPoolManager the pool manager to monitor
     */
    public ThreadPoolMonitor(ThreadPoolManager threadPoolManager) {
        if (threadPoolManager == null) {
            throw new IllegalArgumentException("ThreadPoolManager cannot be null");
        }
        this.threadPoolManager = threadPoolManager;
    }
    
    /**
     * Start periodic monitoring with default interval (60 seconds)
     */
    public void start() {
        start(60000);
    }
    
    /**
     * Start periodic monitoring with custom interval
     * 
     * @param intervalMillis monitoring interval in milliseconds
     */
    public void start(long intervalMillis) {
        if (isRunning) {
            System.out.println("[ThreadPoolMonitor] Already running");
            return;
        }
        
        isRunning = true;
        monitorTimer = new Timer("ThreadPoolMonitor", true);
        
        monitorTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkAndReport();
            }
        }, 0, intervalMillis);
        
        System.out.println("[ThreadPoolMonitor] Started monitoring (interval: " 
            + intervalMillis + "ms)");
    }
    
    /**
     * Stop monitoring
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        isRunning = false;
        if (monitorTimer != null) {
            monitorTimer.cancel();
            monitorTimer = null;
        }
        
        System.out.println("[ThreadPoolMonitor] Stopped monitoring");
    }
    
    /**
     * Check pool status and report issues
     */
    private void checkAndReport() {
        int queueSize = threadPoolManager.getQueueSize();
        int activeThreads = threadPoolManager.getActiveThreadCount();
        long submitted = threadPoolManager.getSubmittedTaskCount();
        long completed = threadPoolManager.getCompletedTaskCount();
        long rejected = threadPoolManager.getRejectedTaskCount();
        
        // Calculate pool utilization
        double utilization = 0.0;
        int poolSize = threadPoolManager.getPoolSize();
        if (poolSize > 0 && activeThreads > 0) {
            utilization = (double) activeThreads / poolSize;
        }
        
        // Check for issues
        boolean hasIssues = false;
        StringBuilder report = new StringBuilder();
        report.append("\n[ThreadPoolMonitor] Status Check\n");
        report.append("  Active Threads: ").append(activeThreads).append("\n");
        report.append("  Queue Size: ").append(queueSize).append("\n");
        report.append("  Submitted: ").append(submitted).append(" | Completed: ").append(completed).append("\n");
        
        // Queue size alerts
        if (queueSize >= queueSizeCriticalThreshold) {
            report.append("  🔴 CRITICAL: Queue size is very high (").append(queueSize).append(")\n");
            report.append("     → Pool may be saturated\n");
            report.append("     → Consider increasing pool size or investigating task duration\n");
            hasIssues = true;
        } else if (queueSize >= queueSizeWarningThreshold) {
            report.append("  ⚠️ WARNING: Queue size is growing (").append(queueSize).append(")\n");
            report.append("     → Monitor for continued growth\n");
            hasIssues = true;
        }
        
        // Rejected tasks alert
        if (rejected > 0) {
            report.append("  🔴 CRITICAL: Tasks are being rejected (").append(rejected).append(" total)\n");
            report.append("     → Pool may be shutdown or saturated\n");
            hasIssues = true;
        }
        
        // Pool utilization alert
        if (utilization >= poolUtilizationWarningThreshold) {
            report.append("  ⚠️ WARNING: High pool utilization (")
                .append(String.format("%.1f%%", utilization * 100)).append(")\n");
            report.append("     → Pool is working at near capacity\n");
            hasIssues = true;
        }
        
        // Pending tasks calculation
        long pending = submitted - completed;
        if (pending > 1000) {
            report.append("  ⚠️ WARNING: Many pending tasks (").append(pending).append(")\n");
            report.append("     → Tasks may not be completing (infinite loops?)\n");
            hasIssues = true;
        }
        
        // Print report only if there are issues or in verbose mode
        if (hasIssues) {
            System.out.println(report.toString());
        }
        
        // Idle state notification (optional, usually not needed)
        if (queueSize == 0 && activeThreads < 5 && submitted > 0) {
            // Pool is idle - this might be normal during certain game states
        }
    }
    
    /**
     * Set the queue size threshold for warnings
     * 
     * @param threshold queue size that triggers a warning
     */
    public void setQueueSizeWarningThreshold(int threshold) {
        this.queueSizeWarningThreshold = threshold;
    }
    
    /**
     * Set the queue size threshold for critical alerts
     * 
     * @param threshold queue size that triggers a critical alert
     */
    public void setQueueSizeCriticalThreshold(int threshold) {
        this.queueSizeCriticalThreshold = threshold;
    }
    
    /**
     * Set the pool utilization threshold for warnings (0.0 to 1.0)
     * 
     * @param threshold utilization percentage (e.g., 0.85 for 85%)
     */
    public void setPoolUtilizationWarningThreshold(double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
        }
        this.poolUtilizationWarningThreshold = threshold;
    }
    
    /**
     * Force an immediate check and report
     */
    public void checkNow() {
        checkAndReport();
    }
    
    /**
     * Print full statistics immediately
     */
    public void printStatistics() {
        threadPoolManager.printStatistics();
    }
    
    /**
     * Check if monitoring is currently running
     * 
     * @return true if monitoring is active
     */
    public boolean isRunning() {
        return isRunning;
    }
}
