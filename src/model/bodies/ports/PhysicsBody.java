package model.bodies.ports;

import model.physics.ports.PhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;

public interface PhysicsBody extends Body {

        public void doMovement(PhysicsValuesDTO phyValues);

        public PhysicsEngine getPhysicsEngine();

        public PhysicsValuesDTO getPhysicsValues();

        public boolean isThrusting();

        public void reboundInEast(PhysicsValuesDTO newVals, PhysicsValuesDTO oldVals,
                        double worldWidth, double worldHeight);

        public void reboundInWest(PhysicsValuesDTO newVals, PhysicsValuesDTO oldVals,
                        double worldWidth, double worldHeight);

        public void reboundInNorth(PhysicsValuesDTO newVals, PhysicsValuesDTO oldVals,
                        double worldWidth, double worldHeight);

        public void reboundInSouth(PhysicsValuesDTO newVals, PhysicsValuesDTO oldVals,
                        double worldWidth, double worldHeight);

        public void setThread(Thread thread);
}
