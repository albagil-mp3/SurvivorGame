package utils.events.domain.ports.eventtype;

import utils.events.domain.core.AbstractDomainEvent;
import utils.events.domain.ports.BodyRefDTO;
import utils.events.domain.ports.DomainEventType;
import utils.events.domain.ports.payloads.EmitPayloadDTO;

public final class EmitEvent extends AbstractDomainEvent<EmitPayloadDTO> implements DomainEvent {

    public EmitEvent(
            DomainEventType eventType,
            BodyRefDTO primaryBodyRef,
            EmitPayloadDTO payload) {

        super(eventType, primaryBodyRef, null, payload);
    }

}
