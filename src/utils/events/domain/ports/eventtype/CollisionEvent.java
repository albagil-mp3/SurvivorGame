package utils.events.domain.ports.eventtype;

import utils.events.domain.core.AbstractDomainEvent;
import utils.events.domain.ports.BodyRefDTO;
import utils.events.domain.ports.DomainEventType;
import utils.events.domain.ports.payloads.CollisionPayload;

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