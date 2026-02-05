package engine.model.physics.core;

import static java.lang.System.nanoTime;

import java.util.concurrent.atomic.AtomicReference;

import engine.model.physics.ports.PhysicsEngine;
import engine.model.physics.ports.PhysicsValuesDTO;

public abstract class AbstractPhysicsEngine implements PhysicsEngine {

        private final AtomicReference<PhysicsValuesDTO> phyValues; // *+

        // region Constructors
        public AbstractPhysicsEngine(PhysicsValuesDTO phyValues) {
                if (phyValues == null) {
                        throw new IllegalArgumentException("PhysicsValuesDTO cannot be null");
                }

                this.phyValues = new AtomicReference<>(phyValues);
        }

        public AbstractPhysicsEngine(double size, double posX, double posY, double angle) {
                this.phyValues = new AtomicReference<>(
                                new PhysicsValuesDTO(nanoTime(), size, posX, posY, angle));
        }
        // endregion

        // *** PUBLIC ***

        public abstract PhysicsValuesDTO calcNewPhysicsValues();

        public abstract void angularAccelerationInc(double angularAcc);

        public final PhysicsValuesDTO getPhysicsValues() {
                return this.phyValues.get();
        }

        // region Rebound (reboundIn***)
        public abstract void reboundInEast(
                        PhysicsValuesDTO phyValues, double worldDim_x, double worldDim_y);

        public abstract void reboundInWest(
                        PhysicsValuesDTO phyValues, double worldDim_x, double worldDim_y);

        public abstract void reboundInNorth(
                        PhysicsValuesDTO phyValues, double worldDim_x, double worldDim_y);

        public abstract void reboundInSouth(
                        PhysicsValuesDTO phyValues, double worldDim_x, double worldDim_y);
        // endregion

        public void resetAcceleration() {
                PhysicsValuesDTO old = this.getPhysicsValues();
                this.setPhysicsValues(new PhysicsValuesDTO(
                                old.timeStamp,
                                old.posX, old.posY, old.angle,
                                old.size,
                                old.speedX, old.speedY,
                                0, 0,
                                old.angularSpeed,
                                old.angularAcc,
                                old.thrust));

        }

        // region Setters (set***)
        public final void setAngularAcceleration(double angularAcc) {
                PhysicsValuesDTO old = this.getPhysicsValues();
                this.setPhysicsValues(new PhysicsValuesDTO(
                                old.timeStamp,
                                old.posX, old.posY, old.angle,
                                old.size,
                                old.speedX, old.speedY,
                                old.accX, old.accY,
                                old.angularSpeed,
                                angularAcc,
                                old.thrust));
        }

        public abstract void setAngularSpeed(double angularSpeed);

        public final void setPhysicsValues(PhysicsValuesDTO phyValues) {
                if (phyValues == null) {
                        throw new IllegalArgumentException("PhysicsValuesDTO cannot be null");
                }

                this.phyValues.set(phyValues);
        }

        public final void setThrust(double thrust) {
                PhysicsValuesDTO old = this.getPhysicsValues();
                this.setPhysicsValues(new PhysicsValuesDTO(
                                old.timeStamp,
                                old.posX, old.posY, old.angle,
                                old.size,
                                old.speedX, old.speedY,
                                old.accX, old.accY,
                                old.angularSpeed,
                                old.angularAcc,
                                thrust));
        }
        // endregion

        @Override
        public void stopPushing() {
                PhysicsValuesDTO old = this.getPhysicsValues();
                this.setPhysicsValues(new PhysicsValuesDTO(
                                old.timeStamp,
                                old.posX, old.posY, old.angle,
                                old.size,
                                old.speedX, old.speedY,
                                0, 0, // Reset accelerations
                                old.angularSpeed,
                                old.angularAcc,
                                0.0d)); // Reset thrust

        }
}
