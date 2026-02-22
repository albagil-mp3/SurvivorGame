package killergame;

import java.util.ArrayList;
import java.util.Random;

import engine.controller.ports.WorldManager;
import engine.generators.AbstractIAGenerator;
import engine.world.ports.DefItem;
import engine.world.ports.DefItemDTO;
import engine.world.ports.WorldDefinition;

/**
 * AI spawner for Killer Game.
 * Spawns enemies at random positions in the maze.
 * Enemies move randomly through the maze, bouncing off walls.
 */
public class KillerEnemySpawner extends AbstractIAGenerator {

    // region Fields
    private final ArrayList<DefItem> enemyDefs;
    private final Random rnd = new Random();
    private final double worldWidth;
    private final double worldHeight;
    // endregion

    // *** CONSTRUCTORS ***

    public KillerEnemySpawner(
            WorldManager worldEvolver, 
            WorldDefinition worldDefinition,
            int maxCreationDelay) {

        super(worldEvolver, worldDefinition, maxCreationDelay);

        this.enemyDefs = this.worldDefinition.asteroids;
        this.worldWidth = worldDefinition.worldWidth;
        this.worldHeight = worldDefinition.worldHeight;
    }

    // *** PROTECTED (alphabetical order) ***

    @Override
    protected String getThreadName() {
        return "KillerEnemySpawner";
    }

    @Override
    protected void onActivate() {
        // Initialize any resources needed for spawning
        System.out.println("Killer Game - Enemy maze spawner activated!");
    }

    @Override
    protected void onTick() {
        // Select a random enemy definition
        DefItem defItem = this.enemyDefs.get(
                this.rnd.nextInt(this.enemyDefs.size()));

        this.addEnemy(defItem);
    }

    // *** PRIVATE (alphabetic order) ***

    private void addEnemy(DefItem defItem) {
        // Convert prototype to DTO to resolve range-based properties
        DefItemDTO enemyDef = this.defItemToDTO(defItem);

        // Spawn enemies at random positions in the maze
        // Avoid the corners and edges where walls are
        double margin = 150.0; // Stay away from outer walls
        double newPosX = margin + rnd.nextDouble() * (worldWidth - 2 * margin);
        double newPosY = margin + rnd.nextDouble() * (worldHeight - 2 * margin);
        
        // Give enemies random movement direction
        double angle = rnd.nextDouble() * 360.0;
        double speed = 60.0 + rnd.nextDouble() * 40.0; // Random speed between 60-100
        double speedX = speed * Math.cos(Math.toRadians(angle));
        double speedY = speed * Math.sin(Math.toRadians(angle));

        // Create new DTO with updated position and velocity
        DefItemDTO updatedEnemyDef = new DefItemDTO(
                enemyDef.assetId,
                enemyDef.size,
                angle,  // Random heading
                newPosX,
                newPosY,
                enemyDef.density,
                speedX,
                speedY,
                enemyDef.angularSpeed,
                0.0);  // No thrust for enemies

        // Inject enemy into the game
        this.addDynamicIntoTheGame(updatedEnemyDef);
    }
}
