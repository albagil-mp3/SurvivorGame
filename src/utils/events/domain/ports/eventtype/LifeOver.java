package utils.events.domain.ports.eventtype;

import utils.events.domain.core.AbstractDomainEvent;
import utils.events.domain.ports.BodyRefDTO;
import utils.events.domain.ports.DomainEventType;
import utils.events.domain.ports.payloads.NoPayload;

public final class LifeOver extends AbstractDomainEvent<NoPayload> implements DomainEvent {

    public LifeOver(BodyRefDTO primaryBodyRef) {

        super(DomainEventType.LIFE_OVER, primaryBodyRef, null);

    }

}
