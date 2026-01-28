package model.emitter.ports;

import events.domain.ports.BodyToEmitDTO;

public interface Emitter {

    public void decCooldown(double dtSeconds);

    public BodyToEmitDTO getBodyToEmitConfig();

    public EmitterConfigDto getConfig();

    public String getEmitterId();

    public boolean mustEmitNow(double dtSeconds);

    public void registerRequest();
}
