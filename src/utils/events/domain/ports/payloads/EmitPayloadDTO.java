package utils.events.domain.ports.payloads;

import utils.events.domain.ports.BodyRefDTO;
import utils.events.domain.ports.BodyToEmitDTO;

public final class EmitPayloadDTO implements DomainEventPayload {

    public final BodyRefDTO emitterRef;
    public final BodyToEmitDTO bodyConfig;

    public EmitPayloadDTO(BodyRefDTO bodyEmitterRef, BodyToEmitDTO bodyConfig) {
        if (bodyEmitterRef == null)
            throw new IllegalArgumentException("EmitPayloadDTO.emitterRef is required");
        if (bodyConfig == null)
            throw new IllegalArgumentException("bodyConfig required");

        this.emitterRef = bodyEmitterRef;
        this.bodyConfig = bodyConfig;
    }
}
