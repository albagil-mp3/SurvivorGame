package world.ports;

import model.bodies.ports.BodyType;

public class WorldDefEmitterDTO {

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
    public final boolean randomAngle;
    public final boolean randomSize;
    public final int emisionRate;
    public final int maxBodiesEmitted;
    public final double burstEmissionRate;
    public final int burstSize;
    public final double reloadTime;
    public final double bodyMass;
    public final double maxLifeTime;
    public final boolean addEmitterSpeed;

    public WorldDefEmitterDTO(
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
            int emisionRate,
            boolean randomAngle,
            boolean randomSize,
            boolean addEmitterSpeed,
            int maxBodiesEmitted,
            double burstEmissionRate,
            int burstSize,
            double reloadTime,
            double bodyMass,
            double maxLifeTime) {

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
        this.randomAngle = randomAngle;
        this.randomSize = randomSize;
        this.addEmitterSpeed = addEmitterSpeed;
        this.emisionRate = emisionRate;
        this.maxBodiesEmitted = maxBodiesEmitted;
        this.burstEmissionRate = burstEmissionRate;
        this.burstSize = burstSize;
        this.reloadTime = reloadTime;
        this.bodyMass = bodyMass;
        this.maxLifeTime = maxLifeTime;
    }
}
