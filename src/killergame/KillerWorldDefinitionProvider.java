package killergame;

import engine.assets.ports.AssetType;
import engine.model.bodies.ports.BodyType;
import engine.utils.helpers.DoubleVector;
import engine.world.core.AbstractWorldDefinitionProvider;
import gameworld.ProjectAssets;

/**
 * World definition for Killer Game.
 * Creates a simple room-based arena with player and enemy spawns.
 */
public final class KillerWorldDefinitionProvider extends AbstractWorldDefinitionProvider {

    // *** CONSTRUCTORS ***

    public KillerWorldDefinitionProvider(DoubleVector worldDimension, ProjectAssets assets) {
        super(worldDimension, assets);
    }

    // *** PROTECTED (alphabetical order) ***

    @Override
    protected void define() {

        // Background - simple background for the maze
        this.setBackgroundStatic("back_01");

        // Add a small decorator to satisfy WorldDefinition validation
        // This will be a tiny light in the corner, barely visible
        this.addDecorator("light_01", 50.0, 50.0, 10.0);
        
        // Register wall assets for LevelGenerator (walls are created there)
        // These assets must be registered here so they load into gameAssets
        this.registerAsset("wall_01");
        this.registerAsset("wall_02");
        
        System.out.println("[DEBUG] KillerWorldDefinitionProvider defining world...");
        
        // Enemies - Small circular enemies that fit in maze corridors
        // Size 30 (diameter) - small enough to navigate the maze
        // Spawned in the center (spawner will create more instances)
        this.addAsteroidRandomAsset(
                1,                      // Number of definitions (spawner will create more)
                AssetType.ASTEROID,     // Enemy type
                0.0,                    // Heading (will be randomized by spawner)
                1.0,                    // Density
                30.0,                   // Size 30 - small for maze navigation
                this.worldWidth / 2.0,  // Center X
                this.worldHeight / 2.0);// Center Y
        
        System.out.println("[DEBUG] Added " + this.asteroids.size() + " enemy definitions");

        // Player - spawns in the center of the maze
        this.addSpaceship("player_ship_animated", this.worldWidth / 2.0, this.worldHeight / 2.0, 35);
        this.registerAsset("player_weapon");
        this.addWeaponPresetBullet("bullet_01");
        
        
    }
}
