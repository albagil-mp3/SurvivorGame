package gameworld;

/**
 * GameState
 *
 * Singleton que guarda el estado global de la partida (nivel de mundo,
 * puntuación global, etc.).
 *
 * Accesible desde cualquier clase — incluyendo gameRules — sin inyección
 * de dependencias:
 *
 *   GameState.get().incrementWorldLevel();
 *   int nivel = GameState.get().getWorldLevel();
 */
public final class GameState {

    // *** SINGLETON ***
    private static final GameState INSTANCE = new GameState();

    public static GameState get() {
        return INSTANCE;
    }

    private GameState() {}

    // *** FIELDS ***
    private volatile int worldLevel = 1;
    private volatile boolean gameOver = false;
    private volatile int finalScore = 0;

    // *** PUBLICS ***

    public int getWorldLevel() {
        return worldLevel;
    }

    public void incrementWorldLevel() {
        worldLevel++;
    }

    public void setWorldLevel(int level) {
        if (level < 1) throw new IllegalArgumentException("level must be >= 1");
        worldLevel = level;
    }

    public void reset() {
        worldLevel = 1;
        gameOver = false;
        finalScore = 0;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public int getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(int score) {
        this.finalScore = score;
    }
}
