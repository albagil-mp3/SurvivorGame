package model.emitter.implementations;

import model.emitter.core.AbstractEmitter;
import model.emitter.ports.EmitterConfigDto;

public class BasicEmitter extends AbstractEmitter  {

    public BasicEmitter(EmitterConfigDto trailConfig) {
        super(trailConfig);
    }

    @Override
    public boolean mustEmitNow(double dtSeconds) {
        if (this.getCooldown() > 0) {
            // Cool down time. Any pending requests are discarded.
            this.decCooldown(dtSeconds);
            this.markAllRequestsHandled();
            return false; // ======== Trail Emiter is overheated =========>
        }

        if (!this.hasRequest()) {
            // Nothing to do
            this.setCooldown(0);
            return false; // ======== No requests =========>
        }

        // Emit
        this.markAllRequestsHandled();
        this.setCooldown(1.0 / this.getConfig().emisionRate);
        return true;
    }


}
