package events.domain.ports.eventtype;

import events.domain.core.AbstractDomainEvent;
import events.domain.ports.BodyRefDTO;
import events.domain.ports.DomainEventType;
import events.domain.ports.payloads.EmitPayloadDTO;

public final class EmitEvent extends AbstractDomainEvent<EmitPayloadDTO> implements DomainEvent {

    public EmitEvent(
            DomainEventType eventType,
            BodyRefDTO primaryBodyRef,
            EmitPayloadDTO payload) {

        super(eventType, primaryBodyRef, null, payload);
    }

}
