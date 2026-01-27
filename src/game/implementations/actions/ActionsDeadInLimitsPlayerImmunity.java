package game.implementations.actions;

import java.util.List;

import controller.ports.ActionsGenerator;
import model.bodies.ports.BodyType;
import utils.actions.Action;
import utils.actions.ActionDTO;
import utils.events.domain.ports.DomainEventType;
import utils.events.domain.ports.eventtype.CollisionEvent;
import utils.events.domain.ports.eventtype.DomainEvent;
import utils.events.domain.ports.eventtype.EmitEvent;
import utils.events.domain.ports.eventtype.LifeOver;
import utils.events.domain.ports.eventtype.LimitEvent;

public class ActionsDeadInLimitsPlayerImmunity implements ActionsGenerator {

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

            case LifeOver e ->
                actions.add(new ActionDTO(
                        e.primaryBodyRef.id(), e.primaryBodyRef.type(),
                        Action.DIE, event));

            case EmitEvent e -> {

                if (e.type == DomainEventType.EMIT_REQUESTED) {
                    actions.add(new ActionDTO(
                            e.primaryBodyRef.id(),
                            e.primaryBodyRef.type(),
                            Action.SPAWN_BODY,
                            event));

                } else {
                    actions.add(new ActionDTO(
                            e.primaryBodyRef.id(),
                            e.primaryBodyRef.type(),
                            Action.SPAWN_PROJECTILE,
                            event));
                }

            }

            case CollisionEvent e -> {

                // No action for collision events in this generator
            }
        }
    }
}
