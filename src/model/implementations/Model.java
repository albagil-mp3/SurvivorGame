package model.implementations;

import static java.lang.System.nanoTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import actions.ActionDTO;
import actions.Action;
import events.domain.ports.BodyRefDTO;
import events.domain.ports.BodyToEmitDTO;
import events.domain.ports.DomainEventType;
import events.domain.ports.eventtype.CollisionEvent;
import events.domain.ports.eventtype.DomainEvent;
import events.domain.ports.eventtype.EmitEvent;
import events.domain.ports.eventtype.LifeOver;
import events.domain.ports.eventtype.LimitEvent;
import events.domain.ports.payloads.CollisionPayload;
import events.domain.ports.payloads.EmitPayloadDTO;

import java.awt.Dimension;
import java.util.HashSet;
import java.util.List;

import model.physics.ports.PhysicsValuesDTO;
import model.bodies.core.AbstractBody;
import model.bodies.implementations.DynamicBody;
import model.bodies.implementations.PlayerBody;
import model.bodies.implementations.ProjectileBody;
import model.bodies.ports.BodyDTO;
import model.bodies.ports.BodyEventProcessor;
import model.bodies.ports.BodyFactory;
import model.bodies.ports.BodyState;
import model.bodies.ports.BodyType;
import model.bodies.ports.PlayerDTO;
import model.emitter.implementations.BasicEmitter;
import model.emitter.ports.Emitter;
import model.emitter.ports.EmitterConfigDto;
import model.ports.DomainEventProcessor;
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
 * entities (dynamic bodies, gravity bodies, decorators) and orchestrates
 * their lifecycle, physics updates, collision detection, and domain event
 * processing.
 *
 * Responsibilities
 * ----------------
 * - Entity management: create, activate, and track all simulation entities
 * - Provide thread-safe snapshot data (BodyDTO) to the Controller for rendering
 * - Delegate physics updates to individual entity threads
 * - Detect domain events (collisions, limits, emissions, life over)
 * - Decide and execute actions based on domain events
 * - Manage spatial partitioning for efficient collision detection
 * - Enforce world boundaries and entity limits
 *
 * Entity types
 * ------------
 * The Model manages several distinct entity categories:
 *
 * 1) Dynamic Bodies (dynamicBodies)
 * - Entities with active physics simulation
 * - Includes: DYNAMIC, PLAYER, and PROJECTILE body types
 * - Each runs on its own thread, continuously updating position/velocity
 * - Players support thrust, rotation, firing commands, and weapon/emitter
 * equipment
 * - Projectiles track shooter ID and immunity period
 * - Stored in ConcurrentHashMap for thread-safe access
 *
 * 2) Gravity Bodies (gravityBodies)
 * - Static bodies that exert gravitational influence
 * - No physics thread
 * - Used for planetary bodies or black holes
 *
 * 3) Decorators (decorators)
 * - Visual-only entities with no gameplay impact (background elements,
 * particles)
 * - No physics thread
 * - Support limited lifetime
 *
 * Lifecycle
 * ---------
 * Construction:
 * - Model is created in STARTING state
 * - Requires worldWidth, worldHeight, and maxDynamicBodies parameters
 * - Entity maps are pre-allocated with expected capacities
 * - SpatialGrid is initialized with cell size 48 and max entities per cell 24
 *
 * Activation (activate()):
 * - Validates that DomainEventProcessor is set
 * - Transitions to ALIVE state
 * - After activation, entities can be created and activated
 *
 * Snapshot generation
 * -------------------
 * The Model provides snapshot methods that return DTO lists:
 * - getDynamicsData(): returns ArrayList<BodyDTO> for all dynamic bodies
 * - getStaticsData(): returns ArrayList<BodyDTO> for decorators + gravity
 * bodies
 * - getPlayerData(playerId): returns PlayerDTO for specific player
 *
 * These snapshots are pulled by the Controller and pushed to the View/Renderer.
 * The pattern ensures clean separation: rendering never accesses mutable
 * entity state directly.
 *
 * Event-Action Pipeline
 * ---------------------
 * The Model implements BodyEventProcessor interface to handle physics updates:
 *
 * 1) Event Detection (detectEvents):
 * - Limit events: when bodies reach world boundaries
 * - Collision events: detected via SpatialGrid broad-phase + circle
 * intersection
 * - Emission events: for particle systems and trails
 * - Fire events: weapon discharge from players
 * - Life over events: when maxLifeTime is reached
 *
 * 2) Action Decision (provideActions):
 * - Domain events are forwarded to DomainEventProcessor
 * - DomainEventProcessor returns ActionDTO list
 * - Movement action is added by default (unless body rebounded)
 *
 * 3) Action Execution (doActions):
 * - Actions are sorted by priority
 * - BODY executor: movement and rebounds delegated to entity
 * - MODEL executor: spawning, projectile emission, death handling
 *
 * Collision Detection
 * -------------------
 * - SpatialGrid provides O(1) broad-phase collision detection
 * - Only nearby entities are tested for collision
 * - Circle-circle intersection test for precise collision
 * - Projectile immunity system prevents self-collision after firing
 * - Collision deduplication via symmetry check (A-B = B-A)
 *
 * Concurrency strategy
 * --------------------
 * - All entity maps use ConcurrentHashMap for thread-safe access
 * - Individual entities manage their own thread synchronization
 * - Body state machine prevents concurrent event processing (HANDS_OFF state)
 * - Model state transitions are protected by volatile fields
 * - Snapshot methods create independent DTO lists to avoid concurrent
 * modification during rendering
 * - Scratch buffers reduce allocation pressure during snapshot generation
 *
 * Design goals
 * ------------
 * - Keep simulation logic isolated from view concerns
 * - Provide deterministic, thread-safe entity management
 * - Support high entity counts (up to MAX_ENTITIES = 5000)
 * - Enable efficient parallel physics updates via per-entity threads
 * - Minimize garbage collection via object pooling (scratch buffers)
 * - Spatial partitioning for O(n) collision detection instead of O(n²)
 */

public class Model implements BodyEventProcessor {

    private static final int MAX_ENTITIES = 5000;

    // region Fields
    private int maxBodies;
    private DomainEventProcessor domainEventProcessor = null;
    private volatile ModelState state = ModelState.STARTING;
    private final double worldWidth;
    private final double worldHeight;
    private final SpatialGrid spatialGrid;
    private final Map<String, AbstractBody> decorators = new ConcurrentHashMap<>(100);
    private final Map<String, AbstractBody> dynamicBodies = new ConcurrentHashMap<>(MAX_ENTITIES);
    private final Map<String, AbstractBody> gravityBodies = new ConcurrentHashMap<>(50);
    // endregion

    // regions Scratch buffers (for zero-allocation snapshot generation)
    private final ArrayList<BodyDTO> scratchDynamicsBuffer = new ArrayList<>(MAX_ENTITIES);
    // endregion

    // *** CONSTRUCTORS ***

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
        this.spatialGrid = new SpatialGrid(48, (int) worldWidth, (int) worldHeight, 24);
    }

    // *** PUBLICS ***

    public void activate() {
        if (this.domainEventProcessor == null) {
            throw new IllegalArgumentException("Controller is not set");
        }

        this.state = ModelState.ALIVE;
    }

    // region Body creation (add***)
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

        AbstractBody body = BodyFactory.create(
                this, this.spatialGrid, phyVals, bodyType, maxLifeTime, shooterId);

        body.activate();

        Map<String, AbstractBody> bodyMap = this.getBodyMap(bodyType);
        bodyMap.put(body.getBodyId(), body);
        this.spatialGridUpsert(body); // &++

        return body.getBodyId();
    }

    public String addDecorator(double size, double posX, double posY, double angle, double maxLifeInSeconds) {

        String entityId = this.addBody(BodyType.DECORATOR, size, posX, posY,
                0, 0, 0, 0,
                angle, 0, 0,
                0, maxLifeInSeconds, null);

        return entityId;
    }

    public String addDynamic(double size,
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

    public String addPlayer(double size,
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

    public String addProjectile(double size,
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
    // endregion

    // region Body equipment (bodyEquip***)
    public String bodyEquipEmitter(String bodyId, EmitterConfigDto emitterConfig) {
        if (bodyId == null || bodyId.isEmpty()) {
            throw new IllegalArgumentException("BodyId cannot be null or empty");
        }
        if (emitterConfig == null) {
            throw new IllegalArgumentException("EmitterConfig cannot be null");
        }

        AbstractBody body = this.dynamicBodies.get(bodyId);
        if (body == null) {
            return ""; // ========= Body not found =========>
        }

        BasicEmitter emitter = new BasicEmitter(emitterConfig);
        String emitterId = body.emitterEquip(emitter);

        return emitterId;
    }

    public String bodyEquipTrail(String bodyId, EmitterConfigDto trailConfig) {
        if (bodyId == null || bodyId.isEmpty()) {
            throw new IllegalArgumentException("BodyId cannot be null or empty");
        }
        if (trailConfig == null) {
            throw new IllegalArgumentException("EmitterConfig cannot be null");
        }

        AbstractBody body = this.dynamicBodies.get(bodyId);
        if (body == null) {
            return ""; // ========= Body not found =========>
        }

        if (!(body instanceof DynamicBody)) {
            throw new IllegalArgumentException("bodyEquipTrail() -> body is not DynamicBody");
        }
        DynamicBody dBody = (DynamicBody) body;

        BasicEmitter trailEmitter = new BasicEmitter(trailConfig);
        String emitterId = dBody.trailEquip(trailEmitter);

        return emitterId;
    }
    // endregion

    // region Getters (get***)
    public int getAliveQuantity() {
        return AbstractBody.getAliveQuantity();
    }

    public AbstractBody getBody(String entityId, BodyType bodyType) {

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
        this.scratchDynamicsBuffer.clear();

        this.dynamicBodies.forEach((entityId, body) -> {
            BodyDTO bodyInfo = new BodyDTO(entityId, body.getBodyType(), body.getPhysicsValues());
            if (bodyInfo != null) {
                this.scratchDynamicsBuffer.add(bodyInfo);
            }
        });

        return this.scratchDynamicsBuffer;
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
    // endregion Getters

    // region Boolean getters (is***)
    public boolean isModelAlive() {
        return this.state == ModelState.ALIVE;
    }
    // endregion

    // region Player equipment (playerEquip***)
    public void playerEquipWeapon(String playerId, WeaponDto weaponConfig) {

        PlayerBody pBody = (PlayerBody) this.dynamicBodies.get(playerId);
        if (pBody == null) {
            return; // ========= Player not found =========>
        }

        Weapon weapon = WeaponFactory.create(weaponConfig);

        pBody.addWeapon(weapon);
    }
    // endregion

    // region Player Actions (player***)
    public void playerFire(String playerId) {
        PlayerBody pBody = (PlayerBody) this.dynamicBodies.get(playerId);
        if (pBody != null) {
            pBody.registerFireRequest();
        }
    }

    public void playerThrustOn(String playerId) {
        PlayerBody pBody = (PlayerBody) this.dynamicBodies.get(playerId);
        if (pBody != null) {
            pBody.thrustMaxOn();
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

    public void playerSelectNextWeapon(String playerId) {
        PlayerBody pBody = (PlayerBody) this.dynamicBodies.get(playerId);
        if (pBody == null) {
            return;
        }

        pBody.selectNextWeapon();
    }
    // endregion Player Actions

    // region Remove and destroy (remove***)
    public void removeBody(AbstractBody body) {
        body.die();

        switch (body.getBodyType()) {
            case PLAYER:
                this.domainEventProcessor.notifyPlayerIsDead(body.getBodyId());
                this.spatialGrid.remove(body.getBodyId());
                this.dynamicBodies.remove(body.getBodyId());
                break;

            case DYNAMIC:
            case PROJECTILE:
                this.domainEventProcessor.notifyDynamicIsDead(body.getBodyId());
                this.spatialGrid.remove(body.getBodyId());
                this.dynamicBodies.remove(body.getBodyId());
                break;

            case DECORATOR:
                this.decorators.remove(body.getBodyId());
                this.domainEventProcessor.notifyStaticIsDead(body.getBodyId());
                break;

            case GRAVITY:
                this.gravityBodies.remove(body.getBodyId());
                break;
            default:
                // Nada
        }

    }
    // endregion

    // region Setters
    public void setDomainEventProcessor(DomainEventProcessor domainEventProcessor) {
        this.domainEventProcessor = domainEventProcessor;
    }

    public void setMaxBodies(int maxDynamicBody) {
        this.maxBodies = maxDynamicBody;
    }
    // endregion

    // *** INTERFACE IMPLEMENTATIONS ***

    // region BodyEventProcessor
    @Override
    public void processBodyEvents(AbstractBody body,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        if (!isProcessable(body)) {
            return; // To avoid duplicate or unnecesary event processing ======>
        }

        BodyState previousState = body.getBodyState();
        body.setState(BodyState.HANDS_OFF);

        try {
            // 1 => Detect events -------------------
            List<DomainEvent> domainEvents = body.getScratchClearEvents();
            this.detectEvents(body, newPhyValues, oldPhyValues, domainEvents);

            // 2 => Decide actions ------------------
            List<ActionDTO> actions = body.getScratchClearActions();
            this.provideActions(body, domainEvents, actions);

            // 3 => Execute actions -----------------
            this.executeActionList(actions, newPhyValues, oldPhyValues);

        } catch (Exception e) { // Fallback anti-zombi
            if (body.getBodyState() == BodyState.HANDS_OFF) {
                body.setState(previousState);
            }

        } finally { // Getout: off HANDS_OFF ... if leaving
            if (body.getBodyState() == BodyState.HANDS_OFF) {
                body.setState(BodyState.ALIVE);
            }
        }
    }
    // endregion

    // *** PRIVATE ***

    // region Check methods (check***)
    private void checkCollisions(AbstractBody checkBody, PhysicsValuesDTO newPhyValues,
            List<DomainEvent> domainEvents) {
        if (checkBody == null)
            throw new IllegalArgumentException("checkCollisions() -> checkBody is null");
        if (newPhyValues == null)
            throw new IllegalArgumentException("checkCollisions() -> newPhyValues is null");
        if (domainEvents == null)
            throw new IllegalArgumentException("checkCollisions() -> domainEvents is null");

        if (!this.isCollidable(checkBody))
            return; // =========== Non-collidable body ============>

        final String checkBodyId = checkBody.getBodyId();
        ArrayList<String> candidates = checkBody.getScratchClearCandidateIds();
        this.spatialGrid.queryCollisionCandidates(checkBodyId, candidates);
        if (candidates.isEmpty())
            return; // =========== No candidates -> no collisions ============>

        HashSet<String> seen = checkBody.getScratchClearSeenCandidateIds();
        for (String bodyId : candidates) {
            if (bodyId == null || bodyId.isEmpty())
                continue;

            // Dedupe by multiple references y differents cells
            if (!seen.add(bodyId))
                continue;

            // Dedupe by symetry
            if (checkBodyId.compareTo(bodyId) >= 0)
                continue;

            final AbstractBody otherBody = this.dynamicBodies.get(bodyId);
            if (otherBody == null)
                continue;

            if (!this.isCollidable(otherBody))
                continue;

            final PhysicsValuesDTO otherPhyValues = otherBody.getPhysicsValues();
            if (otherPhyValues == null)
                continue;

            if (!intersectCircles(newPhyValues, otherPhyValues))
                continue;

            // Immunity check for projectiles
            boolean haveInmunity = false;

            // Primary body have inmunity
            if (otherBody.getBodyType() == BodyType.PROJECTILE) {
                ProjectileBody projectile = (ProjectileBody) otherBody;
                if (projectile.getShooterId().equals(checkBody.getBodyId())) {
                    haveInmunity = projectile.isImmune();
                }
            }

            // Secondary body have inmunity
            if (checkBody.getBodyType() == BodyType.PROJECTILE) {
                ProjectileBody projectile = (ProjectileBody) checkBody;
                if (projectile.getShooterId().equals(otherBody.getBodyId())) {
                    haveInmunity = projectile.isImmune();
                }
            }

            CollisionPayload payload = new CollisionPayload(haveInmunity);
            CollisionEvent collisionEvent = new CollisionEvent(
                    checkBody.getBodyRef(), otherBody.getBodyRef(), payload);
            domainEvents.add(collisionEvent);
        }
    }

    private void checkEmissionEvents2(AbstractBody checkBody, PhysicsValuesDTO newPhyValues,
            PhysicsValuesDTO oldPhyValues, List<DomainEvent> domainEvents) {

        // BodyType bodyType = checkBody.getBodyType();
        // BodyRefDTO primaryBodyRef = checkBody.getBodyRef();

        // if (bodyType == BodyType.PLAYER || bodyType == BodyType.DYNAMIC) {
        // DynamicBody pBody = (DynamicBody) checkBody;

        // double dtSeconds = (newPhyValues.timeStamp - oldPhyValues.timeStamp) /
        // 1_000_000_000.0;
        // if (pBody.mustEmitNow(dtSeconds)) {
        // EmitPayloadDTO payload = new EmitPayloadDTO(
        // primaryBodyRef, pBody.getBodyToEmitConfig());

        // EmitEvent emitEvent = new EmitEvent(
        // DomainEventType.EMIT_REQUESTED, primaryBodyRef, payload);

        // domainEvents.add(emitEvent);
        // }
        // }
    }

    private void checkEmissionEvents(AbstractBody checkBody, PhysicsValuesDTO newPhyValues,
            PhysicsValuesDTO oldPhyValues, List<DomainEvent> domainEvents) {

        BodyRefDTO primaryBodyRef = checkBody.getBodyRef();

        if (!(checkBody.emittersListEmpty())) {
            return; // ======= No emitters =======>
        }
        double dtSeconds = (newPhyValues.timeStamp - checkBody.getPhysicsValues().timeStamp) / 1_000_000_000.0;

        for (Emitter emitter : checkBody.emittersList()) {
            if (emitter.mustEmitNow(dtSeconds)) {
                EmitPayloadDTO payload = new EmitPayloadDTO(primaryBodyRef,
                        emitter.getBodyToEmitConfig());

                domainEvents.add(new EmitEvent(
                        DomainEventType.EMIT_REQUESTED,
                        primaryBodyRef,
                        payload));
            }
        }
    }

    private void checkFireEvents(AbstractBody checkBody,
            PhysicsValuesDTO newPhyValues, List<DomainEvent> domainEvents) {

        BodyType bodyType = checkBody.getBodyType();
        BodyRefDTO primaryBodyRef = checkBody.getBodyRef();

        if (bodyType == BodyType.PLAYER) {
            PlayerBody pBody = (PlayerBody) checkBody;

            if (pBody.mustFireNow(newPhyValues)) {
                EmitPayloadDTO payload = new EmitPayloadDTO(
                        primaryBodyRef, pBody.getProjectileConfig());

                EmitEvent fireEvent = new EmitEvent(DomainEventType.FIRE_REQUESTED,
                        primaryBodyRef, payload);

                domainEvents.add(fireEvent);
            }
        }
    }

    private void checkLifeOverEvents(AbstractBody checkBody, List<DomainEvent> domainEvents) {
        if (checkBody.isLifeOver()) {
            domainEvents.add(new LifeOver(checkBody.getBodyRef()));
        }
    }

    private void checkLimitEvents(AbstractBody body, PhysicsValuesDTO phyValues,
            List<DomainEvent> domainEvents) {

        if (phyValues.posX < 0) {
            domainEvents.add(new LimitEvent(DomainEventType.REACHED_EAST_LIMIT, body.getBodyRef()));
        }

        if (phyValues.posX >= this.worldWidth) {
            domainEvents.add(new LimitEvent(DomainEventType.REACHED_WEST_LIMIT, body.getBodyRef()));
        }

        if (phyValues.posY < 0) {
            domainEvents.add(new LimitEvent(DomainEventType.REACHED_NORTH_LIMIT, body.getBodyRef()));
        }

        if (phyValues.posY >= this.worldHeight) {
            domainEvents.add(new LimitEvent(DomainEventType.REACHED_SOUTH_LIMIT, body.getBodyRef()));
        }
    }
    // endregion

    private void provideActions(AbstractBody body, List<DomainEvent> domainEvents, List<ActionDTO> actions) {
        if (!domainEvents.isEmpty())
            this.domainEventProcessor.provideActions(domainEvents, actions);

        boolean actionWithMovementImplicit = actions.stream()
                .anyMatch(a -> a.action == Action.REBOUND_IN_EAST
                        || a.action == Action.REBOUND_IN_WEST
                        || a.action == Action.REBOUND_IN_NORTH
                        || a.action == Action.REBOUND_IN_SOUTH);

        if (!actionWithMovementImplicit)
            // Always add MOVE action except if body rebounded
            actions.add(new ActionDTO(
                    body.getBodyId(), body.getBodyType(), Action.MOVE, null));

    }

    private void detectEvents(AbstractBody checkBody,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues, List<DomainEvent> domainEvents) {

        // 1 => Limits (all bodies) -----------------------
        this.checkLimitEvents(checkBody, newPhyValues, domainEvents);

        // 2 => Collisions (all bodies) -------------------
        this.checkCollisions(checkBody, newPhyValues, domainEvents);

        // 3 => Emission on (dynamics and players) ----------
        this.checkEmissionEvents(checkBody, newPhyValues, oldPhyValues, domainEvents);

        // 4 => Fire (only players) -----------------------
        this.checkFireEvents(checkBody, newPhyValues, domainEvents);

        // 5 => Life over (all body types) -------------------
        this.checkLifeOverEvents(checkBody, domainEvents);
    }

    // region Execute actions (executeAction***)
    private void executeAction(ActionDTO action, AbstractBody body,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        if (body == null) {
            throw new IllegalArgumentException("doModelAction() -> body is null");
        }
        if (action == null) {
            throw new IllegalArgumentException("doModelAction() -> action is null");
        }

        switch (action.action) {
            case MOVE:
                body.doMovement(newPhyValues);
                spatialGridUpsert((AbstractBody) body);
                break;

            case REBOUND_IN_EAST:
                body.reboundInEast(newPhyValues, oldPhyValues,
                        this.worldWidth, this.worldHeight);
                spatialGridUpsert((AbstractBody) body);
                break;

            case REBOUND_IN_WEST:
                body.reboundInWest(newPhyValues, oldPhyValues,
                        this.worldWidth, this.worldHeight);
                spatialGridUpsert((AbstractBody) body);
                break;

            case REBOUND_IN_NORTH:
                body.reboundInNorth(newPhyValues, oldPhyValues,
                        this.worldWidth, this.worldHeight);
                spatialGridUpsert((AbstractBody) body);
                break;

            case REBOUND_IN_SOUTH:
                body.reboundInSouth(newPhyValues, oldPhyValues,
                        this.worldWidth, this.worldHeight);
                spatialGridUpsert((AbstractBody) body);
                break;

            case GO_INSIDE:
                // To-Do: lógica futura
                break;
            case SPAWN_BODY:
            case SPAWN_PROJECTILE:
                if (!(action.relatedEvent instanceof EmitEvent emitEvent))
                    break;

                EmitPayloadDTO payload = emitEvent.payload;
                if (payload == null || payload.bodyConfig == null)
                    break;

                this.spawnBody(body, payload.bodyConfig, newPhyValues);
                break;

            case DIE:
                this.removeBody(body);
                break;

            case EXPLODE_IN_FRAGMENTS:
                break;

            default:
        }
    }

    private void executeActionList(
            List<ActionDTO> actions, PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        if (actions == null || actions.isEmpty()) {
            return;
        }

        for (ActionDTO action : actions) {
            if (action == null || action.action == null) {
                continue;
            }

            AbstractBody targetBody = this.getBody(action.entityId, action.bodyType);
            if (targetBody == null) {
                continue; // Body already removed, skip this action
            }

            this.executeAction(action, targetBody, newPhyValues, oldPhyValues);

        }
    }
    // endregion

    private ArrayList<BodyDTO> getBodiesData(Map<String, AbstractBody> bodies) {
        ArrayList<BodyDTO> bodyInfos = new ArrayList<>(bodies.size());

        bodies.forEach((entityId, body) -> {
            BodyDTO bodyInfo = new BodyDTO(entityId, body.getBodyType(), body.getPhysicsValues());
            if (bodyInfo != null) {
                bodyInfos.add(bodyInfo);
            }
        });

        return bodyInfos;
    }

    private Map<String, AbstractBody> getBodyMap(BodyType bodyType) {
        Map<String, AbstractBody> bodyMap = null;

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

    private boolean isCollidable(AbstractBody body) {
        return body != null
                && body.getBodyState() != BodyState.DEAD
                && (body.getSpatialGrid() != null);
    }

    private boolean isProcessable(AbstractBody entity) {
        return entity != null
                && this.state == ModelState.ALIVE
                && entity.getBodyState() == BodyState.ALIVE;
    }

    private void spawnBody(AbstractBody body, BodyToEmitDTO bodyConfig, PhysicsValuesDTO newPhyValues) {
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
        double posX = newPhyValues.posX + directorX * bodyConfig.forwardOffset;
        double posY = newPhyValues.posY + directorY * bodyConfig.forwardOffset;

        posX = posX - directorY * bodyConfig.sideOffset;
        posY = posY + directorX * bodyConfig.sideOffset;
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
                0, maxLifeTime, body.getBodyId());

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

    private void spatialGridUpsert(AbstractBody body) {
        if (body == null)
            return;

        body.spatialGridUpsert();
    }
}