package model.bodies.ports;

import java.util.ArrayList;
import java.util.HashSet;

import model.physics.ports.PhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;
import model.spatial.core.SpatialGrid;

public interface Body {
    public void activate();

    public void die();

    public BodyType getBodyType();

    public long getBornTime();

    public String getEntityId();

    public double getLifeInSeconds();

    public double getLifePercentage();

    public double getMaxLife();

    public SpatialGrid getSpatialGrid();

    public int[] getScratchIdxs();

    public ArrayList<String> getScratchCandidateIds();

    public HashSet<String> getScratchSeenCandidateIds();

    public PhysicsEngine getPhysicsEngine();

    public PhysicsValuesDTO getPhysicsValues();

    public BodyState getState();

    public boolean isLifeOver();

    public void setState(BodyState state);

}
