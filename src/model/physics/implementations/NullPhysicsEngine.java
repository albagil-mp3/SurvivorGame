package model.physics.implementations;

import model.physics.core.AbstractPhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;

public class NullPhysicsEngine extends AbstractPhysicsEngine {

    //
    // CONSTRUCTORS
    //

    public NullPhysicsEngine(PhysicsValuesDTO phyVals) {
        super(phyVals);
    }

    public NullPhysicsEngine(double size, double posX, double posY, double angle) {
        super(size, posX, posY, angle);
    }

    //
    // PUBLICS
    //

    @Override
    public void angularAccelerationInc(double angularAcc) {
    }

    @Override
    public PhysicsValuesDTO calcNewPhysicsValues() {
        return this.getPhysicsValues();
    }

    @Override
    public boolean isThrusting() {
        return false;
    }

    @Override
    public void reboundInEast(PhysicsValuesDTO newPhyVals, PhysicsValuesDTO oldPhyVals,
            double worldDim_x, double worldDim_y) {
    }

    @Override
    public void reboundInWest(PhysicsValuesDTO newPhyVals, PhysicsValuesDTO oldPhyVals,
            double worldDim_x, double worldDim_y) {
    }

    @Override
    public void reboundInNorth(PhysicsValuesDTO newPhyVals, PhysicsValuesDTO oldPhyVals,
            double worldDim_x, double worldDim_y) {
    }

    @Override
    public void reboundInSouth(PhysicsValuesDTO newPhyVals, PhysicsValuesDTO oldPhyVals,
            double worldDim_x, double worldDim_y) {
    }

    @Override
    public void setAngularAcceleration(double angularAcceleration) {
    }

    @Override
    public void setAngularSpeed(double angularSpeed) {
    }

    @Override
    public void setThrust(double thrust) {
    }

    @Override
    public void stopPushing() {
    }
}
