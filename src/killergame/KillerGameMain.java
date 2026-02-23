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
        // endregion

        // region AI generator - Enemy spawner
        KillerEnemySpawner spawner = new KillerEnemySpawner(controller, worldDef, maxEnemySpawnDelay, mazeNavigator);
        // endregion

        boolean[] started = new boolean[] { false };
        GameMenuPanel menuPanel = new GameMenuPanel(() -> {
            if (started[0]) {
                return;
            }
            started[0] = true;
            view.showGame();
            controller.activate();
            mazeAI.activate();
            spawner.activate();
            System.out.println("[MAIN] === Game initialization complete! ===\n");
        });

        view.setMenuPanel(menuPanel);
        view.showMenu();
    }
}
