package world.implementations;

import assets.implementations.ProjectAssets;
import assets.ports.AssetType;
import model.bodies.ports.BodyType;
import world.core.AbstractWorldDefinitionProvider;

public final class EarthInCenterWorldDefinitionProvider extends AbstractWorldDefinitionProvider {

    // *** CONSTRUCTORS ***

    public EarthInCenterWorldDefinitionProvider(double worldWidth, double worldHeight, ProjectAssets assets) {
        super(worldWidth, worldHeight, assets);
    }

    // *** PROTECTED (alphabetical order) ***

    @Override
    protected void define() {

        this.setBackgroundStatic("back_3");

        // region Statics
        this.addGravityBody("stars_2", 2100, 300, 600);

        this.addGravityBody("planet_4", worldWidth / 2.0, worldHeight / 2.0, 500);
        // endregion

        // region Dynamic bodies
        this.addAsteroidPrototypeAnywhereRandomAsset(
                6, AssetType.ASTEROID,
                1, 40,
                10, 175,
                0, 150);
        // endregion

        // region Players
        this.addSpaceshipPrototypeAnywhereRandomAsset(
                1, AssetType.SPACESHIP, 1, 60, 120);

        this.addTrailEmitterCosmetic("stars_6", 100.0, BodyType.DECORATOR, 100.0);
        // endregion

        // region Weapons
        this.addWeaponPresetBulletRandomAsset(AssetType.BULLET);

        this.addWeaponPresetBurstRandomAsset(AssetType.BULLET);

        this.addWeaponPresetMineLauncherRandomAsset(AssetType.MINE);

        this.addWeaponPresetMissileLauncherRandomAsset(AssetType.MISSILE);
        // endregion
    }
}
