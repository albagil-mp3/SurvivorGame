package engine.utils.threading;

/**
 * ThreadPoolDemo
 * --------------
 * 
 * Demonstration class showing the new capabilities of ThreadPoolManager.
 * Run this class to see the improved features in action.
 * 
 * This is NOT part of the production code - it's a demo/testing utility.
 */
public class ThreadPoolDemo {
    
    private static ThreadPoolManager threadPoolManager;
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║   ThreadPoolManager - Feature Demonstration         ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");
        
        // Demo 1: Configuration and Setup
        demo1_ConfigurationAndSetup();
        
        // Demo 2: Task Submission and Monitoring
        demo2_TaskSubmissionAndMonitoring();
        
        // Demo 3: Monitoring Features
        demo3_MonitoringFeatures();
        
        // Demo 4: Exception Handling
        demo4_ExceptionHandling();
        
        // Demo 5: Graceful Shutdown
        demo5_GracefulShutdown();
        
        System.out.println("\n✅ Demo completed successfully!");
    }
    
    private static void demo1_ConfigurationAndSetup() {
        System.out.println("━━━ Demo 1: Configuration and Setup ━━━\n");
        
        // Configure pool size
        int poolSize = 10;
        threadPoolManager = new ThreadPoolManager(poolSize);
        System.out.println("✓ Created ThreadPoolManager with " + poolSize + " threads");
        
        // Prestart threads
        threadPoolManager.prestartAllCoreThreads();
        System.out.println("✓ Prestarted all core threads");
        
        waitForUser();
    }
    
    private static void demo2_TaskSubmissionAndMonitoring() {
        System.out.println("\n━━━ Demo 2: Task Submission and Monitoring ━━━\n");
        
        // Submit some tasks
        System.out.println("Submitting 20 tasks...");
        for (int i = 0; i < 20; i++) {
            final int taskId = i;
            threadPoolManager.submit(() -> {
                try {
                    System.out.println("  Task " + taskId + " executing on " + Thread.currentThread().getName());
                    Thread.sleep(100);
                    System.out.println("  Task " + taskId + " completed");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        System.out.println("\n✓ Submitted 20 tasks");
        
        // Show stats during execution
        sleep(50);
        System.out.println("\n📊 Statistics during execution:");
        System.out.println("  Active Threads: " + threadPoolManager.getActiveThreadCount());
        System.out.println("  Queue Size: " + threadPoolManager.getQueueSize());
        System.out.println("  Submitted: " + threadPoolManager.getSubmittedTaskCount());
        
        // Wait for completion
        sleep(2500);
        System.out.println("\n✓ All tasks completed");
        
        waitForUser();
    }
    
    private static void demo3_MonitoringFeatures() {
        System.out.println("\n━━━ Demo 3: Monitoring Features ━━━\n");
        
        System.out.println("Starting ThreadPoolMonitor...");
        ThreadPoolMonitor monitor = new ThreadPoolMonitor(threadPoolManager);
        monitor.setQueueSizeWarningThreshold(5);
        monitor.setQueueSizeCriticalThreshold(15);
        monitor.start(2000); // Check every 2 seconds
        
        System.out.println("✓ Monitor started\n");
        
        // Simulate burst of tasks
        System.out.println("Simulating burst of 50 tasks...");
        for (int i = 0; i < 50; i++) {
            final int taskId = i;
            threadPoolManager.submit(() -> {
                try {
                    Thread.sleep(500); // Simulate work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // Let monitor check a few times
        System.out.println("(Monitor will report issues if queue grows too large)\n");
        sleep(7000);
        
        monitor.stop();
        System.out.println("\n✓ Monitor stopped");
        
        waitForUser();
    }
    
    private static void demo4_ExceptionHandling() {
        System.out.println("\n━━━ Demo 4: Exception Handling ━━━\n");
        
        System.out.println("Submitting task that will throw an exception...");
        threadPoolManager.submit(() -> {
            System.out.println("  Task starting, about to throw exception...");
            throw new RuntimeException("Demo exception - this is expected!");
        });
        
        sleep(500);
        System.out.println("\n✓ Exception was caught and logged by UncaughtExceptionHandler");
        System.out.println("  (Notice the error output above - the thread pool continues working)");
        
        // Verify pool still works
        System.out.println("\nSubmitting normal task to verify pool still works...");
        threadPoolManager.submit(() -> {
            System.out.println("  ✓ Normal task executed successfully after exception");
        });
        
        sleep(500);
        
        waitForUser();
    }
    
    private static void demo5_GracefulShutdown() {
        System.out.println("\n━━━ Demo 5: Graceful Shutdown ━━━\n");
        
        // Submit some long-running tasks
        System.out.println("Submitting 5 long-running tasks...");
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            threadPoolManager.submit(() -> {
                try {
                    System.out.println("  Long task " + taskId + " working...");
                    Thread.sleep(2000);
                    System.out.println("  Long task " + taskId + " completed");
                } catch (InterruptedException e) {
                    System.out.println("  Long task " + taskId + " interrupted");
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        sleep(500);
        
        // Show stats before shutdown
        System.out.println("\n📊 Statistics before shutdown:");
        threadPoolManager.printStatistics();
        
        // Initiate graceful shutdown
        System.out.println("\nInitiating graceful shutdown (10 second timeout)...");
        boolean success = threadPoolManager.shutdown(10, java.util.concurrent.TimeUnit.SECONDS);
        
        if (success) {
            System.out.println("\n✅ Graceful shutdown completed - all tasks finished");
        } else {
            System.out.println("\n⚠️ Shutdown timeout - some tasks were forced to stop");
        }
        
        // Show final stats
        System.out.println("\n📊 Final statistics:");
        System.out.println("  Total Submitted: " + threadPoolManager.getSubmittedTaskCount());
        System.out.println("  Total Completed: " + threadPoolManager.getCompletedTaskCount());
        System.out.println("  Total Rejected: " + threadPoolManager.getRejectedTaskCount());
    }
    
    // Helper methods
    
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static void waitForUser() {
        System.out.println("\n[Press Enter to continue to next demo...]");
        try {
            System.in.read();
            // Clear input buffer
            while (System.in.available() > 0) {
                System.in.read();
            }
        } catch (Exception e) {
            // Ignore
        }
    }
}
