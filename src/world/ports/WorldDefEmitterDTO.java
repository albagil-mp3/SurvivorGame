package world.ports;

import model.bodies.ports.BodyType;

public class WorldDefEmitterDTO {

    // region Fields
    public final String bodyAssetId;
    public final double bodyAngularAcceleration;
    public final double bodyAngularSpeed;
    public final boolean bodyAddEmitterSpeedOnHeading;
    public final double bodyThrust;
    public final double bodyThrustDuration;
    public final double bodyMass;
    public final double bodyMaxLifetime;
    public final double bodySize;
    public final double bodyInitialSpeed;
    public final BodyType bodyType;

    public final double burstEmissionRate;
    public final int burstSize;
    public final double emissionRate;
    public final int maxBodiesEmitted;
    public final double emitterOffsetHorizontal;
    public final double emitterOffsetVertical;
    public final boolean randomizeInitialAngle;
    public final boolean randomizeSize;
    public final double emitterReloadTime;
    // endregion

    // *** CONSTRUCTOR ***

    public WorldDefEmitterDTO(
            String bodyAssetId,
            double bodyAngularAcceleration,
            double bodyAngularSpeed,
            boolean bodyAddEmitterSpeedOnHeading,
            double bodyThrust,
            double bodyThrustDuration,
            double bodyMass,
            double bodyMaxLifetime,
            double bodySize,
            double bodyInitialSpeed,
            BodyType bodyType,

            double burstEmissionRate,
            int burstSize,
            double emissionRate,
            double emitterOffsetHorizontal,
            double emitterOffsetVertical,
            double emitterReloadTime,
            int maxBodiesEmitted,
            boolean randomizeInitialAngle,
            boolean randomizeSize) {

        this.bodyAddEmitterSpeedOnHeading = bodyAddEmitterSpeedOnHeading;
        this.bodyAngularAcceleration = bodyAngularAcceleration;
        this.bodyAngularSpeed = bodyAngularSpeed;
        this.bodyAssetId = bodyAssetId;
        this.bodyInitialSpeed = bodyInitialSpeed;
        this.bodyMass = bodyMass;
        this.bodyMaxLifetime = bodyMaxLifetime;
        this.bodySize = bodySize;
        this.bodyThrust = bodyThrust;
        this.bodyThrustDuration = bodyThrustDuration;
        this.bodyType = bodyType;
        this.burstEmissionRate = burstEmissionRate;
        this.burstSize = burstSize;
        this.emissionRate = emissionRate;
        this.emitterOffsetHorizontal = emitterOffsetHorizontal;
        this.emitterOffsetVertical = emitterOffsetVertical;
        this.emitterReloadTime = emitterReloadTime;
        this.maxBodiesEmitted = maxBodiesEmitted;
        this.randomizeInitialAngle = randomizeInitialAngle;
        this.randomizeSize = randomizeSize;
    }
}
