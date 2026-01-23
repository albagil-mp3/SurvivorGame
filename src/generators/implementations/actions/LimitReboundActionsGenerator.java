package generators.implementations.actions;

import java.util.List;

import actions.ActionDTO;
import actions.Action;
import events.domain.ports.DomainEventType;
import events.domain.ports.eventtype.CollisionEvent;
import events.domain.ports.eventtype.DomainEvent;
import events.domain.ports.eventtype.EmitEvent;
import events.domain.ports.eventtype.LifeOver;
import events.domain.ports.eventtype.LimitEvent;
import generators.ports.ActionsGenerator;
import model.bodies.ports.BodyType;

/**
 * DefaultGameRulesEngine
 * ----------------------
 *
 * Default rules implementation that mirrors the current Controller behavior.
 * It converts Model domain events into ActionDTO commands for the engine.
 *
 * Rules covered
 * -------------
 * - LimitEvent → REBOUND_IN_* (high priority, body executor)
 * - LifeOver → DIE (high priority, model executor)
 * - EmitEvent → SPAWN_BODY / SPAWN_PROJECTILE (low priority, model executor)
 * - CollisionEvent → resolved by resolveCollision (disabled by default)
 *
 * Design goals
 * ------------
 * - Keep rule logic isolated from Controller
 * - Enable swapping rules without touching core engine wiring
 */
public class LimitReboundActionsGenerator implements ActionsGenerator {

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
                Action action;

                switch (limitEvent.type) {
                    case REACHED_EAST_LIMIT:
                        action = Action.REBOUND_IN_EAST;
                        break;
                    case REACHED_WEST_LIMIT:
                        action = Action.REBOUND_IN_WEST;
                        break;
                    case REACHED_NORTH_LIMIT:
                        action = Action.REBOUND_IN_NORTH;
                        break;
                    case REACHED_SOUTH_LIMIT:
                        action = Action.REBOUND_IN_SOUTH;
                        break;
                    default:
                        action = Action.NONE;
                        break;
                }

                actions.add(new ActionDTO(
                        limitEvent.primaryBodyRef.id(), limitEvent.primaryBodyRef.type(),
                        action, event));

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

                // resolveCollision(e, actions);

            }
        }
    }

    private void resolveCollision(CollisionEvent event, List<ActionDTO> actions) {
        BodyType primary = event.primaryBodyRef.type();
        BodyType secondary = event.secondaryBodyRef.type();

        // Ignore collisions with DECORATOR bodies
        if (primary == BodyType.DECORATOR || secondary == BodyType.DECORATOR) {
            return;
        }

        // Check shooter immunity for PLAYER vs PROJECTILE and viceversa
        if (event.payload.haveImmunity) {
            return; // Projectile passes through its shooter during immunity period
        }

        // Default: Both die
        actions.add(new ActionDTO(
                event.primaryBodyRef.id(), event.primaryBodyRef.type(), Action.DIE, event));

        actions.add(new ActionDTO(
                event.secondaryBodyRef.id(), event.secondaryBodyRef.type(), Action.DIE, event));
    }
}
