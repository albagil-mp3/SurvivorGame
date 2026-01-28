package events.domain.ports.eventtype;

import events.domain.core.AbstractDomainEvent;
import events.domain.ports.BodyRefDTO;
import events.domain.ports.DomainEventType;
import events.domain.ports.payloads.CollisionPayload;

public final class CollisionEvent extends AbstractDomainEvent<CollisionPayload> implements DomainEvent {

    public CollisionEvent(
            BodyRefDTO primaryBodyRef,
            BodyRefDTO secondaryBodyRef,
            CollisionPayload payload) {

        super(DomainEventType.COLLISION, primaryBodyRef, secondaryBodyRef, payload);    
        if (secondaryBodyRef == null)
            throw new IllegalArgumentException("CollisionEvent requires secondaryBody");
    }
}