package gamerules;

import java.util.List;

import engine.actions.Action;
import engine.actions.ActionDTO;
import engine.controller.ports.ActionsGenerator;
import engine.events.domain.ports.DomainEventType;
import engine.events.domain.ports.eventtype.CollisionEvent;
import engine.events.domain.ports.eventtype.DomainEvent;
import engine.events.domain.ports.eventtype.EmitEvent;
import engine.events.domain.ports.eventtype.LifeOver;
import engine.events.domain.ports.eventtype.LimitEvent;
import engine.model.bodies.ports.BodyType;

public class DeadInLimitsPlayerImmunity implements ActionsGenerator {

    // *** INTERFACE IMPLEMENTATIONS ***

    @Override //
    public void provideActions(List<DomainEvent> domainEvents, List<ActionDTO> actions) {
        if (domainEvents != null) {
            for (DomainEvent event : domainEvents) {
                this.applyGameRules(event, actions);
            }
        }
    }

    // *** PRIVATE ***

    private void applyGameRules(DomainEvent event, List<ActionDTO> actions) {
        switch (event) {
            case LimitEvent limitEvent -> {

                Action action = Action.DIE;
                if (limitEvent.primaryBodyRef.type() == BodyType.PLAYER)
                    action = Action.NO_MOVE;

                actions.add(new ActionDTO(
                        limitEvent.primaryBodyRef.id(), limitEvent.primaryBodyRef.type(),
                        action, event));
                break;

            }

            case LifeOver lifeOver ->
                actions.add(new ActionDTO(
                        lifeOver.primaryBodyRef.id(), lifeOver.primaryBodyRef.type(),
                        Action.DIE, event));

            case EmitEvent emitEvent -> {

                if (emitEvent.type == DomainEventType.EMIT_REQUESTED) {
                    actions.add(new ActionDTO(
                            emitEvent.primaryBodyRef.id(),
                            emitEvent.primaryBodyRef.type(),
                            Action.SPAWN_BODY,
                            event));

                } else {
                    actions.add(new ActionDTO(
                            emitEvent.primaryBodyRef.id(),
                            emitEvent.primaryBodyRef.type(),
                            Action.SPAWN_PROJECTILE,
                            event));
                }

            }

            case CollisionEvent collisionEvent -> {

                this.resolveCollision(collisionEvent, actions);
            }
        }
    }

    private void resolveCollision(CollisionEvent event, List<ActionDTO> actions) {
        BodyType primary = event.primaryBodyRef.type();
        BodyType secondary = event.secondaryBodyRef.type();

        // Check shooter immunity for PLAYER vs PROJECTILE and viceversa
        if (event.payload.haveImmunity) {
            return; // Projectile passes through its shooter during immunity period
        }

        // Ignore collisions with DECORATOR bodies
        if (primary == BodyType.PLAYER) {
            actions.add(new ActionDTO(
                    event.primaryBodyRef.id(), event.primaryBodyRef.type(),
                    Action.NO_MOVE, event));

            return;
        }

        if (secondary == BodyType.PLAYER) {
            actions.add(new ActionDTO(
                    event.secondaryBodyRef.id(), event.secondaryBodyRef.type(),
                    Action.NO_MOVE, event));

            return;
        }

        // Default: Both die
        actions.add(new ActionDTO(
                event.primaryBodyRef.id(), event.primaryBodyRef.type(), Action.DIE, event));

        actions.add(new ActionDTO(
                event.secondaryBodyRef.id(), event.secondaryBodyRef.type(), Action.DIE, event));
    }

}
