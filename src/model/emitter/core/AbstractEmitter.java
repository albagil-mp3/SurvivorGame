package model.emitter.core;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import events.domain.ports.BodyToEmitDTO;
import model.emitter.ports.Emitter;
import model.emitter.ports.EmitterConfigDto;

public abstract class AbstractEmitter implements Emitter {

    private final String id;
    private final EmitterConfigDto config;
    private final AtomicLong lastRequest = new AtomicLong(0L);
    private AtomicLong lastHandledRequest = new AtomicLong(0L);
    private volatile double cooldown = 0.0; // seconds

    public AbstractEmitter(EmitterConfigDto config) {
        if (config == null) {
            throw new IllegalArgumentException(
                    "config cannot be null. Emitter not created");
        }

        if (config.emisionRate <= 0) {
            throw new IllegalArgumentException(
                    "emisionRate must be > 0. Emitter not created");
        }

        this.id = UUID.randomUUID().toString();
        this.config = config;
    }

    @Override
    public void decCooldown(double dtSeconds) {
        this.cooldown -= dtSeconds;
    }

    @Override
    public String getId() {
        return this.id;
    }

    public double getCooldown() {
        return this.cooldown;
    }

    public BodyToEmitDTO getBodyToEmitConfig() {
        return this.config.bodyEmitted;
    }

    @Override
    public EmitterConfigDto getConfig() { //
        return this.config;
    }

    @Override
    public abstract boolean mustEmitNow(double dtSeconds);

    @Override
    public void registerRequest() {
        this.lastRequest.set(System.nanoTime());
    }

    public void setCooldown(double cooldown) {
        this.cooldown = cooldown;
    }

    //
    // PROTECTED METHODS
    //

    protected boolean hasRequest() {
        return this.lastRequest.get() > this.lastHandledRequest.get();
    }

    protected void markAllRequestsHandled() {
        this.lastHandledRequest.set(this.lastRequest.get());
    }

}
