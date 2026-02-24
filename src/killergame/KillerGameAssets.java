package killergame;

import java.util.Arrays;

import engine.assets.core.AssetCatalog;
import engine.assets.ports.AnimatedAssetInfoDTO;
import engine.assets.ports.AssetIntensity;
import engine.assets.ports.AssetType;
import killergame.KillerTheme;;

/**
 * Asset catalog for Killer Game.
 * Defines simple geometric shapes for player and enemies.
 */
public final class KillerGameAssets {

    public final AssetCatalog catalog;
    public KillerGameAssets() {
        this(KillerTheme.SPACE);
    }

    public KillerGameAssets(KillerTheme theme) {
        this.catalog = new AssetCatalog("src/resources/images/");
        this.registerCommonAssets();
        this.registerThemeAssets(theme);
    }

    private void registerCommonAssets() {
        // Background placeholders and common assets
        //this.catalog.register("killer_back", "bg-01-space-seamless.jpg", AssetType.BACKGROUND, AssetIntensity.LOW);

        // Enemies - circles
        this.catalog.register("enemy_circle", "asteroid-01-mini.png", AssetType.ASTEROID, AssetIntensity.HIGH);

        // Ship animation frames
        this.catalog.register("pac-man-a-1", "pac-man-a-1.png", AssetType.SPACESHIP, AssetIntensity.HIGH);
        this.catalog.register("pac-man-a-2", "pac-man-a-2.png", AssetType.SPACESHIP, AssetIntensity.HIGH);
        this.catalog.register("pac-man-a-3", "pac-man-a-3.png", AssetType.SPACESHIP, AssetIntensity.HIGH);

        this.catalog.register("bullet_01", "bullet-01.png", AssetType.BULLET, AssetIntensity.LOW);
        this.catalog.register("player_weapon", "ak-47.png", AssetType.WEAPON, AssetIntensity.LOW);

        // Register player spaceship animation (using spaceship frames)
        this.catalog.registerAnimation(new AnimatedAssetInfoDTO(
            "player_ship_animated",
            Arrays.asList(
                "pac-man-a-1", "pac-man-a-2", "pac-man-a-3", "pac-man-a-2"
            ),
            AssetType.SPACESHIP,
            AssetIntensity.HIGH,
            200
        ));
    }

    private void registerThemeAssets(KillerTheme theme) {
        switch (theme) {
            case JUNGLE -> {
                // Jungle theme: try to register jungle background; fall back to default files if not present
                this.catalog.register("theme_back", "jungle-floor.jpg", AssetType.BACKGROUND, AssetIntensity.LOW);
                // For now reuse player weapon visual; swap later if jungle-specific weapon added
                
            }
            case SPACE  -> {
                this.catalog.register("theme_back", "bg-13-space-seamless.jpg", AssetType.BACKGROUND, AssetIntensity.LOW);
            }
        }
    }
}
