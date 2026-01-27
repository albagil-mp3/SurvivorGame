package game.implementations.world;

import model.bodies.ports.BodyType;
import utils.assets.implementations.ProjectAssets;
import utils.assets.ports.AssetType;
import world.core.AbstractWorldDefinitionProvider;

public final class RandomWorldDefinitionProvider extends AbstractWorldDefinitionProvider {

	// *** CONSTRUCTORS ***

	public RandomWorldDefinitionProvider(double worldWidth, double worldHeight, ProjectAssets assets) {
		super(worldWidth, worldHeight, assets);
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
				2, AssetType.STARS, density, 200, 600);
		// endregion

		// region Gravity bodies => Static bodies
		this.addGravityBodyAnywhereRandomAsset(
				1, AssetType.PLANET, density, 100, 200);

		this.addGravityBodyAnywhereRandomAsset(
				1, AssetType.MOON, density, 40, 80);

		this.addGravityBodyAnywhereRandomAsset(
				1, AssetType.SUN, density, 20, 40);

		this.addGravityBodyAnywhereRandomAsset(
				1, AssetType.BLACK_HOLE, density, 45, 55);
		// endregion

		// region Dynamic bodies
		this.addAsteroidPrototypeAnywhereRandomAsset(
				6, AssetType.ASTEROID,
				6, 25,
				10, 175,
				0, 150);
		// endregion

		// region Players
		this.addSpaceshipAnywhereRandomAsset(
				1, AssetType.SPACESHIP, density, 50, 50);

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
