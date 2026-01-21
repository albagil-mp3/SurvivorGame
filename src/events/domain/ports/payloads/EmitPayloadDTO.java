package events.domain.ports.payloads;

import events.domain.ports.BodyRefDTO;
import events.domain.ports.BodyToEmitDTO;

public final class EmitPayloadDTO implements DomainEventPayload {

    public final BodyRefDTO emitterRef;
    public final BodyToEmitDTO bodyConfig;

    public EmitPayloadDTO(BodyRefDTO emitterRef, BodyToEmitDTO bodyConfig) {
        if (emitterRef == null)
            throw new IllegalArgumentException("EmitPayloadDTO.emitterRef is required");
        if (bodyConfig == null)
            throw new IllegalArgumentException("bodyConfig required");

        this.emitterRef = emitterRef;
        this.bodyConfig = bodyConfig;
    }
}
