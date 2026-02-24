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
import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
        int maxBodies = 3000; // Max number of entities (player + enemies + walls)
        int maxEnemySpawnDelay = 1000; // Spawn delay in milliseconds (2000ms = 2 seconds)
        // endregion

        // *** ASSETS ***
        // Allow selecting theme via first command-line argument (SPACE or JUNGLE)
        Theme resolvedTheme = Theme.JUNGLE;
        if (args != null && args.length > 0) {
            try {
                resolvedTheme = Theme.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException ex) {
                // Silent: unknown theme, defaulting to SPACE.
            }
        } else {
            // No arg provided -> pick a random theme at startup
            Theme[] themes = Theme.values();
            int idx = java.util.concurrent.ThreadLocalRandom.current().nextInt(themes.length);
            resolvedTheme = themes[idx];
        }

        final Theme selectedTheme = resolvedTheme;

        // Silent: selected theme is set
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

        // Wire pause overlay handlers to perform reset and exit similar to PlayAgain
        final Controller controllerRefPause = controller;
        final Model modelRefPause = model;
        final View viewRefPause = view;
        final WorldDefinitionProvider worldProvRefPause = worldProv;
        final DoubleVector worldDimRefPause = worldDimension;
        final DoubleVector viewDimRefPause = viewDimension;
        final int maxBodiesRefPause = maxBodies;
        final int maxEnemySpawnDelayRefPause = maxEnemySpawnDelay;

        // Reset action removed: PauseOverlay has no REINICIAR button now.

        view.setPauseExitHandler(() -> SwingUtilities.invokeLater(() -> {
            try {
                // Pause the engine and show main menu
                controller.enginePause();
                view.setMenuLocked(true);
                view.showMenu();
                view.requestFocusInWindow();
                // reset transient game state
                gameworld.GameState.get().reset();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }));

        AtomicBoolean playRequested = new AtomicBoolean(false);
        AtomicReference<Runnable> startGameActionRef = new AtomicReference<>(null);

        Runnable onPlayRequested = () -> SwingUtilities.invokeLater(() -> {
            playRequested.set(true);
            Runnable action = startGameActionRef.get();
            if (action != null) {
                action.run();
            }
        });

        try {
            SwingUtilities.invokeAndWait(() -> {
                final GameMenuPanel menuPanel = new GameMenuPanel(onPlayRequested);
                menuPanel.updateHighscore();
                view.setMenuPanel(menuPanel);
                view.setMenuLocked(true);
                view.showMenu();

                // Keep a reference to menuPanel for later updates (used by lambda below)
                // Using final array to capture variable in lambdas
                final GameMenuPanel[] menuRef = new GameMenuPanel[] { menuPanel };

                // Update pause exit handler to refresh menu when showing it
                view.setPauseExitHandler(() -> SwingUtilities.invokeLater(() -> {
                    try {
                        controller.enginePause();
                        view.setMenuLocked(true);
                        // refresh highscore before showing
                        try { menuRef[0].updateHighscore(); } catch (Throwable t) {}
                        view.showMenu();
                        view.requestFocusInWindow();
                        gameworld.GameState.get().reset();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }));
            });
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize main menu", ex);
        }
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

        // Start app with world initialized but paused until user presses PLAY.
        controller.enginePause();

        AtomicBoolean gameplayStarted = new AtomicBoolean(false);

        Runnable startRoundTimer = () -> gameworld.GameTimer.get().start(120_000L, () -> {
            System.err.println("[TIMER] Time up! Game over.");

            // Determine final score from local player (if available) and set GameState
            try {
                String localPlayerId = view.getLocalPlayerId();
                int finalScore = 0;
                if (localPlayerId != null && !localPlayerId.isEmpty()) {
                    engine.view.renderables.ports.PlayerRenderDTO p = controller.getPlayerRenderData(localPlayerId);
                    if (p != null) finalScore = p.score;
                }

                // Check highscore and persist + notify if new record
                int previousHS = killergame.HighscoreStore.getHighscore();
                if (finalScore > previousHS) {
                    killergame.HighscoreStore.saveHighscore(finalScore);
                    final int fs = finalScore;
                    // show dialog on EDT
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        javax.swing.JOptionPane.showMessageDialog(view,
                                "\u00A1Nuevo r\u00E9cord! Has conseguido " + fs + " puntos",
                                "Nuevo r\u00E9cord",
                                javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    });
                }

                gameworld.GameState.get().setFinalScore(finalScore);
                gameworld.GameState.get().setGameOver(true);
            } catch (Throwable t) {
                t.printStackTrace();
            }

            // Show Play Again button. When clicked, reset the game in the same window.
                final Controller controllerRef = controller;
                final Model modelRef = model;
                final View viewRef = view;
                final WorldDefinitionProvider worldProvRef = worldProv;
                final DoubleVector worldDimRef = worldDimension;
                final DoubleVector viewDimRef = viewDimension;
                final int maxBodiesRef = maxBodies;
                final int maxEnemySpawnDelayRef = maxEnemySpawnDelay;

                view.showPlayAgainButton(() -> {
                    try {
                        // Stop current engine and model
                        controllerRef.engineStop();
                        try { modelRef.shutdown(); } catch (Throwable t) { t.printStackTrace(); }

                        // Clear renderables so previous visuals vanish
                        viewRef.hidePlayAgainButton();
                        viewRef.getLayeredPane().repaint();
                        viewRef.getLayeredPane().revalidate();
                        viewRef.repaint();

                        // Reset global game state
                        gameworld.GameState.get().reset();

                        // Construct fresh model + controller using same view (no new window)
                        Model newModel = new Model(worldDimRef, maxBodiesRef);
                        ActionsGenerator newGameRules = new KillerGameRules(newModel);
                        Controller newController = new Controller(
                                worldDimRef,
                                viewDimRef,
                                maxBodiesRef,
                                viewRef,
                                newModel,
                                newGameRules);

                        newController.activate();

                        // Rebuild scene
                        WorldDefinition newWorldDef = worldProvRef.provide();
                        KillerLevelGenerator newLevelGenerator = new KillerLevelGenerator(newController, newWorldDef);
                        MazeNavigator newMazeNavigator = newLevelGenerator.createMazeNavigator();

                        MazeAIController newMazeAI = new MazeAIController(newModel, newMazeNavigator);
                        newMazeAI.activate();

                        new KillerEnemySpawner(newController, newWorldDef, maxEnemySpawnDelayRef, newMazeNavigator).activate();

                        // Restart timer
                        gameworld.GameTimer.get().start(120_000L, () -> {
                            try {
                                String localPlayerId = viewRef.getLocalPlayerId();
                                int finalScore = 0;
                                if (localPlayerId != null && !localPlayerId.isEmpty()) {
                                    engine.view.renderables.ports.PlayerRenderDTO p = newController.getPlayerRenderData(localPlayerId);
                                    if (p != null) finalScore = p.score;
                                }

                                int previousHS = killergame.HighscoreStore.getHighscore();
                                if (finalScore > previousHS) {
                                    killergame.HighscoreStore.saveHighscore(finalScore);
                                    final int fs = finalScore;
                                    javax.swing.SwingUtilities.invokeLater(() -> {
                                        javax.swing.JOptionPane.showMessageDialog(viewRef,
                                                "\u00A1Nuevo r\u00E9cord! Has conseguido " + fs + " puntos",
                                                "Nuevo r\u00E9cord",
                                                javax.swing.JOptionPane.INFORMATION_MESSAGE);
                                    });
                                }

                                gameworld.GameState.get().setFinalScore(finalScore);
                                gameworld.GameState.get().setGameOver(true);
                            } catch (Throwable t) { t.printStackTrace(); }

                            // Show Play again button for next round
                            viewRef.showPlayAgainButton(null);
                        });

                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                });
        });

        Runnable startGameAction = () -> SwingUtilities.invokeLater(() -> {
            try {
                if (gameplayStarted.compareAndSet(false, true)) {
                    mazeAI.activate();
                    spawner.activate();
                    startRoundTimer.run();
                }

                controller.engineResume();
                view.setMenuLocked(false);
                view.showGame();
                view.requestFocusInWindow();
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        });

        startGameActionRef.set(startGameAction);
        if (playRequested.get()) {
            startGameAction.run();
        }
    }
}
