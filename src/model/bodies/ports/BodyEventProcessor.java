package model.bodies.ports;

import model.bodies.core.AbstractBody;
import model.physics.ports.PhysicsValuesDTO;

public interface BodyEventProcessor {

    public void processBodyEvents(AbstractBody body, PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues);

}
