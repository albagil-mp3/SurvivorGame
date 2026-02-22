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
        this.registerAssetId("wall_01");
        this.registerAssetId("wall_02");
        
        // NO player - will be added later
        // NO enemies - will be added later
        // NO weapons - will be added later
        
        // Walls will be created directly in LevelGenerator using the wall assets
    }
}
