package gameworld;

/**
 * Theme
 *
 * Define los temas visuales disponibles en el juego.
 * Cada tema mapea los asset IDs "temáticos" a archivos de imagen distintos.
 *
 * Para añadir un tema nuevo:
 *   1. Añade un valor aquí (DESERT, UNDERWATER, etc.)
 *   2. Añade su case en ProjectAssets.registerThemeAssets()
 *
 * Uso en Main.java:
 *   ProjectAssets assets = new ProjectAssets(Theme.JUNGLE);
 */
public enum Theme {
    SPACE,
    JUNGLE
}
