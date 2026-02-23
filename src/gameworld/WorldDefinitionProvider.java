package gameworld;

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
        // "theme_back" apunta al fondo del tema activo (SPACE o JUNGLE)
        this.setBackgroundStatic("theme_back");

        // region Players
        this.addSpaceship("player_ship_animated", 1250, 1250, 40);
        this.registerAsset("player_weapon");
        this.addWeaponPresetBullet("bullet_01");
        // endregion
    }
    
}
