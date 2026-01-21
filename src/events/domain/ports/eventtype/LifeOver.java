package events.domain.ports.eventtype;

import events.domain.core.AbstractDomainEvent;
import events.domain.ports.BodyRefDTO;
import events.domain.ports.DomainEventType;
import events.domain.ports.payloads.NoPayload;

public final class LifeOver extends AbstractDomainEvent<NoPayload> implements DomainEvent {

    public LifeOver(BodyRefDTO primaryBodyRef) {

        super(DomainEventType.LIFE_OVER, primaryBodyRef, null);

    }

}
