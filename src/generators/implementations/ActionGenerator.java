package generators.implementations;

import java.util.List;

import actions.ActionDTO;
import actions.ActionExecutor;
import actions.ActionPriority;
import actions.ActionType;
import events.domain.ports.DomainEventType;
import events.domain.ports.eventtype.CollisionEvent;
import events.domain.ports.eventtype.DomainEvent;
import events.domain.ports.eventtype.EmitEvent;
import events.domain.ports.eventtype.LifeOver;
import events.domain.ports.eventtype.LimitEvent;
import generators.ports.GameRulesEngine;
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
public class ActionGenerator implements GameRulesEngine {

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
            case LimitEvent e -> {
                ActionType actionType;

                switch (e.type) {
                    case REACHED_EAST_LIMIT:
                        actionType = ActionType.REBOUND_IN_EAST;
                        break;
                    case REACHED_WEST_LIMIT:
                        actionType = ActionType.REBOUND_IN_WEST;
                        break;
                    case REACHED_NORTH_LIMIT:
                        actionType = ActionType.REBOUND_IN_NORTH;
                        break;
                    case REACHED_SOUTH_LIMIT:
                        actionType = ActionType.REBOUND_IN_SOUTH;
                        break;
                    default:
                        actionType = ActionType.NONE;
                        break;
                }

                actions.add(new ActionDTO(
                        e.primaryBodyRef.id(), e.primaryBodyRef.type(),
                        actionType, ActionExecutor.BODY, ActionPriority.HIGH,
                        event));

            }

            case LifeOver e ->
                actions.add(new ActionDTO(
                        e.primaryBodyRef.id(), e.primaryBodyRef.type(),
                        ActionType.DIE, ActionExecutor.MODEL, ActionPriority.HIGH,
                        event));

            case EmitEvent e -> {

                if (e.type == DomainEventType.EMIT_REQUESTED) {
                    actions.add(new ActionDTO(
                            e.primaryBodyRef.id(),
                            e.primaryBodyRef.type(),
                            ActionType.SPAWN_BODY,
                            ActionExecutor.MODEL,
                            ActionPriority.LOW, event));

                } else {
                    actions.add(new ActionDTO(
                            e.primaryBodyRef.id(),
                            e.primaryBodyRef.type(),
                            ActionType.SPAWN_PROJECTILE,
                            ActionExecutor.MODEL,
                            ActionPriority.LOW, event));
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
        actions.add(new ActionDTO(event.primaryBodyRef.id(), event.primaryBodyRef.type(),
                ActionType.DIE, ActionExecutor.MODEL, ActionPriority.HIGH, event));
        actions.add(new ActionDTO(event.secondaryBodyRef.id(), event.secondaryBodyRef.type(),
                ActionType.DIE, ActionExecutor.MODEL, ActionPriority.HIGH, event));
    }
}
