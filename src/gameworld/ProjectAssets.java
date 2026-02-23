package gameworld;

import java.util.Arrays;

import engine.assets.core.AssetCatalog;
import engine.assets.ports.AnimatedAssetInfoDTO;
import engine.assets.ports.AssetIntensity;
import engine.assets.ports.AssetType;

public final class ProjectAssets {

    public final AssetCatalog catalog;
    private final Theme theme;

    // Constructor por defecto: tema SPACE
    public ProjectAssets() {
        this(Theme.SPACE);
    }

    public ProjectAssets(Theme theme) {
        this.theme = theme;
        this.catalog = new AssetCatalog("src/resources/images/");
        this.registerCommonAssets();
        this.registerThemeAssets(theme);
    }

    public Theme getTheme() {
        return this.theme;
    }

    // *** PRIVATE ***

    /**
     * Registra los assets temáticos bajo IDs predecibles:
     *   "theme_back"   → fondo principal del mundo
     *   "theme_enemy"  → enemigo/asteroide principal del tema
     *
     * WorldDefinitionProvider usa estos IDs sin saber qué tema está activo.
     */
    private void registerThemeAssets(Theme theme) {
        switch (theme) {
            case JUNGLE -> {
                // Fondo de jungla
                this.catalog.register("theme_back",  "jungle-floor.jpg",   AssetType.BACKGROUND, AssetIntensity.LOW);
                this.catalog.register("wall_01", "wall-01.png", AssetType.STATIC, AssetIntensity.HIGH);
                this.catalog.register("wall_02", "wall-02.png", AssetType.STATIC, AssetIntensity.HIGH);
                this.catalog.register("stars_01", "dec1-jungle.png", AssetType.STARS, AssetIntensity.HIGH);
                this.catalog.register("stars_02", "dec2-jungle.png", AssetType.STARS, AssetIntensity.HIGH);
                this.catalog.register("stars_03", "dec3-jungle.png", AssetType.STARS, AssetIntensity.HIGH);




                // Enemigos/obstáculos de jungla  ← añade aquí tus PNGs de jungla
                // this.catalog.register("theme_enemy_01", "jungle-rock-01.png",  AssetType.ASTEROID, AssetIntensity.HIGH);
            }
            case SPACE -> {
                // Fondo de espacio (por defecto)
                this.catalog.register("theme_back",  "bg-13-space-seamless.jpg", AssetType.BACKGROUND, AssetIntensity.LOW);
                this.catalog.register("wall_01", "wall-space1.png", AssetType.STATIC, AssetIntensity.HIGH);
                this.catalog.register("wall_02", "wall-space2.png", AssetType.STATIC, AssetIntensity.HIGH);
                this.catalog.register("stars_01", "stars-01.png", AssetType.STARS, AssetIntensity.HIGH);
               
                this.catalog.register("stars_06", "stars-06.png", AssetType.STARS, AssetIntensity.HIGH);
                this.catalog.register("stars_07", "stars-07.png", AssetType.STARS, AssetIntensity.HIGH);
            }
        }
    }

    private void registerCommonAssets() {

        // region asteroids (asteroid-***)
        this.catalog.register("asteroid_01", "asteroid-01-mini.png", AssetType.ASTEROID, AssetIntensity.HIGH);
        this.catalog.register("asteroid_02", "asteroid-02-mini.png", AssetType.ASTEROID, AssetIntensity.HIGH);
        this.catalog.register("asteroid_03", "asteroid-03-mini.png", AssetType.ASTEROID, AssetIntensity.HIGH);
        this.catalog.register("asteroid_04", "asteroid-04-mini.png", AssetType.ASTEROID, AssetIntensity.HIGH);
        this.catalog.register("asteroid_05", "asteroid-05-mini.png", AssetType.ASTEROID, AssetIntensity.HIGH);
        this.catalog.register("asteroid_06", "asteroid-06-mini.png", AssetType.ASTEROID, AssetIntensity.HIGH);
        this.catalog.register("asteroid_07", "asteroid-07.png", AssetType.ASTEROID, AssetIntensity.HIGH);
        this.catalog.register("asteroid_08", "asteroid-08.png", AssetType.ASTEROID, AssetIntensity.HIGH);
        // endregion

        // region backgrounds (bg-***)
        // NOTA: el fondo del tema activo se registra como "theme_back" en registerThemeAssets()
        
        // region bullets (bullet-***)
        this.catalog.register("bullet_01", "bullet-01.png", AssetType.BULLET, AssetIntensity.LOW);
        // endregion

        // region walls (wall-***)
      
        this.catalog.register("wall_03", "wall-03.png", AssetType.STATIC, AssetIntensity.HIGH);
        this.catalog.register("wall_04", "wall-04.png", AssetType.STATIC, AssetIntensity.HIGH);
        // endregion

        // region bombs (bomb-***)
        this.catalog.register("bomb_01", "grenade-01.png", AssetType.MINE, AssetIntensity.MEDIUM);
        this.catalog.register("bomb_02", "grenade-02.png", AssetType.MINE, AssetIntensity.MEDIUM);
        // endregion

        // region cosmic portals (cosmic-portal-***)
        this.catalog.register("cosmic_portal_01", "cosmic-portal-01.png", AssetType.COSMIC_PORTAL, AssetIntensity.HIGH);
        // endregion

        // region lights (light-***)
        this.catalog.register("light_01", "light-01.png", AssetType.LIGHT, AssetIntensity.HIGH);
        this.catalog.register("light_02", "light-02.png", AssetType.LIGHT, AssetIntensity.HIGH);
        // endregion

        // region missiles (misil-***)
        this.catalog.register("misil_01", "misil-01-mini.png", AssetType.MISSILE, AssetIntensity.HIGH);
        this.catalog.register("misil_02", "misil-02-mini.png", AssetType.MISSILE, AssetIntensity.HIGH);
        this.catalog.register("misil_03", "misil-03-mini.png", AssetType.MISSILE, AssetIntensity.HIGH);
        this.catalog.register("misil_04", "misil-04.png", AssetType.MISSILE, AssetIntensity.HIGH);
        this.catalog.register("misil_05", "misil-05-mini.png", AssetType.MISSILE, AssetIntensity.HIGH);
        // endregion

        // region meteors (meteor-***)
        this.catalog.register("meteor_01", "meteor-01.png", AssetType.METEOR, AssetIntensity.HIGH);
        this.catalog.register("meteor_02", "meteor-02.png", AssetType.METEOR, AssetIntensity.HIGH);
        this.catalog.register("meteor_03", "meteor-03.png", AssetType.METEOR, AssetIntensity.HIGH);
        this.catalog.register("meteor_04", "meteor-04.png", AssetType.METEOR, AssetIntensity.HIGH);
        this.catalog.register("meteor_05", "meteor-05.png", AssetType.METEOR, AssetIntensity.HIGH);
        // endregion

     

        // region rainbows (rainbow-***)
        this.catalog.register("rainbow_01", "rainbow-01.png", AssetType.RAINBOW, AssetIntensity.HIGH);
        // endregion

        // region rockets (rocket-***)
        this.catalog.register("rocket_01", "rocket-01.png", AssetType.ROCKET, AssetIntensity.HIGH);
        this.catalog.register("rocket_02", "rocket-02.png", AssetType.ROCKET, AssetIntensity.HIGH);
        this.catalog.register("rocket_03", "rocket-03.png", AssetType.ROCKET, AssetIntensity.HIGH);
        this.catalog.register("rocket_04", "rocket-04.png", AssetType.ROCKET, AssetIntensity.HIGH);
        this.catalog.register("rocket_05", "rocket-05.png", AssetType.ROCKET, AssetIntensity.HIGH);
        this.catalog.register("rocket_06", "rocket-06.png", AssetType.ROCKET, AssetIntensity.HIGH);
        this.catalog.register("rocket_07", "rocket-07.png", AssetType.ROCKET, AssetIntensity.HIGH);
        // endregion

        // Player weapon visual overlay - AssetType.WEAPON solo para armas visuales
        this.catalog.register("player_weapon", "ak-47.png", AssetType.WEAPON, AssetIntensity.LOW);

        // Player animation frames (must exist as regular assets before animation playback)
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

        // region suns (sun-***)
        this.catalog.register("sun_01", "sun-01.png", AssetType.SUN, AssetIntensity.HIGH);
        this.catalog.register("sun_02", "sun-02.png", AssetType.SUN, AssetIntensity.HIGH);
        this.catalog.register("sun_03", "sun-03.png", AssetType.SUN, AssetIntensity.HIGH);
        this.catalog.register("sun_04", "sun-04.png", AssetType.SUN, AssetIntensity.HIGH);
        // endregion

        // region ui signs (ui-signs-***)
        this.catalog.register("signs_01", "ui-signs-1.png", AssetType.UI_SIGN, AssetIntensity.HIGH);
        // endregion

    }
}
