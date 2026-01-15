package model.trails.core;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import model.trails.ports.TrailDto;

public abstract class AbstractTrailEmitter {

    private final String id;
    private final TrailDto config;
    private final AtomicLong lastRequest = new AtomicLong(0L);
    private AtomicLong lastHandledRequest = new AtomicLong(0L);
    private volatile double cooldown = 0.0; // seconds

    public AbstractTrailEmitter(TrailDto config) {

        if (config.emisionRate <= 0) {
            throw new IllegalArgumentException(
                    "emisionRate must be > 0. Trail not created");
        }

        this.id = UUID.randomUUID().toString();
        this.config = config;
    }

    public void decCooldown(double dtSeconds) {
        this.cooldown -= dtSeconds;
    }

    public String getId() {
        return this.id;
    }

    public double getCooldown() {
        return this.cooldown;
    }

    public TrailDto getConfig() {
        return new TrailDto(this.config);
    }

    protected boolean hasRequest() {
        return this.lastRequest.get() > this.lastHandledRequest.get();
    }

    protected void markAllRequestsHandled() {
        this.lastHandledRequest.set(this.lastRequest.get());
    }

    public void registerRequest() {
        this.lastRequest.set(System.nanoTime());
    }

    public void setCooldown(double cooldown) {
        this.cooldown = cooldown;
    }
}
