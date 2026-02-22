package killergame;

import engine.controller.impl.Controller;
import engine.controller.ports.ActionsGenerator;
import engine.model.impl.Model;
import engine.utils.helpers.DoubleVector;
import engine.view.core.View;
import engine.world.ports.WorldDefinition;
import engine.world.ports.WorldDefinitionProvider;
import gameworld.ProjectAssets;

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
        DoubleVector viewDimension = new DoubleVector(1000, 1000);
        DoubleVector worldDimension = new DoubleVector(1000, 1000); // Square maze
        // endregion

        // region Game configuration
        int maxBodies = 150; // Max number of entities (player + enemies + walls)
        int maxEnemySpawnDelay = 3; // Spawn delay in seconds for enemies (slower spawn)
        // endregion

        // *** ASSETS ***
        ProjectAssets projectAssets = new ProjectAssets();

        // *** GAME RULES ***
        ActionsGenerator gameRules = new KillerGameRules();

        // *** WORLD DEFINITION PROVIDER ***
        WorldDefinitionProvider worldProv = new KillerWorldDefinitionProvider(
                worldDimension, projectAssets);

        // *** CORE ENGINE ***

        // region Controller
        Controller controller = new Controller(
                worldDimension,
                viewDimension,
                maxBodies,
                new View(),
                new Model(worldDimension, maxBodies),
                gameRules);

        controller.activate();
        // endregion

        // *** SCENE ***

        // region World definition
        WorldDefinition worldDef = worldProv.provide();
        // endregion

        // region Level generator
        new KillerLevelGenerator(controller, worldDef);
        // endregion

        // region AI generator - DISABLED for now
        // new KillerEnemySpawner(controller, worldDef, maxEnemySpawnDelay).activate();
        // endregion
    }
}
