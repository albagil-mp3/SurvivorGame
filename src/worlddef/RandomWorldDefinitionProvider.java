package worlddef;

import assets.impl.ProjectAssets;
import assets.ports.AssetType;
import engine.model.bodies.ports.BodyType;
import engine.utils.helpers.DoubleVector;
import engine.world.core.AbstractWorldDefinitionProvider;

public final class RandomWorldDefinitionProvider extends AbstractWorldDefinitionProvider {

	// *** CONSTRUCTORS ***

	public RandomWorldDefinitionProvider(DoubleVector worldDimension, ProjectAssets assets) {
		super(worldDimension, assets);
	}

	// *** PROTECTED (alphabetical order) ***

	@Override
	protected void define() {
		double density = 100d;

		// region Background
		this.setBackgroundStatic("back_24");
		// endregion

		// region Decorations
		this.addDecoratorAnywhereRandomAsset(50, AssetType.STARS, density, 200, 400);
		this.addDecorator("rainbow_01", 7000, 5000, 3000);
		this.addDecorator("stars_07", 7000, 15000, 1000);
		this.addDecoratorAnywhereRandomAsset(5, AssetType.GALAXY, density, 100, 300);
		this.addDecoratorAnywhereRandomAsset(10, AssetType.GALAXY, density, 50, 200);
		this.addDecoratorAnywhereRandomAsset(10, AssetType.HALO, density, 50, 200);
		// endregion

		// region Gravity bodies => Static bodies
		this.addGravityBody("planet_04", 4500, 4500, 1000);
		this.addGravityBody("cosmic_portal_01", 300, 1100, 400);
		this.addGravityBody("sun_02", 34000, 2000, 2000);
		this.addGravityBody("lab_01", 3000, 24000, 400);
		this.addGravityBody("black_hole_01", 26000, 30000, 600);
		this.addGravityBody("black_hole_02", 22000, 9000, 300);

		this.addGravityBodyAnywhereRandomAsset(10, AssetType.PLANET, density, 50, 300);
		this.addGravityBodyAnywhereRandomAsset(10, AssetType.MOON, density, 100, 500);
		this.addGravityBodyAnywhereRandomAsset(10, AssetType.MINE, density, 50, 100);
		// endregion

		// region Dynamic bodies
		this.addAsteroidPrototypeAnywhereRandomAsset(
				6, AssetType.ASTEROID,
				10, 25,
				10, 750,
				0, 150);
		// endregion

		// region Players
		this.addSpaceshipRandomAsset(1, AssetType.SPACESHIP, density, 50, 55, 300, 300);
		this.addTrailEmitterCosmetic("stars_06", 200, BodyType.DECORATOR, 20);
		// endregion

		// region Weapons (addWeapon***)
		this.addWeaponPresetBulletRandomAsset(AssetType.BULLET);

		this.addWeaponPresetBurstRandomAsset(AssetType.BULLET);

		this.addWeaponPresetMineLauncherRandomAsset(AssetType.MINE);

		this.addWeaponPresetMissileLauncherRandomAsset(AssetType.MISSILE);
		// endregion
	}
}
