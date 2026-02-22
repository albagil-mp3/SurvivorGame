package engine.model.physics.implementations;

import static java.lang.System.nanoTime;

import engine.model.bodies.ports.BodyType;
import engine.model.physics.core.AbstractPhysicsEngine;
import engine.model.physics.ports.PhysicsValuesDTO;
import engine.utils.profiling.impl.BodyProfiler;

public class BasicPhysicsEngine extends AbstractPhysicsEngine {

    // region Fields
    private final BodyProfiler profiler;
    private final BodyType bodyType;
    // endregion

    // region Constructors
    public BasicPhysicsEngine(PhysicsValuesDTO dto1, PhysicsValuesDTO dto2, PhysicsValuesDTO dto3, 
                              BodyType bodyType, BodyProfiler profiler) {
        super(dto1, dto2, dto3);
        this.profiler = profiler;
        this.bodyType = bodyType;
    }
    // endregion

    // *** PUBLIC ***

    @Override
    public void angularAccelerationInc(double angularAcc) {
        PhysicsValuesDTO old = this.getPhysicsValues();
        
        // Update nextPhyValues instead of creating new DTO
        nextPhyValues.update(
                old.timeStamp,
                old.posX, old.posY, old.angle,
                old.size,
                old.speedX, old.speedY,
                old.accX, old.accY,
                old.angularSpeed,
                old.angularAcc + angularAcc,
                old.thrust);
        
        this.setPhysicsValues(nextPhyValues);
    }

    @Override
    public PhysicsValuesDTO calcNewPhysicsValues() {
        long dtStart = this.profiler.startInterval();
        PhysicsValuesDTO phyVals = this.getPhysicsValues();
        long now = nanoTime();
        long elapsedNanos = now - phyVals.timeStamp;
        double dt = ((double) elapsedNanos) / 1_000_000_000.0d; // Nanos to seconds

        // ✅ Protección contra valores anómalos
        if (dt <= 0.0) {
            System.err.println("WARNING: Negative dt detected:  " + dt + "s.  Using 0.001s");
        } else if (dt > 0.5) {
            System.err.println("WARNING: Large dt detected: " + dt + "s. Clamping to 0.5s");
        }
        this.profiler.stopInterval("PHYSICS_DT", dtStart);

        return integrateMRUA(phyVals, dt);
    }

    @Override
    public boolean isThrusting() {
        PhysicsValuesDTO phyValues = this.getPhysicsValues();
        return phyValues.thrust != 0.0d;
    }

    // region Rebounds
    @Override
    public void reboundInEast(PhysicsValuesDTO phyValues,
            double worldDim_x, double worldDim_y) {

        // New speed: horizontal component flipped, vertical preserved
        double speedX = -phyValues.speedX;
        double speedY = phyValues.speedY;

        // New position: snapped to the east boundary (slightly inside)
        double posX = 0.0001d;
        double posY = phyValues.posY;
        double angle = phyValues.angle;

        // Acceleration is preserved
        double accX = phyValues.accX;
        double accY = phyValues.accY;

        // Update nextPhyValues instead of creating new DTO
        nextPhyValues.update(
                phyValues.timeStamp,
                posX, posY, angle,
                phyValues.size,
                speedX, speedY,
                accX, accY,
                phyValues.angularSpeed, phyValues.angularSpeed,
                phyValues.thrust);

        this.setPhysicsValues(nextPhyValues);
    }

    @Override
    public void reboundInWest(PhysicsValuesDTO phyValues,
            double worldDim_x, double worldDim_y) {

        // New speed: horizontal component flipped, vertical preserved
        double speedX = -phyValues.speedX;
        double speedY = phyValues.speedY;

        // New position: snapped to the east boundary (slightly inside)
        double posX = worldDim_x - 0.0001;
        double posY = phyValues.posY;
        double angle = phyValues.angle;

        // Acceleration is preserved
        double accX = phyValues.accX;
        double accY = phyValues.accY;

        // Update nextPhyValues instead of creating new DTO
        nextPhyValues.update(
                phyValues.timeStamp,
                posX, posY, angle,
                phyValues.size,
                speedX, speedY,
                accX, accY,
                phyValues.angularSpeed, phyValues.angularSpeed,
                phyValues.thrust);

        this.setPhysicsValues(nextPhyValues);
    }

    @Override
    public void reboundInNorth(PhysicsValuesDTO phyValues, double worldDim_x, double worldDim_y) {

        // New speed: horizontal component flipped, vertical preserved
        double speedX = phyValues.speedX;
        double speedY = -phyValues.speedY;

        // New position: snapped to the east boundary (slightly inside)
        double posX = phyValues.posX;
        double posY = 0.0001;
        double angle = phyValues.angle;

        // Acceleration is preserved
        double accX = phyValues.accX;
        double accY = phyValues.accY;

        // Update nextPhyValues instead of creating new DTO
        nextPhyValues.update(
                phyValues.timeStamp,
                posX, posY, angle,
                phyValues.size,
                speedX, speedY,
                accX, accY,
                phyValues.angularSpeed, phyValues.angularSpeed,
                phyValues.thrust);

        this.setPhysicsValues(nextPhyValues);
    }

    @Override
    public void reboundInSouth(PhysicsValuesDTO phyValues, double worldDim_x, double worldDim_y) {

        // New speed: horizontal component flipped, vertical preserved
        double speedX = phyValues.speedX;
        double speedY = -phyValues.speedY;

        // New position: snapped to the east boundary (slightly inside)
        double posX = phyValues.posX;
        double posY = worldDim_y - 0.0001;
        double angle = phyValues.angle;

        // Acceleration is preserved
        double accX = phyValues.accX;
        double accY = phyValues.accY;

        // Update nextPhyValues instead of creating new DTO
        nextPhyValues.update(
                phyValues.timeStamp,
                posX, posY, angle,
                phyValues.size,
                speedX, speedY,
                accX, accY,
                phyValues.angularSpeed, phyValues.angularSpeed,
                phyValues.thrust);

        this.setPhysicsValues(nextPhyValues);
    }
    // endregion

    @Override
    public void setAngularSpeed(double angularSpeed) {
        PhysicsValuesDTO old = this.getPhysicsValues();
        
        // Update nextPhyValues instead of creating new DTO
        nextPhyValues.update(
                old.timeStamp,
                old.posX, old.posY, old.angle,
                old.size,
                old.speedX, old.speedY,
                old.accX, old.accY,
                angularSpeed,
                old.angularAcc,
                old.thrust);
        
        this.setPhysicsValues(nextPhyValues);
    }

    // *** PRIVATES ***

    private PhysicsValuesDTO integrateMRUA(PhysicsValuesDTO phyVals, double dt) {
        // Applying thrust according actual angle OR using direct acceleration
        long thrustStart = this.profiler.startInterval();
        double accX = phyVals.accX; // Start with direct acceleration from DTO
        double accY = phyVals.accY;
        
        // If thrust is active, add angle-based acceleration
        if (phyVals.thrust != 0.0d) {
            double angleRad = Math.toRadians(phyVals.angle);
            accX += Math.cos(angleRad) * phyVals.thrust;
            accY += Math.sin(angleRad) * phyVals.thrust;
        }
        this.profiler.stopInterval("PHYSICS_THRUST", thrustStart);

        long linearStart = this.profiler.startInterval();
        
        // Apply different damping based on body type:
        // - PROJECTILE: Very low friction (maintain velocity)
        // - PLAYER/DYNAMIC: High friction for responsive control
        boolean isAccelerating = (accX != 0.0d || accY != 0.0d);
        double dampingFactor;
        
        if (this.bodyType == BodyType.PROJECTILE) {
            // Projectiles: minimal friction, maintain velocity
            dampingFactor = Math.pow(0.98, dt);
        } else {
            // Player/Dynamics: high friction for tight control
            dampingFactor = isAccelerating ? Math.pow(0.35, dt) : Math.pow(0.0001, dt);
        }
        
        double dampedSpeedX = phyVals.speedX * dampingFactor;
        double dampedSpeedY = phyVals.speedY * dampingFactor;
        
        // v1 = v0 + a*dt
        double oldSpeedX = dampedSpeedX;
        double oldSpeedY = dampedSpeedY;
        double newSpeedX = oldSpeedX + accX * dt;
        double newSpeedY = oldSpeedY + accY * dt;

        // avg_speed = (v0 + v1) / 2
        double avgSpeedX = (oldSpeedX + newSpeedX) * 0.5;
        double avgSpeedY = (oldSpeedY + newSpeedY) * 0.5;

        // x1 = x0 + v_avg * dt
        double newPosX = phyVals.posX + avgSpeedX * dt;
        double newPosY = phyVals.posY + avgSpeedY * dt;
        this.profiler.stopInterval("PHYSICS_LINEAR", linearStart);

        long angularStart = this.profiler.startInterval();
        // w1 = w0 + α*dt
        double newAngularSpeed = phyVals.angularSpeed + phyVals.angularAcc * dt;

        // θ1 = θ0 + w0*dt + 0.5*α*dt^2
        double newAngle = (phyVals.angle
                + phyVals.angularSpeed * dt
                + 0.5d * newAngularSpeed * dt * dt) % 360;
        this.profiler.stopInterval("PHYSICS_ANGULAR", angularStart);

        long dtoStart = this.profiler.startInterval();
        long newTimeStamp = phyVals.timeStamp + (long) (dt * 1_000_000_000.0d);

        // Update nextPhyValues instead of creating new DTO
        nextPhyValues.update(
                newTimeStamp,
                newPosX, newPosY, newAngle,
                phyVals.size,
                newSpeedX, newSpeedY,
                accX, accY, // only for information and debugging
                newAngularSpeed,
                phyVals.angularAcc, // keep same angular acc
                phyVals.thrust // keep same thrust
        );
        this.profiler.stopInterval("PHYSICS_DTO", dtoStart);

        return nextPhyValues;
    }
}
