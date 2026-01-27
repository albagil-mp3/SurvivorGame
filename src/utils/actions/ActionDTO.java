package utils.actions;

import model.bodies.ports.BodyType;
import utils.events.domain.ports.eventtype.DomainEvent;

public class ActionDTO {
    final public String entityId;
    final public BodyType bodyType;
    final public Action action;
    final public DomainEvent relatedEvent;

    public ActionDTO(String entityId, BodyType bodyType, Action type, DomainEvent relatedEvent) {
        this.entityId = entityId;
        this.bodyType = bodyType;
        this.action = type;
        this.relatedEvent = relatedEvent;
    }
}