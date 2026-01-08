package model.bodies.ports;

import model.implementations.Model;
import model.physics.ports.PhysicsValuesDTO;

public interface Body {
    public void activate();

    public void die();

    public long getBornTime();

    public String getEntityId();

    public double getLifeInSeconds();

    public double getLifePercentage();

    public double getMaxLife();

    public PhysicsValuesDTO getPhysicsValues();

    public BodyState getState();

    public BodyType getType();

    public boolean isLifeOver();

    public void setModel(Model model);

    public void setState(BodyState state);
}
