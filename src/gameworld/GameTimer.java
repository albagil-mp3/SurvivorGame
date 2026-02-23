package gameworld;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class GameTimer {
    private static final GameTimer INSTANCE = new GameTimer();

    public static GameTimer get() { return INSTANCE; }

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "GameTimer");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> future = null;
    private long endTimeMs = 0L;
    private volatile boolean running = false;

    private GameTimer() {}

    public synchronized void start(long durationMs, Runnable onFinish) {
        stop();
        if (durationMs <= 0) return;
        this.endTimeMs = System.currentTimeMillis() + durationMs;
        this.running = true;
        this.future = scheduler.schedule(() -> {
            this.running = false;
            try {
                onFinish.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, durationMs, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (this.future != null) {
            this.future.cancel(false);
            this.future = null;
        }
        this.running = false;
        this.endTimeMs = 0L;
    }

    public boolean isRunning() {
        return this.running;
    }

    public long getRemainingMillis() {
        if (!this.running) return 0L;
        long rem = this.endTimeMs - System.currentTimeMillis();
        return Math.max(0L, rem);
    }

    public int getRemainingSeconds() {
        return (int) (getRemainingMillis() / 1000L);
    }

    public String getRemainingFormatted() {
        int secs = getRemainingSeconds();
        int minutes = secs / 60;
        int seconds = secs % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
