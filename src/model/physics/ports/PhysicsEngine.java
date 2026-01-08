package model.physics.ports;

public interface PhysicsEngine {

        public void addAngularAcceleration(double angularAcc);

        public PhysicsValuesDTO calcNewPhysicsValues();

        public PhysicsValuesDTO getPhysicsValues();

        public void reboundInEast(
                        PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues,
                        double worldDim_x, double worldDim_y);

        public void reboundInWest(
                        PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues,
                        double worldDim_x, double worldDim_y);

        public void reboundInNorth(
                        PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues,
                        double worldDim_x, double worldDim_y);

        public void reboundInSouth(
                        PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues,
                        double worldDim_x, double worldDim_y);

        public void resetAcceleration();

        public void setAngularAcceleration(double angularAcceleration);

        public void setAngularSpeed(double angularSpeed);

        public void setPhysicsValues(PhysicsValuesDTO phyValues);

        public void setThrust(double thrust);
}
