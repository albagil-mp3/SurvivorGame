package model.emitter.ports;

import model.bodies.ports.BodyType;

public class EmitterDto {

    public final BodyEmittedDTO bodyEmitted;
    public final int emisionRate;
    public final int maxBodiesEmitted;
    public final double reloadTime;

    public EmitterDto(
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
            boolean randomAngle,
            boolean randomSize,
            boolean addEmitterSpeed,
            int emisionRate,
            int maxBodiesEmitted,
            double reloadTime,
            double bodyMass,
            double maxLifeTime) {

                this.bodyEmitted = new BodyEmittedDTO(
                    type,
                    assetId,
                    size,
                    xOffset,
                    yOffset,
                    speed,
                    acceleration,
                    accelerationTime,
                    angularSpeed,
                    angularAcc,
                    thrust,
                    bodyMass,
                    maxLifeTime,
                    randomAngle,
                    randomSize,
                    addEmitterSpeed
                );

        this.emisionRate = emisionRate;
        this.maxBodiesEmitted = maxBodiesEmitted;
        this.reloadTime = reloadTime;
    }
}
