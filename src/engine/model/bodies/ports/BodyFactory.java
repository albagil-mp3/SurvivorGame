package engine.model.bodies.ports;

import engine.model.bodies.core.AbstractBody;
import engine.model.bodies.impl.DynamicBody;
import engine.model.bodies.impl.PlayerBody;
import engine.model.bodies.impl.StaticBody;
import engine.model.physics.implementations.BasicPhysicsEngine;
import engine.model.physics.ports.PhysicsEngine;
import engine.model.physics.ports.PhysicsValuesDTO;
import engine.utils.spatial.core.SpatialGrid;

public class BodyFactory {

    public static AbstractBody create(
            BodyEventProcessor bodyEventProcessor,
            SpatialGrid spatialGrid,
            PhysicsValuesDTO phyVals,
            BodyType bodyType,
            double maxLifeTime,
            String emitterId) {

        AbstractBody body = null;
        PhysicsEngine phyEngine = null;

        switch (bodyType) {
            case DYNAMIC:
                phyEngine = new BasicPhysicsEngine(phyVals);
                body = new DynamicBody(
                        bodyEventProcessor, spatialGrid, phyEngine,
                        BodyType.DYNAMIC,
                        maxLifeTime, null);
                break;

            case PLAYER:
                phyEngine = new BasicPhysicsEngine(phyVals);
                body = new PlayerBody(
                        bodyEventProcessor, spatialGrid, phyEngine,
                        maxLifeTime, null);
                break;

            case PROJECTILE:
                phyEngine = new BasicPhysicsEngine(phyVals);
                body = new DynamicBody(
                        bodyEventProcessor, 
                        spatialGrid, 
                        phyEngine,
                        BodyType.PROJECTILE,
                        maxLifeTime,
                        emitterId);
                break;

            case DECORATOR:
                body = new StaticBody(
                        bodyEventProcessor, null, bodyType,
                        phyVals.size, phyVals.posX, phyVals.posY, phyVals.angle,
                        maxLifeTime, null);
                break;

            case GRAVITY:
                body = new StaticBody(
                        bodyEventProcessor, spatialGrid, bodyType,
                        phyVals.size, phyVals.posX, phyVals.posY, phyVals.angle,
                        maxLifeTime, null);

                break;

            default:
                break;
        }

        return body;
    }

}
