package events.domain.ports.eventtype;

import events.domain.core.AbstractDomainEvent;
import events.domain.ports.BodyRefDTO;
import events.domain.ports.DomainEventType;
import events.domain.ports.payloads.NoPayload;

public final class LimitEvent extends AbstractDomainEvent <NoPayload> implements DomainEvent {

    public LimitEvent(
            DomainEventType type,
            BodyRefDTO primaryBodyRef) {

        super(type, primaryBodyRef, null);

        if (type != DomainEventType.REACHED_EAST_LIMIT &&
                type != DomainEventType.REACHED_WEST_LIMIT &&
                type != DomainEventType.REACHED_NORTH_LIMIT &&
                type != DomainEventType.REACHED_SOUTH_LIMIT) {

            throw new IllegalArgumentException("LimitEvent requires a limit event type");
        }
    }

}
