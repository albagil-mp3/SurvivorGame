package model.bodies.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import events.domain.ports.BodyRefDTO;
import events.domain.ports.eventtype.DomainEvent;
import model.bodies.ports.BodyEventProcessor;
import model.bodies.ports.BodyState;
import model.bodies.ports.BodyType;
import model.physics.ports.PhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;
import model.ports.ActionDTO;
import model.spatial.core.SpatialGrid;

/**
 *
 * @author juanm
 */
public abstract class AbstractBody {

    private static volatile int aliveQuantity = 0;
    private static volatile int createdQuantity = 0;
    private static volatile int deadQuantity = 0;

    private final BodyEventProcessor bodyEventProcessor;
    private volatile BodyState state;
    private final BodyType bodyType;
    private final String entityId;
    private final PhysicsEngine phyEngine;
    private final long bornTime = System.nanoTime();
    private final double maxLifeInSeconds; // Infinite life by default

    // Spatial grid and buffers for collision detection and avoiding garbage
    // creation during the
    // physics update. ==> Zero allocation strategy
    private final SpatialGrid spatialGrid;
    private final int[] scratchIdxs;
    private final ArrayList<String> scratchCandidateIds;
    private final HashSet<String> scratchSeenCandidateIds = new HashSet<>(64);

    // Buffers and precomputed DTOs for events and actions processing
    private final BodyRefDTO bodyRef;
    private final ArrayList<DomainEvent> scratchEvents = new ArrayList<>(32);
    private final List<ActionDTO> scratchActions = new ArrayList<>(32);

    /**
     * CONSTRUCTORS
     */

    public AbstractBody(BodyEventProcessor bodyEventProcessor, SpatialGrid spatialGrid,
            PhysicsEngine phyEngine, BodyType bodyType,
            double maxLifeInSeconds) {

        this.bodyEventProcessor = bodyEventProcessor;
        this.phyEngine = phyEngine;
        this.bodyType = bodyType;
        this.maxLifeInSeconds = maxLifeInSeconds;

        if (spatialGrid != null) {
            this.spatialGrid = spatialGrid;
            this.scratchIdxs = new int[spatialGrid.getMaxCellsPerBody()];
            this.scratchCandidateIds = new ArrayList<String>(64);

        } else {
            this.spatialGrid = null;
            this.scratchIdxs = null;
            this.scratchCandidateIds = null;
        }

        this.entityId = UUID.randomUUID().toString();
        this.state = BodyState.STARTING;
        this.bodyRef = new BodyRefDTO(this.entityId, this.bodyType);
    }

    public synchronized void activate() {
        if (this.state != BodyState.STARTING) {
            throw new IllegalArgumentException("Entity activation error due is not starting!");
        }

        AbstractBody.aliveQuantity++;
        this.state = BodyState.ALIVE;
    }

    public synchronized void die() {
        if (this.state == BodyState.DEAD) {
            return;
        }

        this.state = BodyState.DEAD;
        AbstractBody.deadQuantity++;

        if (AbstractBody.aliveQuantity > 0) {
            AbstractBody.aliveQuantity--;
        }
    }

    public BodyRefDTO getBodyRef() {
        return this.bodyRef;
    }

    public long getBornTime() {
        return this.bornTime;
    }

    public String getEntityId() {
        return this.entityId;
    }

    public double getLifeInSeconds() {
        return (System.nanoTime() - this.bornTime) / 1_000_000_000.0D;
    }

    public double getLifePercentage() {
        if (this.maxLifeInSeconds <= 0) {
            return 1D;
        }

        return Math.min(1D, this.getLifeInSeconds() / this.maxLifeInSeconds);
    }

    public double getMaxLife() {
        return this.maxLifeInSeconds;
    }

    public PhysicsEngine getPhysicsEngine() {
        return this.phyEngine;
    }

    public PhysicsValuesDTO getPhysicsValues() {
        return this.phyEngine.getPhysicsValues();
    }

    public List<ActionDTO> getClearScratchActions() {
        this.scratchActions.clear();
        return this.scratchActions;
    }

    public ArrayList<String> getClearScratchCandidateIds() {
        this.scratchCandidateIds.clear();
        return scratchCandidateIds;
    }

    public HashSet<String> getClearScratchSeenCandidateIds() {
        this.scratchSeenCandidateIds.clear();
        return this.scratchSeenCandidateIds;
    }

    public ArrayList<DomainEvent> getClearScratchEvents() {
        this.scratchEvents.clear();
        return this.scratchEvents;
    }

    public int[] getScratchIdxs() {
        return this.scratchIdxs;
    }

    public SpatialGrid getSpatialGrid() {
        return this.spatialGrid;
    }

    public BodyState getState() {
        return this.state;
    }

    public BodyType getBodyType() {
        return this.bodyType;
    }

    public boolean isLifeOver() {
        if (this.maxLifeInSeconds < 0) {
            return false;
        }

        boolean lifeOver = this.getLifeInSeconds() >= this.maxLifeInSeconds;
        return lifeOver;
    }

    public void processBodyEvents(AbstractBody body, PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {
        this.bodyEventProcessor.processBodyEvents(body, newPhyValues, oldPhyValues);
    }

    public void setState(BodyState state) {
        this.state = state;
    }

    public void spatialGridUpsert() {
        if (this.spatialGrid == null) {
            return;
        }

        final PhysicsValuesDTO phyValues = this.getPhysicsValues();

        final double r = phyValues.size * 0.5; // si size es radio, r = committed.size
        final double minX = phyValues.posX - r;
        final double maxX = phyValues.posX + r;
        final double minY = phyValues.posY - r;
        final double maxY = phyValues.posY + r;

        this.spatialGrid.upsert(this.getEntityId(), minX, maxX, minY, maxY, this.getScratchIdxs());
    }

    //
    // STATICS
    //

    static public int getCreatedQuantity() {
        return AbstractBody.createdQuantity;
    }

    static public int getAliveQuantity() {
        return AbstractBody.aliveQuantity;
    }

    static public int getDeadQuantity() {
        return AbstractBody.deadQuantity;
    }

    static protected int incCreatedQuantity() {
        AbstractBody.createdQuantity++;

        return AbstractBody.createdQuantity;
    }

    static protected int incAliveQuantity() {
        AbstractBody.aliveQuantity++;

        return AbstractBody.aliveQuantity;
    }

    static protected int decAliveQuantity() {
        AbstractBody.aliveQuantity--;

        return AbstractBody.aliveQuantity;
    }

    static protected int incDeadQuantity() {
        AbstractBody.deadQuantity++;

        return AbstractBody.deadQuantity;
    }
}
