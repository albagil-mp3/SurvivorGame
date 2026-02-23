package killergame;

import java.util.List;

import engine.actions.ActionDTO;
import engine.actions.ActionType;
import engine.controller.ports.ActionsGenerator;
import engine.events.domain.ports.DomainEventType;
import engine.events.domain.ports.eventtype.CollisionEvent;
import engine.events.domain.ports.eventtype.DomainEvent;
import engine.events.domain.ports.eventtype.EmitEvent;
import engine.events.domain.ports.eventtype.LifeOver;
import engine.events.domain.ports.eventtype.LimitEvent;
import engine.model.bodies.ports.BodyType;
import engine.model.impl.Model;

/**
 * Game rules for Killer Game - Maze Chase Edition.
 * Defines how entities react to events in the maze:
 * - Enemies are destroyed only by projectiles (bullets)
 * - Player and enemies bounce off walls and each other
 * - Goal: Hunt down all enemies with your weapon
 */
public class KillerGameRules implements ActionsGenerator {

    private static final int KILL_SCORE = 10;

    private final Model model;

    public KillerGameRules(Model model) {
        this.model = model;
    }

    // *** INTERFACE IMPLEMENTATIONS ***

    @Override
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
            // Limit events - entities rebound at maze boundaries
            case LimitEvent limitEvent -> {
                ActionType action;

                switch (limitEvent.type) {
                    case REACHED_EAST_LIMIT:
                        action = ActionType.MOVE_REBOUND_IN_EAST;
                        break;
                    case REACHED_WEST_LIMIT:
                        action = ActionType.MOVE_REBOUND_IN_WEST;
                        break;
                    case REACHED_NORTH_LIMIT:
                        action = ActionType.MOVE_REBOUND_IN_NORTH;
                        break;
                    case REACHED_SOUTH_LIMIT:
                        action = ActionType.MOVE_REBOUND_IN_SOUTH;
                        break;
                    default:
                        action = ActionType.NO_MOVE;
                        break;
                }

                actions.add(new ActionDTO(
                        limitEvent.primaryBodyRef.id(),
                        limitEvent.primaryBodyRef.type(),
                        action,
                        event));
            }

            // Life over - entity dies when health reaches 0 (players are immortal)
            case LifeOver e -> {
                if (e.primaryBodyRef.type() != BodyType.PLAYER) {
                    actions.add(new ActionDTO(
                            e.primaryBodyRef.id(),
                            e.primaryBodyRef.type(),
                            ActionType.DIE,
                            event));
                }
            }

            // Collision events - handle collisions in the maze
            case CollisionEvent e -> {
                BodyType primaryType = e.primaryBodyRef.type();
                BodyType secondaryType = e.secondaryBodyRef.type();

                // Projectile hits enemy - enemy dies, projectile dies, +10 score
                if (primaryType == BodyType.PROJECTILE && secondaryType == BodyType.DYNAMIC) {
                    actions.add(new ActionDTO(
                            e.secondaryBodyRef.id(),
                            e.secondaryBodyRef.type(),
                            ActionType.DIE,
                            event));
                    actions.add(new ActionDTO(
                            e.primaryBodyRef.id(),
                            e.primaryBodyRef.type(),
                            ActionType.DIE,
                            event));
                    this.model.playerAddScoreToAll(KILL_SCORE);
                }
                // Enemy hits projectile - enemy dies, projectile dies, +10 score
                else if (primaryType == BodyType.DYNAMIC && secondaryType == BodyType.PROJECTILE) {
                    actions.add(new ActionDTO(
                            e.primaryBodyRef.id(),
                            e.primaryBodyRef.type(),
                            ActionType.DIE,
                            event));
                    actions.add(new ActionDTO(
                            e.secondaryBodyRef.id(),
                            e.secondaryBodyRef.type(),
                            ActionType.DIE,
                            event));
                    this.model.playerAddScoreToAll(KILL_SCORE);
                }
                // Projectile hits wall (GRAVITY) - projectile dies
                else if (primaryType == BodyType.PROJECTILE && secondaryType == BodyType.GRAVITY) {
                    actions.add(new ActionDTO(
                            e.primaryBodyRef.id(),
                            e.primaryBodyRef.type(),
                            ActionType.DIE,
                            event));
                }
                // Wall hits projectile - projectile dies
                else if (primaryType == BodyType.GRAVITY && secondaryType == BodyType.PROJECTILE) {
                    actions.add(new ActionDTO(
                            e.secondaryBodyRef.id(),
                            e.secondaryBodyRef.type(),
                            ActionType.DIE,
                            event));
                }
                // Player hits wall - use rebound system that works correctly
                else if (primaryType == BodyType.PLAYER && secondaryType == BodyType.GRAVITY) {
                    actions.add(new ActionDTO(
                            e.primaryBodyRef.id(),
                            e.primaryBodyRef.type(),
                            ActionType.NO_MOVE,
                            event));
                }
                // Wall hits player - use rebound system
                else if (primaryType == BodyType.GRAVITY && secondaryType == BodyType.PLAYER) {
                    actions.add(new ActionDTO(
                            e.secondaryBodyRef.id(),
                            e.secondaryBodyRef.type(),
                            ActionType.NO_MOVE,
                            event));
                }
                // Player hits enemy - both bounce (no death)
                else if ((primaryType == BodyType.PLAYER && secondaryType == BodyType.DYNAMIC) ||
                         (primaryType == BodyType.DYNAMIC && secondaryType == BodyType.PLAYER)) {
                    // Let physics handle the bounce
                }
                // Enemy hits enemy - both bounce
                else if (primaryType == BodyType.DYNAMIC && secondaryType == BodyType.DYNAMIC) {
                    // Let physics handle the bounce
                }
                // Enemy hits wall - enemy bounces
                else if ((primaryType == BodyType.DYNAMIC && secondaryType == BodyType.GRAVITY) ||
                         (primaryType == BodyType.GRAVITY && secondaryType == BodyType.DYNAMIC)) {
                    String enemyId = (primaryType == BodyType.DYNAMIC)
                        ? e.primaryBodyRef.id()
                        : e.secondaryBodyRef.id();
                    actions.add(new ActionDTO(
                        enemyId,
                        BodyType.DYNAMIC,
                        ActionType.NO_MOVE,
                        event));
                }
            }

            // Emit events - handle weapon fire and other emissions
            case EmitEvent e -> {
                if (e.type == DomainEventType.EMIT_REQUESTED) {
                    actions.add(new ActionDTO(
                            e.primaryBodyRef.id(),
                            e.primaryBodyRef.type(),
                            ActionType.SPAWN_BODY,
                            event));
                } else { // EMIT_PROJECTILE
                    actions.add(new ActionDTO(
                            e.primaryBodyRef.id(),
                            e.primaryBodyRef.type(),
                            ActionType.SPAWN_PROJECTILE,
                            event));
                }
            }

            default -> {
                // Ignore other event types
            }
        }
    }
}
