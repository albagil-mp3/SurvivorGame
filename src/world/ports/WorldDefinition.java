package world.ports;

import java.util.ArrayList;

import utils.assets.core.AssetCatalog;

public class WorldDefinition {

	// region Fields
	public final double worldWidth;
	public final double worldHeight;
	public final AssetCatalog gameAssets;

	public final DefBackgroundDTO background;

	// Entity polymorphic lists are grouped by type to simplify core consumption
	public final ArrayList<DefItem> spaceDecorators; 
	public final ArrayList<DefItem> gravityBodies; 
	public final ArrayList<DefItem> asteroids; 
	public final ArrayList<DefItem> spaceships; 
	public final ArrayList<DefEmitterDTO> trailEmitters;

	// Weapon lists are grouped by type to simplify core consumption
	public final ArrayList<DefWeaponDTO> bulletWeapons;
	public final ArrayList<DefWeaponDTO> burstWeapons;
	public final ArrayList<DefWeaponDTO> mineLaunchers;
	public final ArrayList<DefWeaponDTO> missileLaunchers;
	// endregion

	// *** CONSTRUCTOR ***

	public WorldDefinition(
			double worldWidth,
			double worldHeight,
			AssetCatalog gameAssets,
			DefBackgroundDTO background,
			ArrayList<DefItem> spaceDecorators,
			ArrayList<DefItem> gravityBodies,
			ArrayList<DefItem> asteroids,
			ArrayList<DefItem> spaceships,
			ArrayList<DefEmitterDTO> trailEmitters,
			ArrayList<DefWeaponDTO> bulletWeapons,
			ArrayList<DefWeaponDTO> burstWeapons,
			ArrayList<DefWeaponDTO> mineLaunchers,
			ArrayList<DefWeaponDTO> missileLaunchers) {

		this.worldWidth = worldWidth;
		this.worldHeight = worldHeight;
		this.gameAssets = gameAssets;
		this.background = background;
		this.spaceDecorators = spaceDecorators;
		this.gravityBodies = gravityBodies;
		this.asteroids = asteroids;
		this.bulletWeapons = bulletWeapons;
		this.burstWeapons = burstWeapons;
		this.mineLaunchers = mineLaunchers;
		this.missileLaunchers = missileLaunchers;
		this.spaceships = spaceships;
		this.trailEmitters = trailEmitters;
	}
}
