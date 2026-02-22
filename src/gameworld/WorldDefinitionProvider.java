package gameworld;

import engine.assets.ports.AssetType;
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
        //this.addSpaceship("spaceship_02", 19250, 19250, 55);
        this.addSpaceship("player_ship_animated", 19250, 19250, 55);
        this.registerAsset("player_weapon"); // weapon visual overlay - must be in Images
        this.addWeaponPresetBullet("bullet_01");

        
		// endregion
    }
    
}
