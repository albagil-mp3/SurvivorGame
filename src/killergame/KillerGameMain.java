package killergame;

import engine.controller.impl.Controller;
import engine.controller.ports.ActionsGenerator;
import engine.model.impl.Model;
import engine.utils.helpers.DoubleVector;
import engine.view.core.View;
import engine.world.ports.WorldDefinition;
import engine.world.ports.WorldDefinitionProvider;
import gameworld.ProjectAssets;
import gameworld.Theme;

/**
 * Main entry point for Killer Game.
 * 
 * A simple room-based killer game where:
 * - Player is a blue triangle controlled with WASD
 * - Enemies are red circles that spawn from the edges
 * - Goal: Survive as long as possible
 */
public class KillerGameMain {

    public static void main(String[] args) {

        // region Graphics configuration
        System.setProperty("sun.java2d.uiScale", "1.0");
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.d3d", "false"); // OpenGL
        // endregion

        // region Game dimensions
        // Maze size - square world for Pacman-style gameplay
        DoubleVector viewDimension = new DoubleVector(800, 800);    // Camera viewport (what you see)
        DoubleVector worldDimension = new DoubleVector(2400, 2400); // Full maze world (much larger)
        // endregion

        // region Game configuration
        int maxBodies = 20000; // Max number of entities (player + enemies + walls) - maze has thousands of walls!
        int maxEnemySpawnDelay = 2000; // Spawn delay in milliseconds (2000ms = 2 seconds)
        // endregion

        // *** ASSETS ***
        // Allow selecting theme via first command-line argument (SPACE or JUNGLE)
        Theme resolvedTheme = Theme.JUNGLE;
        if (args != null && args.length > 0) {
            try {
                resolvedTheme = Theme.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException ex) {
                System.out.println("Unknown theme '" + args[0] + "', defaulting to SPACE.");
            }
        } else {
            // No arg provided -> pick a random theme at startup
            Theme[] themes = Theme.values();
            int idx = java.util.concurrent.ThreadLocalRandom.current().nextInt(themes.length);
            resolvedTheme = themes[idx];
        }

        final Theme selectedTheme = resolvedTheme;

        System.out.println("Selected theme: " + selectedTheme);
        ProjectAssets projectAssets = new ProjectAssets(selectedTheme);

        // *** WORLD DEFINITION PROVIDER ***
        WorldDefinitionProvider worldProv = new KillerWorldDefinitionProvider(
                worldDimension, projectAssets);

        // *** CORE ENGINE ***

        // region Model - Create model first so KillerGameRules can reference it
        Model model = new Model(worldDimension, maxBodies);
        // endregion

        // *** GAME RULES (created after Model so it can receive the reference) ***
        // KillerGameRules needs Model to award score on kills
        ActionsGenerator gameRules = new KillerGameRules(model);

        // region Controller
        View view = new View();

        Controller controller = new Controller(
            worldDimension,
            viewDimension,
            maxBodies,
            view,
            model,  // Pass the model reference
            gameRules);

        controller.activate();
        // endregion

        // *** SCENE ***

        // region World definition
        WorldDefinition worldDef = worldProv.provide();
        // endregion

        // region Level generator
        KillerLevelGenerator levelGenerator = new KillerLevelGenerator(controller, worldDef);
        // endregion

        // region Maze Navigation - Create navigator for AI pathfinding
        MazeNavigator mazeNavigator = levelGenerator.createMazeNavigator();
        // endregion

        // region Maze AI Controller - Manages enemy navigation
        MazeAIController mazeAI = new MazeAIController(model, mazeNavigator);
        mazeAI.activate();
        System.out.println("[MAIN] === Game initialization complete! ===\n");
        // endregion

        // region AI generator - Enemy spawner
        new KillerEnemySpawner(controller, worldDef, maxEnemySpawnDelay, mazeNavigator).activate();
        // endregion

        // Start 10-second game timer. When it finishes, stop the engine (game over)
        gameworld.GameTimer.get().start(10_000L, () -> {
            System.out.println("[TIMER] Time up! Game over.");

            // Determine final score from local player (if available) and set GameState
            try {
                String localPlayerId = view.getLocalPlayerId();
                int finalScore = 0;
                if (localPlayerId != null && !localPlayerId.isEmpty()) {
                    engine.view.renderables.ports.PlayerRenderDTO p = controller.getPlayerRenderData(localPlayerId);
                    if (p != null) finalScore = p.score;
                }

                gameworld.GameState.get().setFinalScore(finalScore);
                gameworld.GameState.get().setGameOver(true);
            } catch (Throwable t) {
                t.printStackTrace();
            }

            // Show Play Again button. When clicked, stop the current engine and relaunch main.
            view.showPlayAgainButton(() -> {
                try {
                    controller.engineStop();
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }

                // Small pause to allow threads to terminate gracefully
                try { Thread.sleep(500L); } catch (InterruptedException e) { /* ignore */ }

                // Relaunch the game in the same JVM by calling main again with the
                // previously selected theme. This approach works for quick reload
                // during development; if you need a full process restart consider
                // launching a new JVM process instead.
                try {
                    KillerGameMain.main(new String[] { selectedTheme.name() });
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            });
        });
    }
}
