package gameworld;

import java.awt.Dimension;

import engine.assets.ports.AssetType;
import engine.model.bodies.ports.BodyType;
import engine.utils.helpers.DoubleVector;
import engine.world.core.AbstractWorldDefinitionProvider;

public class WorldDefinitionProvider extends AbstractWorldDefinitionProvider {

    // *** CONSTRUCTORS ***

    public WorldDefinitionProvider(DoubleVector worldDimension, ProjectAssets assets) {
        super(worldDimension, assets);
    }

    // *** PROTECTED (alphabetical order) ***

    @Override
    protected void define() {
        this.setBackgroundStatic("back_13");
        // region Players
        //this.addSpaceship("spaceship_3", 40, 10, 300, 300, 200);

		this.addSpaceshipRandomAsset(1, AssetType.SPACESHIP, 10, 10, 55, 19000, 19500);
        
		// endregion
    }
    
}
