package model.emitter.ports;

import model.bodies.ports.BodyType;
import utils.events.domain.ports.BodyToEmitDTO;

public class EmitterConfigDto {

    public final BodyToEmitDTO bodyEmitted;
    public final double emisionRate;
    public final int maxBodiesEmitted;
    public final double burstEmissionRate;
    public final int burstSize;
    public final double reloadTime;

    public EmitterConfigDto(
            BodyType type,
            String assetId,
            double size,
            double fordwardOffset,
            double sideOffset,
            double speed,
            double acceleration,
            double accelerationTime,
            double angularSpeed,
            double angularAcc,
            double thrust,
            boolean randomAngle,
            boolean randomSize,
            boolean addEmitterSpeed,
            double emisionRate,
            int maxBodiesEmitted,
            double burstEmissionRate,
            int burstSize,
            double reloadTime,
            double bodyMass,
            double maxLifeTime) {

        this.bodyEmitted = new BodyToEmitDTO(
                type,
                assetId,
                size,
                fordwardOffset,
                sideOffset,
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
                addEmitterSpeed);

        this.emisionRate = emisionRate;
        this.maxBodiesEmitted = maxBodiesEmitted;
        this.burstEmissionRate = burstEmissionRate;
        this.burstSize = burstSize;
        this.reloadTime = reloadTime;

    }
}
