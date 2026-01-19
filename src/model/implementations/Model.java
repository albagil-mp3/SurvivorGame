package model.implementations;

import static java.lang.System.nanoTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.Dimension;
import java.util.HashSet;
import java.util.List;

import model.bodies.core.AbstractBody;
import model.bodies.implementations.DynamicBody;
import model.bodies.implementations.PlayerBody;
import model.physics.ports.PhysicsValuesDTO;
import model.bodies.ports.Body;
import model.bodies.ports.BodyDTO;
import model.bodies.ports.BodyEventProcessor;
import model.bodies.ports.BodyFactory;
import model.bodies.ports.BodyState;
import model.bodies.ports.BodyType;
import model.bodies.ports.PhysicsBody;
import model.bodies.ports.PlayerDTO;
import model.emitter.implementations.BasicEmitter;
import model.emitter.ports.BodyEmittedDTO;
import model.emitter.ports.Emitter;
import model.emitter.ports.EmitterDto;
import model.ports.ActionDTO;
import model.ports.ActionExecutor;
import model.ports.ActionPriority;
import model.ports.ActionType;
import model.ports.DomainEventProcessor;
import model.ports.Event;
import model.ports.EventType;
import model.ports.ModelState;
import model.spatial.core.SpatialGrid;
import model.spatial.ports.SpatialGridStatisticsDTO;
import model.weapons.ports.Weapon;
import model.weapons.ports.WeaponDto;
import model.weapons.ports.WeaponFactory;

/**
 * Model
 * -----
 *
 * Core simulation layer of the MVC triad. The Model owns and manages all
 * entities (dynamic bodies, static bodies, players, decorators) and
 * orchestrates
 * their lifecycle, physics updates, and interactions.
 *
 * Responsibilities
 * ----------------
 * - Entity management: create, activate, and track all simulation entities
 * - Provide thread-safe snapshot data (EntityInfoDTO / DBodyInfoDTO) to the
 * Controller for rendering
 * - Delegate physics updates to individual entity threads
 * - Maintain entity collections with appropriate concurrency strategies
 * - Enforce world boundaries and entity limits
 *
 * Entity types
 * ------------
 * The Model manages several distinct entity categories:
 *
 * 1) Dynamic Bodies (dBodies)
 * - Entities with active physics simulation (ships, asteroids, projectiles)
 * - Each runs on its own thread, continuously updating position/velocity
 * - Stored in ConcurrentHashMap for thread-safe access
 *
 * 2) Player Bodies (pBodies)
 * - Special dynamic bodies with player controls and weapons
 * - Keyed by player ID string
 * - Support thrust, rotation, and firing commands
 *
 * 3) Static Bodies (sBodies)
 * - Non-moving entities with fixed positions (obstacles, platforms)
 * - No physics thread
 * - Push-updated to View when created/modified
 *
 * 4) Gravity Bodies (gravityBodies)
 * - Static bodies that exert gravitational influence
 * - Used for planetary bodies or black holes
 *
 * 5) Decorators (decorators)
 * - Visual-only entities with no gameplay impact (background elements)
 * - Push-updated to View when created/modified
 *
 * Lifecycle
 * ---------
 * Construction:
 * - Model is created in STARTING state
 * - Entity maps are pre-allocated with expected capacities
 *
 * Activation (activate()):
 * - Validates that Controller, world dimensions, and max entities are set
 * - Transitions to ALIVE state
 * - After activation, entities can be created and activated
 *
 * Snapshot generation
 * -------------------
 * The Model provides snapshot methods that return immutable DTOs:
 * - getDBodyInfo(): returns List<DBodyInfoDTO> for all active dynamic bodies
 * - getSBodyInfo(): returns List<EntityInfoDTO> for all active static bodies
 * - getDecoratorInfo(): returns List<EntityInfoDTO> for all decorators
 *
 * These snapshots are pulled by the Controller and pushed to the View/Renderer.
 * The pattern ensures clean separation: rendering never accesses mutable
 * entity state directly.
 *
 * Concurrency strategy
 * --------------------
 * - All entity maps use ConcurrentHashMap for thread-safe access
 * - Individual entities manage their own thread synchronization
 * - Model state transitions are protected by volatile fields
 * - Snapshot methods create independent DTO lists to avoid concurrent
 * modification during rendering
 *
 * Design goals
 * ------------
 * - Keep simulation logic isolated from view concerns
 * - Provide deterministic, thread-safe entity management
 * - Support high entity counts (up to MAX_ENTITIES = 5000)
 * - Enable efficient parallel physics updates via per-entity threads
 */

public class Model implements BodyEventProcessor {

    private int maxBodies;

    private DomainEventProcessor domainEventProcessor = null;
    private volatile ModelState state = ModelState.STARTING;

    private static final int MAX_ENTITIES = 5000;
    private final double worldWidth;
    private final double worldHeight;
    private final SpatialGrid spatialGrid;
    private final Map<String, Body> decorators = new ConcurrentHashMap<>(100);
    private final Map<String, Body> dynamicBodies = new ConcurrentHashMap<>(MAX_ENTITIES);
    private final Map<String, Body> gravityBodies = new ConcurrentHashMap<>(50);

    //
    // CONSTRUCTORS
    //

    public Model(double worldWidth, double worldHeight, int maxDynamicBodies) {
        if (worldWidth <= 0 || worldHeight <= 0) {
            throw new IllegalArgumentException("Invalid world dimension");
        }

        if (maxDynamicBodies <= 0) {
            throw new IllegalArgumentException("Max dynamic bodies not set");
        }

        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.maxBodies = maxDynamicBodies;
        this.spatialGrid = new SpatialGrid(128, (int) worldWidth, (int) worldHeight, 16);
    }

    //
    // PUBLIC
    //

    public void activate() {
        if (this.domainEventProcessor == null) {
            throw new IllegalArgumentException("Controller is not set");
        }

        this.state = ModelState.ALIVE;
    }

    public String addBody(BodyType bodyType,
            double size,
            double posX, double posY, double speedX, double speedY,
            double accX, double accY,
            double angle, double angularSpeed, double angularAcc,
            double thrust, double maxLifeTime, String shooterId) {

        if (AbstractBody.getAliveQuantity() >= this.maxBodies) {
            return null; // ========= Max vObject quantity reached ==========>>
        }
        if (bodyType == null) {
            throw new IllegalArgumentException("Body type is null");
        }
        if (bodyType == BodyType.PROJECTILE && (shooterId == null || shooterId.isEmpty())) {
            throw new IllegalArgumentException("Projectile body requires shooterId");
        }
        if (maxLifeTime == 0) {
            throw new IllegalArgumentException("maxLifeInSeconds must be greater than zero o -1 for infinite life");
        }

        PhysicsValuesDTO phyVals = new PhysicsValuesDTO(
                nanoTime(), posX, posY, angle, size,
                speedX, speedY, accX, accY, angularSpeed, angularAcc,
                thrust);

        Body body = BodyFactory.create(
                this, this.spatialGrid, phyVals, bodyType, maxLifeTime, shooterId);

        body.activate();

        Map<String, Body> bodyMap = this.getBodyMap(bodyType);
        bodyMap.put(body.getEntityId(), body);
        this.upsertCommittedToGrid(body); // &++

        return body.getEntityId();
    }

    public String addDecorator(double size, double posX, double posY, double angle, double maxLifeInSeconds) {

        String entityId = this.addBody(BodyType.DECORATOR, size, posX, posY,
                0, 0, 0, 0,
                angle, 0, 0,
                0, maxLifeInSeconds, null);

        return entityId;
    }

    public String addDynamicBody(double size,
            double posX, double posY, double speedX, double speedY,
            double accX, double accY,
            double angle, double angularSpeed, double angularAcc,
            double thrust, double maxLifeInSeconds) {

        String entityId = this.addBody(BodyType.DYNAMIC,
                size, posX, posY, speedX, speedY, accX, accY,
                angle, angularSpeed, angularAcc,
                thrust, maxLifeInSeconds, null);

        return entityId;
    }

    public String addPlayerBody(double size,
            double posX, double posY, double speedX, double speedY,
            double accX, double accY,
            double angle, double angularSpeed, double angularAcc,
            double thrust, double maxLifeInSeconds) {

        String entityId = this.addBody(BodyType.PLAYER,
                size, posX, posY, speedX, speedY, accX, accY,
                angle, angularSpeed, angularAcc,
                thrust, maxLifeInSeconds, null);

        return entityId;
    }

    public String addProjectileBody(double size,
            double posX, double posY, double speedX, double speedY,
            double accX, double accY,
            double angle, double angularSpeed, double angularAcc,
            double thrust, double maxLifeInSeconds,
            String shooterId) {

        String entityId = this.addBody(BodyType.PROJECTILE,
                size, posX, posY, speedX, speedY, accX, accY,
                angle, angularSpeed, angularAcc,
                thrust, maxLifeInSeconds, shooterId);

        return entityId;
    }

    public void addWeaponToPlayer(String playerId, WeaponDto weaponConfig) {

        PlayerBody pBody = (PlayerBody) this.dynamicBodies.get(playerId);
        if (pBody == null) {
            return; // ========= Player not found =========>
        }

        Weapon weapon = WeaponFactory.create(weaponConfig);

        pBody.addWeapon(weapon);
    }

    public void addEmitterToPlayer(String playerId, EmitterDto emitterConfig) {
        PlayerBody pBody = (PlayerBody) this.dynamicBodies.get(playerId);
        if (pBody == null) {
            return; // ========= Player not found =========>
        }

        BasicEmitter emitter = new BasicEmitter(emitterConfig);
        pBody.setEmitter(emitter);
    }

    public void addTrailEmitter(String playerId, EmitterDto trailConfig) {
        PlayerBody pBody = (PlayerBody) this.dynamicBodies.get(playerId);
        if (pBody == null) {
            return; // ========= Player not found =========>
        }

        BasicEmitter trailEmitter = new BasicEmitter(trailConfig);
        pBody.setEmitter(trailEmitter);
    }

    public int getAliveQuantity() {
        return AbstractBody.getAliveQuantity();
    }

    public Body getBody(String entityId, BodyType bodyType) {

        switch (bodyType) {
            case DECORATOR:
                return this.decorators.get(entityId);

            case DYNAMIC:
            case PLAYER:
            case PROJECTILE:
                return this.dynamicBodies.get(entityId);

            case GRAVITY:
                return this.gravityBodies.get(entityId);

            default:
                return null;
        }
    }

    public int getCreatedQuantity() {
        return AbstractBody.getCreatedQuantity();
    }

    public int getDeadQuantity() {
        return AbstractBody.getDeadQuantity();
    }

    public ArrayList<BodyDTO> getDynamicsData() {
        return this.getBodiesData(this.dynamicBodies);
    }

    public int getMaxBodies() {
        return this.maxBodies;
    }

    public PlayerDTO getPlayerData(String playerId) {
        PlayerBody pBody = (PlayerBody) this.dynamicBodies.get(playerId);
        if (pBody == null) {
            return null;
        }

        PlayerDTO playerData = pBody.getData();
        return playerData;
    }

    public ArrayList<BodyDTO> getStaticsData() {
        ArrayList<BodyDTO> staticsInfo;

        staticsInfo = this.getBodiesData(this.decorators);
        staticsInfo.addAll(this.getBodiesData(this.gravityBodies));

        return staticsInfo;
    }

    public ModelState getState() {
        return this.state;
    }

    public SpatialGridStatisticsDTO getSpatialGridStatistics() {
        return this.spatialGrid.getStatistics();
    }

    public Dimension getWorldDimension() {
        return new Dimension((int) this.worldWidth, (int) this.worldHeight);
    }

    public boolean isAlive() {
        return this.state == ModelState.ALIVE;
    }

    public void killBody(Body body) {
        body.die();

        switch (body.getBodyType()) {
            case PLAYER:
                body.die();
                this.domainEventProcessor.notifyPlayerIsDead(body.getEntityId());
                this.spatialGrid.remove(body.getEntityId());
                this.dynamicBodies.remove(body.getEntityId());
                break;

            case DYNAMIC:
            case PROJECTILE:
                body.die();
                this.domainEventProcessor.notiyDynamicIsDead(body.getEntityId());
                this.spatialGrid.remove(body.getEntityId());
                this.dynamicBodies.remove(body.getEntityId());
                break;

            case DECORATOR:
                body.die();
                this.decorators.remove(body.getEntityId());
                this.domainEventProcessor.notiyStaticIsDead(body.getEntityId());
                break;

            case GRAVITY:
                this.gravityBodies.remove(body.getEntityId());
                break;
            default:
                // Nada
        }

    }

    public void playerFire(String playerId) {
        PlayerBody pBody = (PlayerBody) this.dynamicBodies.get(playerId);
        if (pBody != null) {
            pBody.registerFireRequest();
        }
    }

    public void playerThrustOn(String playerId) {
        PlayerBody pBody = (PlayerBody) this.dynamicBodies.get(playerId);
        if (pBody != null) {
            pBody.thrustOn();
        }
    }

    public void playerThrustOff(String playerId) {
        PlayerBody pBody = (PlayerBody) this.dynamicBodies.get(playerId);
        if (pBody != null) {
            pBody.thrustOff();
        }
    }

    public void playerReverseThrust(String playerId) {
        PlayerBody pBody = (PlayerBody) this.dynamicBodies.get(playerId);
        if (pBody != null) {
            pBody.reverseThrust();
        }
    }

    public void playerRotateLeftOn(String playerId) {
        PlayerBody pBody = (PlayerBody) this.dynamicBodies.get(playerId);
        if (pBody != null) {
            pBody.rotateLeftOn();
        }
    }

    public void playerRotateOff(String playerId) {
        PlayerBody pBody = (PlayerBody) this.dynamicBodies.get(playerId);
        if (pBody != null) {
            pBody.rotateOff();
        }
    }

    public void playerRotateRightOn(String playerId) {
        PlayerBody pBody = (PlayerBody) this.dynamicBodies.get(playerId);
        if (pBody != null) {
            pBody.rotateRightOn();
        }
    }

    @Override
    public void processBodyEvents(Body body,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        if (!isProcessable(body)) {
            return; // To avoid duplicate or unnecesary event processing ======>
        }

        BodyState previousState = body.getState();
        body.setState(BodyState.HANDS_OFF);

        try {
            // 1 => Detect events -------------------
            List<Event> events = this.detectEvents(body, newPhyValues, oldPhyValues);

            // 2 => Decide actions ------------------
            List<ActionDTO> actions = null;
            if (events != null && !events.isEmpty())
                actions = this.domainEventProcessor.decideActions(events);

            if (actions == null)
                actions = new ArrayList<>(4);

            boolean executorIsPhysicBody = actions.stream()
                    .anyMatch(a -> a.executor == ActionExecutor.PHYSICS_BODY);
            if (!executorIsPhysicBody)
                // MOVE is the default action to commit physics values when no other
                // PHYSICS_BODY action (rebound, teleport, etc.) is already doing it
                actions.add(new ActionDTO(body.getEntityId(), body.getBodyType(),
                        ActionType.MOVE, ActionExecutor.PHYSICS_BODY, ActionPriority.NORMAL));

            // 3 => Execute actions -----------------
            this.doActions(actions, newPhyValues, oldPhyValues);

        } catch (Exception e) { // Fallback anti-zombi
            if (body.getState() == BodyState.HANDS_OFF) {
                body.setState(previousState);
            }

        } finally { // Getout: off HANDS_OFF ... if leaving
            if (body.getState() == BodyState.HANDS_OFF) {
                body.setState(BodyState.ALIVE);
            }
        }
    }

    public void selectNextWeapon(String playerId) {
        PlayerBody pBody = (PlayerBody) this.dynamicBodies.get(playerId);
        if (pBody == null) {
            return;
        }

        pBody.selectNextWeapon();
    }

    public void setDomainEventProcessor(DomainEventProcessor domainEventProcessor) {
        this.domainEventProcessor = domainEventProcessor;
    }

    public void setMaxBodies(int maxDynamicBody) {
        this.maxBodies = maxDynamicBody;
    }

    //
    // PRIVATE
    //

    private List<Event> checkCollisions(Body checkBody, PhysicsValuesDTO newPhyValues) {
        if (checkBody == null || newPhyValues == null)
            return List.of();

        if (!this.isCollidable(checkBody))
            return List.of();

        final String checkBodyId = checkBody.getEntityId();

        ArrayList<String> candidates = checkBody.getScratchCandidateIds();
        this.spatialGrid.queryCollisionCandidates(checkBodyId, candidates);
        if (candidates.isEmpty())
            return List.of();

        List<Event> collisionEvents = null;
        HashSet<String> seen = checkBody.getScratchSeenCandidateIds();
        seen.clear();
        for (String bodyId : candidates) {
            if (bodyId == null || bodyId.isEmpty())
                continue;

            // Dedupe by multiple references y differents cells
            if (!seen.add(bodyId))
                continue;

            // Dedupe by symetry
            if (checkBodyId.compareTo(bodyId) >= 0)
                continue;

            final Body otherBody = this.dynamicBodies.get(bodyId);
            if (otherBody == null)
                continue;

            if (!this.isCollidable(otherBody))
                continue;

            final PhysicsValuesDTO otherPhyValues = otherBody.getPhysicsValues();
            if (otherPhyValues == null)
                continue;

            if (!intersectCircles(newPhyValues, otherPhyValues))
                continue;

            if (collisionEvents == null) {
                collisionEvents = new ArrayList<>(8);
            }
            collisionEvents.add(new Event(checkBody, otherBody, EventType.COLLISION));
        }
        return collisionEvents == null ? List.of() : collisionEvents;
    }

    private List<Event> checkLimitEvents(Body body, PhysicsValuesDTO phyValues) {
        // List<Event> limitEvents = new ArrayList<>(4);

        ArrayList<Event> limitEvents = null;
        if (phyValues.posX < 0) {
            if (limitEvents == null)
                limitEvents = new ArrayList<>(2);
            limitEvents.add(new Event(body, null, EventType.REACHED_EAST_LIMIT));
        }

        if (phyValues.posX >= this.worldWidth) {
            if (limitEvents == null)
                limitEvents = new ArrayList<>(2);
            limitEvents.add(new Event(body, null, EventType.REACHED_WEST_LIMIT));
        }

        if (phyValues.posY < 0) {
            if (limitEvents == null)
                limitEvents = new ArrayList<>(2);
            limitEvents.add(new Event(body, null, EventType.REACHED_NORTH_LIMIT));
        }

        if (phyValues.posY >= this.worldHeight) {
            if (limitEvents == null)
                limitEvents = new ArrayList<>(1);
            limitEvents.add(new Event(body, null, EventType.REACHED_SOUTH_LIMIT));
        }

        return limitEvents == null ? List.of() : limitEvents;
    }

    private List<Event> detectEvents(Body checkBody,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        ArrayList<Event> events = null;
        BodyType bodyType = checkBody.getBodyType();

        // 1 => Limits (all bodies) -----------------------
        final List<Event> limitEvents = this.checkLimitEvents(checkBody, newPhyValues);
        if (limitEvents != null && !limitEvents.isEmpty()) {
            events = new ArrayList<>(limitEvents.size() + 4);
            events.addAll(limitEvents);
        }

        // 2 => Collisions (all bodies) -------------------
        final List<Event> collisionEvents = this.checkCollisions(checkBody, newPhyValues);
        if (collisionEvents != null && !collisionEvents.isEmpty()) {
            if (events == null) {
                events = new ArrayList<>(collisionEvents.size() + 4);
            }
            events.addAll(collisionEvents);
        }

        // 3 => Emission on (dynamics and players) ----------
        if (bodyType == BodyType.PLAYER ||
                bodyType == BodyType.DYNAMIC) {
            DynamicBody pBody = (DynamicBody) checkBody;

            if (pBody.mustEmitNow(newPhyValues)) {
                if (events == null)
                    events = new ArrayList<>(2);
                events.add(new Event(checkBody, null, EventType.MUST_EMIT));
            }
        }

        // 4 => Fire (only players) -----------------------
        if (bodyType == BodyType.PLAYER) {
            PlayerBody pBody = (PlayerBody) checkBody;

            if (pBody.mustFireNow(newPhyValues)) {
                if (events == null)
                    events = new ArrayList<>(2);
                events.add(new Event(checkBody, null, EventType.MUST_FIRE));
            }
        }

        // 5 => Life over (all bodies) --------------------
        if (checkBody.isLifeOver()) {
            if (events == null)
                events = new ArrayList<>(1);
            events.add(new Event(checkBody, null, EventType.LIFE_OVER));
        }

        return events == null ? List.of() : events;
    }

    private void doActions(
            List<ActionDTO> actions, PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        if (actions == null || actions.isEmpty()) {
            return;
        }

        // actions.sort(Comparator.comparing(a -> a.priority));

        for (ActionDTO action : actions) {
            if (action == null || action.type == null) {
                continue;
            }

            Body targetBody = this.getBody(action.entityId, action.bodyType);
            if (targetBody == null) {
                continue; // Body already removed, skip this action
            }

            switch (action.executor) {
                case BODY:
                    this.doBodyAction(action.type, targetBody, newPhyValues, oldPhyValues);
                    break;

                case PHYSICS_BODY:
                    this.doPhysicsBodyAction(action.type, (PhysicsBody) targetBody, newPhyValues, oldPhyValues);
                    break;

                case MODEL:
                    this.doModelAction(action.type, targetBody, newPhyValues, oldPhyValues);
                    break;

                default:
                    // Nada
            }
        }
    }

    private void doBodyAction(ActionType action, Body body,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        if (body == null) {
            return;
        }

        switch (action) {
            case NONE:
            default:
                // Nothing to do...
        }
    }

    private void doPhysicsBodyAction(ActionType action, PhysicsBody body,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        if (body == null) {
            return;
        }

        switch (action) {
            case MOVE:
                body.doMovement(newPhyValues);
                upsertCommittedToGrid((Body) body);
                break;

            case REBOUND_IN_EAST:
                body.reboundInEast(newPhyValues, oldPhyValues,
                        this.worldWidth, this.worldHeight);
                upsertCommittedToGrid((Body) body);
                break;

            case REBOUND_IN_WEST:
                body.reboundInWest(newPhyValues, oldPhyValues,
                        this.worldWidth, this.worldHeight);
                upsertCommittedToGrid((Body) body);
                break;

            case REBOUND_IN_NORTH:
                body.reboundInNorth(newPhyValues, oldPhyValues,
                        this.worldWidth, this.worldHeight);
                upsertCommittedToGrid((Body) body);
                break;

            case REBOUND_IN_SOUTH:
                body.reboundInSouth(newPhyValues, oldPhyValues,
                        this.worldWidth, this.worldHeight);
                upsertCommittedToGrid((Body) body);
                break;

            case GO_INSIDE:
                // To-Do: lógica futura
                break;

            case NONE:
            default:
                // Nothing to do...
        }
    }

    private void doModelAction(ActionType action, Body body,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        if (body == null) {
            return;
        }

        switch (action) {
            case SPAWN_PROJECTILE:
                if (!(body instanceof PlayerBody)) {
                    throw new IllegalArgumentException("Body is not a player and cannot spawn projectiles!");
                }
                this.spawnBody(body, ((PlayerBody) body).getProjectileConfig(), newPhyValues);
                break;

            case DIE:
                this.killBody(body);
                break;

            case EXPLODE_IN_FRAGMENTS:
                break;

            case SPAWN_BODY:
                if (!(body instanceof Emitter)) {
                    throw new IllegalArgumentException("Body is not an emitter and cannot spawn bodies!");
                }
                this.spawnBody(body, ((Emitter) body).getBodyEmittedConfig(), newPhyValues);
            default:
        }
    }

    private ArrayList<BodyDTO> getBodiesData(Map<String, Body> bodies) {
        ArrayList<BodyDTO> bodyData = new ArrayList<BodyDTO>(bodies.size());

        bodies.forEach((entityId, body) -> {
            BodyDTO bodyInfo = new BodyDTO(entityId, body.getBodyType(), body.getPhysicsValues());
            if (bodyInfo != null) {
                bodyData.add(bodyInfo);
            }
        });

        return bodyData;
    }

    private Map<String, Body> getBodyMap(BodyType bodyType) {
        Map<String, Body> bodyMap = null;

        switch (bodyType) {
            case DECORATOR:
                bodyMap = this.decorators;
                break;

            case DYNAMIC:
            case PLAYER:
            case PROJECTILE:
                bodyMap = this.dynamicBodies;
                break;
            case GRAVITY:
                bodyMap = this.gravityBodies;
                break;
        }

        if (bodyMap == null) {
            throw new IllegalArgumentException("Invalid body type: " + bodyType);
        }

        return bodyMap;
    }

    private boolean intersectCircles(PhysicsValuesDTO a, PhysicsValuesDTO b) {
        // OJO: asumo size = diámetro. Si size ya es radio: ra=a.size; rb=b.size;
        final double ra = a.size * 0.5;
        final double rb = b.size * 0.5;

        final double dx = a.posX - b.posX;
        final double dy = a.posY - b.posY;
        final double r = ra + rb;

        return (dx * dx + dy * dy) <= (r * r);
    }

    private boolean isCollidable(Body body) {
        return body != null
                && body.getState() != BodyState.DEAD
                && (body.getSpatialGrid()!= null);
    }

    private boolean isProcessable(Body entity) {
        return entity != null
                && this.state == ModelState.ALIVE
                && entity.getState() == BodyState.ALIVE;
    }

    private void spawnBody(Body body, BodyEmittedDTO bodyConfig, PhysicsValuesDTO newPhyValues) {
        if (body == null) {
            throw new IllegalArgumentException("Spawner body is null");
        }
        if (bodyConfig == null) {
            throw new IllegalArgumentException("BodyEmittedDTO is null");
        }
        if (newPhyValues == null) {
            throw new IllegalArgumentException("PhysicsValuesDTO is null");
        }

        double angleDeg = newPhyValues.angle;
        double angleRad = Math.toRadians(angleDeg);

        // Direction vector
        double directorX = Math.cos(angleRad);
        double directorY = Math.sin(angleRad);

        // Apply Offsets
        double posX = newPhyValues.posX + directorX * bodyConfig.xOffset;
        double posY = newPhyValues.posY + directorY * bodyConfig.xOffset;

        posX = posX - directorY * bodyConfig.yOffset;
        posY = posY + directorX * bodyConfig.yOffset;
        // Body initial speed
        double speedX = bodyConfig.speed * directorX;
        double speedY = bodyConfig.speed * directorY;

        // Body acceleration
        double accX = bodyConfig.acceleration * directorX;
        double accY = bodyConfig.acceleration * directorY;
        // Spawn body
        if (bodyConfig.randomAngle) {
            angleDeg = Math.random() * 360d;
        }
        double size = bodyConfig.size;
        if (bodyConfig.randomSize) {
            size = bodyConfig.size * (2.5 * Math.random());
        }

        double maxLifeTime = bodyConfig.maxLifeTime;
        maxLifeTime = maxLifeTime * (0.5 + 2 * Math.random());

        String entityId = this.addBody(bodyConfig.type,
                size, posX, posY, speedX, speedY, accX, accY,
                angleDeg, 0, 0,
                0, maxLifeTime, body.getEntityId());

        if (entityId == null || entityId.isEmpty()) {
            return; // ======= Max entity quantity reached =======>
        }

        if (bodyConfig.type == BodyType.DECORATOR ||
                bodyConfig.type == BodyType.GRAVITY) {

            this.domainEventProcessor.notifyNewStatic(
                    entityId, bodyConfig.assetId);
            return;
        }

        this.domainEventProcessor.notifyNewDynamic(
                entityId, bodyConfig.assetId);
    }

    private void upsertCommittedToGrid(Body body) {
        if (body == null)
            return;

        body.spatialGridUpsert();
    }
}