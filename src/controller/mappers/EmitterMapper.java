package controller.mappers;

import model.emitter.ports.EmitterConfigDto;
import world.ports.WorldDefEmitterDTO;

public class EmitterMapper {

    public static EmitterConfigDto fromWorldDef(
            WorldDefEmitterDTO emitterDef) {

        if (emitterDef == null) {
            return null;
        }
        return new EmitterConfigDto(
                emitterDef.bodyType,
                emitterDef.bodyAssetId,
                emitterDef.bodySize,
                emitterDef.emitterOffsetHorizontal,
                emitterDef.emitterOffsetVertical,
                emitterDef.bodyInitialSpeed,
                emitterDef.bodyAcceleration,
                emitterDef.bodyThrustDuration,
                emitterDef.bodyAngularSpeed,
                emitterDef.bodyAngularAcceleration,
                emitterDef.bodyThrust,
                emitterDef.randomizeInitialAngle,
                emitterDef.randomizeSize,
                emitterDef.addEmitterSpeedToBody,
                emitterDef.emissionRate,
                emitterDef.maxBodiesEmitted,
                emitterDef.burstEmissionRate,
                emitterDef.burstSize,
                emitterDef.emitterReloadTime,
                emitterDef.bodyMass,
                emitterDef.bodyMaxLifetime);
    }
}