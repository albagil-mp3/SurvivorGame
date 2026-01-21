package model.emitter.implementations;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import model.emitter.ports.EmitterConfigDto;
import model.weapons.ports.WeaponDto;
import model.weapons.ports.WeaponState;

public class BurstEmitter extends BasicEmitter {

    private int bodiesRemainingInBursts = 0;
    private AtomicInteger bodiesRemaining = new AtomicInteger(0);

    public BurstEmitter(EmitterConfigDto emitterConfig) {
        super(emitterConfig);
    }


    public void decBodiesRemaining() {
        this.bodiesRemaining.decrementAndGet();
    }

    private int getBodiesRemaining() {
        return this.bodiesRemaining.get();
    }

    @Override
    public boolean mustEmitNow(double dtSeconds) {
        if (this.getCooldown() > 0) {
            // Cool down emitter between shots or between bursts.
            // Any pending requests.
            this.decCooldown(dtSeconds);
            this.markAllRequestsHandled();
            return false; // ========= Emitter is overheated ==========>
        }

        EmitterConfigDto emitterConfig = this.getConfig();

        if (this.getBodiesRemaining() <= 0) {
            // No ammunition: set time to reload, reload and discard requests
            this.markAllRequestsHandled();
            this.bodiesRemainingInBursts = 0; // cancel any ongoing burst
            this.setCooldown(emitterConfig.reloadTime);
            this.setBodiesRemaining(emitterConfig.maxBodiesEmitted);
            return false;
        }

        if (this.bodiesRemainingInBursts > 0) {
            // Burst mode ongoing...
            // Discard any pending requests while in burst mode
            this.markAllRequestsHandled();

            this.bodiesRemainingInBursts--;
            this.decBodiesRemaining();

            if (this.bodiesRemainingInBursts == 0) {
                // Burst finished. Cooldown between bursts
                this.setCooldown(1.0 / emitterConfig.emisionRate);
            } else {
                // More shots to fire in this burst. Cooldown between shots
                this.setCooldown(1.0 / emitterConfig.burstEmissionRate);
            }

            return true; // ======== Must emit now! ======>
        }

        if (!this.hasRequest()) {
            return false; // ===== No burst to start ======>
        }

        // Consume request and start new burst
        this.markAllRequestsHandled();

        int burstSize = Math.max(1, emitterConfig.burstSize);

        this.bodiesRemainingInBursts = burstSize - 1; // One shot now
        this.decBodiesRemaining();

        // Cooldown depends on whether burst continues
        if (this.bodiesRemainingInBursts == 0) {
            this.setCooldown(1.0 / emitterConfig.emisionRate); // between bursts
        } else {
            this.setCooldown(1.0 / emitterConfig.burstEmissionRate); // between burst shots
        }

        return true; // ========= Requesting first shot =========>
    }

    public void setBodiesRemaining(int bodiesRemaining) {
        this.bodiesRemaining.set(bodiesRemaining);
    }
}
