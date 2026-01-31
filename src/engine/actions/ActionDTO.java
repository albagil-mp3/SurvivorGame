package engine.actions;

import engine.events.domain.ports.eventtype.DomainEvent;
import engine.model.bodies.ports.BodyType;

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