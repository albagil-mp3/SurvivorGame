package game.worlddef;

import assets.impl.ProjectAssets;
import assets.ports.AssetType;
import model.bodies.ports.BodyType;
import utils.helpers.DoubleVector;
import world.core.AbstractWorldDefinitionProvider;

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
		this.setBackgroundStatic("back_9");
		// endregion

		// region Decorations
		this.addDecoratorAnywhereRandomAsset(
				3, AssetType.STARS, density, 200, 400);
		// endregion

		// region Gravity bodies => Static bodies
		this.addGravityBodyAnywhereRandomAsset(
				2, AssetType.PLANET, density, 100, 600);

		this.addGravityBodyAnywhereRandomAsset(
				3, AssetType.GALAXY, density, 100, 400);

		this.addGravityBodyAnywhereRandomAsset(
				1, AssetType.COSMIC_PORTAL, density, 200, 400);

		this.addGravityBodyAnywhereRandomAsset(
				1, AssetType.LAB, density, 100, 200);

		this.addGravityBodyAnywhereRandomAsset(
				2, AssetType.HALO, density, 100, 400);

		this.addGravityBodyAnywhereRandomAsset(
				1, AssetType.RAINBOW, density, 2000, 4000);

		this.addGravityBodyAnywhereRandomAsset(
				2, AssetType.MOON, density, 100, 1000);

		this.addGravityBodyAnywhereRandomAsset(
				1, AssetType.SUN, density, 800, 1400);

		this.addGravityBodyAnywhereRandomAsset(
				10, AssetType.MINE, density, 50, 100);

		this.addGravityBodyAnywhereRandomAsset(
				1, AssetType.BLACK_HOLE, density, 100, 400);
		// endregion

		// region Dynamic bodies
		this.addAsteroidPrototypeAnywhereRandomAsset(
				6, AssetType.ASTEROID,
				6, 25,
				10, 175,
				0, 150);
		// endregion

		// region Players
		this.addSpaceshipRandomAsset(
				1, AssetType.SPACESHIP, density, 50, 60, 200, 200);

		this.addTrailEmitterCosmetic("stars_6", 100, BodyType.DECORATOR, 20);
		// endregion

		// region Weapons (addWeapon***)
		this.addWeaponPresetBulletRandomAsset(AssetType.BULLET);

		this.addWeaponPresetBurstRandomAsset(AssetType.BULLET);

		this.addWeaponPresetMineLauncherRandomAsset(AssetType.MINE);

		this.addWeaponPresetMissileLauncherRandomAsset(AssetType.MISSILE);
		// endregion
	}
}
