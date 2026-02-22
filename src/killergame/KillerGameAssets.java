package killergame;

import engine.assets.core.AssetCatalog;
import engine.assets.ports.AssetIntensity;
import engine.assets.ports.AssetType;

/**
 * Asset catalog for Killer Game.
 * Defines simple geometric shapes for player and enemies.
 */
public final class KillerGameAssets {

    public final AssetCatalog catalog;

    public KillerGameAssets() {
        this.catalog = new AssetCatalog("src/resources/images/");

        // Background - usando uno existente de los assets del proyecto
        this.catalog.register("killer_back", "bg-01-space-seamless.jpg", AssetType.BACKGROUND, AssetIntensity.LOW);

        // Player - triángulo azul (usaremos un spaceship existente)
        this.catalog.register("player_triangle", "spaceship-01.png", AssetType.SPACESHIP, AssetIntensity.MEDIUM);

        // Enemies - círculos rojos (usaremos asteroides como placeholder)
        this.catalog.register("enemy_circle", "asteroid-01-mini.png", AssetType.ASTEROID, AssetIntensity.HIGH);
    }
}
