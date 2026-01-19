package model.emitter.ports;

import model.bodies.ports.BodyType;

public class BodyEmittedDTO {
   public final BodyType type;
    public final String assetId;
    public final double size;
    public final double xOffset;
    public final double yOffset;
    public final double speed;
    public final double acceleration;
    public final double accelerationTime;
    public final double angularSpeed;
    public final double angularAcc;
    public final double thrust;
    public final double bodyMass;
    public final double maxLifeTime;

    public final boolean randomAngle;
    public final boolean randomSize;
    public final boolean addEmitterSpeed;

    public BodyEmittedDTO(
            BodyType type,
            String assetId,
            double size,
            double xOffset,
            double yOffset,
            double speed,
            double acceleration,
            double accelerationTime,
            double angularSpeed,
            double angularAcc,
            double thrust,
            double bodyMass,
            double maxLifeTime,
            boolean randomAngle,
            boolean randomSize,
            boolean addEmitterSpeed) {

        this.type = type;
        this.assetId = assetId;
        this.size = size;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.speed = speed;
        this.acceleration = acceleration;
        this.accelerationTime = accelerationTime;
        this.angularSpeed = angularSpeed;
        this.angularAcc = angularAcc;
        this.thrust = thrust;
        this.bodyMass = bodyMass;
        this.maxLifeTime = maxLifeTime;
        this.randomAngle = randomAngle;
        this.randomSize = randomSize;
        this.addEmitterSpeed = addEmitterSpeed;
    }
}
