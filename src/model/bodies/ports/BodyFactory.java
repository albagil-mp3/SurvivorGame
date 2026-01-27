package model.bodies.ports;

// region Fields
import model.bodies.core.AbstractBody;
import model.bodies.implementations.DynamicBody;
import model.bodies.implementations.PlayerBody;
import model.bodies.implementations.ProjectileBody;
import model.bodies.implementations.StaticBody;
import model.physics.implementations.BasicPhysicsEngine;
import model.physics.ports.PhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;
import model.spatial.core.SpatialGrid;
// endregion

public class BodyFactory {


    
    public static AbstractBody create(
            BodyEventProcessor bodyEventProcessor,
            SpatialGrid spatialGrid,
            PhysicsValuesDTO phyVals,
            BodyType bodyType,
            double maxLifeTime,
            String shooterId) {

        AbstractBody body = null;
        PhysicsEngine phyEngine = null;

        switch (bodyType) {
            case DYNAMIC:
                phyEngine = new BasicPhysicsEngine(phyVals);
                body = new DynamicBody(
                        bodyEventProcessor, spatialGrid, phyEngine,
                        BodyType.DYNAMIC,
                        maxLifeTime);
                break;

            case PLAYER:
                phyEngine = new BasicPhysicsEngine(phyVals);
                body = new PlayerBody(
                        bodyEventProcessor, spatialGrid, phyEngine,
                        maxLifeTime);
                break;

            case PROJECTILE:
                phyEngine = new BasicPhysicsEngine(phyVals);
                body = new ProjectileBody(
                        bodyEventProcessor, spatialGrid, phyEngine,
                        maxLifeTime,
                        shooterId);
                break;

            case DECORATOR:
                body = new StaticBody(
                        bodyEventProcessor, null, bodyType,
                        phyVals.size, phyVals.posX, phyVals.posY, phyVals.angle,
                        maxLifeTime);
                break;

            case GRAVITY:
                body = new StaticBody(
                        bodyEventProcessor, spatialGrid, bodyType,
                        phyVals.size, phyVals.posX, phyVals.posY, phyVals.angle,
                        maxLifeTime);

                break;

            default:
                break;
        }

        return body;
    }

}
