package utils.events.domain.ports.eventtype;

public sealed interface DomainEvent permits 
        CollisionEvent, EmitEvent, LimitEvent, LifeOver {

}