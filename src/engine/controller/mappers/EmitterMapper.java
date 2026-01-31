package engine.controller.mappers;

import engine.model.emitter.ports.EmitterConfigDto;
import engine.world.ports.DefEmitterDTO;

public class EmitterMapper {

    public static EmitterConfigDto fromWorldDef(DefEmitterDTO emitterDef) {

        if (emitterDef == null) {
            return null;
        }

        return new EmitterConfigDto(
                emitterDef.bodyType,
                emitterDef.bodyAssetId,
                emitterDef.bodySize,

                // offsets (forward/side)
                emitterDef.emitterOffsetVertical,
                emitterDef.emitterOffsetHorizontal,

                // kinematics
                emitterDef.bodyInitialSpeed,

                // acceleration (deprecated in new world def) -> neutral
                0.0,

                // duration for propulsion
                emitterDef.bodyThrustDuration,

                emitterDef.bodyAngularSpeed,
                emitterDef.bodyAngularAcceleration,

                // thrust
                emitterDef.bodyThrust,

                emitterDef.randomizeInitialAngle,
                emitterDef.randomizeSize,

                emitterDef.bodyAddEmitterSpeedOnHeading,

                emitterDef.emissionRate,
                emitterDef.maxBodiesEmitted,
                emitterDef.burstEmissionRate,
                emitterDef.burstSize,
                emitterDef.emitterReloadTime,

                emitterDef.bodyMass,
                emitterDef.bodyMaxLifetime
        );
    }
}
