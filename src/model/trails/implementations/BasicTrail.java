package model.trails.implementations;

import model.trails.core.AbstractTrailEmitter;
import model.trails.ports.TrailDto;

public class BasicTrail extends AbstractTrailEmitter {

    public BasicTrail(TrailDto trailConfig) {
        super(trailConfig);
    }

    public boolean mustEmitNow(double dtSeconds) {
        if (this.getCooldown() > 0) {
            // Cool down trail emitter. Any pending requests are discarded.
            this.decCooldown(dtSeconds);
            this.markAllRequestsHandled();
            return false; // ======== Trail Emiter is overheated =========>
        }

        if (!this.hasRequest()) {
            // Nothing to do
            this.setCooldown(0);
            return false; // ==================>
        }

        // Emit
        this.markAllRequestsHandled();
        this.setCooldown(1.0 / this.getConfig().emisionRate);
        return true;
    }
}
