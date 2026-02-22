package killergame;

import java.util.Arrays;

import engine.assets.core.AssetCatalog;
import engine.assets.ports.AnimatedAssetInfoDTO;
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

        // Enemies - círculos rojos (usaremos asteroides como placeholder)
        this.catalog.register("enemy_circle", "asteroid-01-mini.png", AssetType.ASTEROID, AssetIntensity.HIGH);
         
        this.catalog.register("player_weapon", "ak-47.png", AssetType.WEAPON, AssetIntensity.LOW);
        this.catalog.register("pac-man-a-1", "pac-man-a-1.png", AssetType.SPACESHIP, AssetIntensity.HIGH);
        this.catalog.register("pac-man-a-2", "pac-man-a-2.png", AssetType.SPACESHIP, AssetIntensity.HIGH);
        this.catalog.register("pac-man-a-3", "pac-man-a-3.png", AssetType.SPACESHIP, AssetIntensity.HIGH);

        // Register player spaceship animation (using spaceship frames 2-15)
        this.catalog.registerAnimation(new AnimatedAssetInfoDTO(
            "player_ship_animated",
            Arrays.asList(
                "pac-man-a-1", "pac-man-a-2", "pac-man-a-3","pac-man-a-2"
                
            ),
            AssetType.SPACESHIP,
            AssetIntensity.HIGH,
            200  // 100ms per frame = 10 FPS animation  
        ));
        // endregion

    }
}
