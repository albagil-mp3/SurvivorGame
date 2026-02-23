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
    private static final int MAX_ENEMIES = 150; // Maximum number of concurrent enemies
    
    private final ArrayList<DefItem> enemyDefs;
    private final Random rnd = new Random();
    private final double worldWidth;
    private final double worldHeight;
    private final MazeNavigator navigator;
    // endregion

    // *** CONSTRUCTORS ***

    public KillerEnemySpawner(
            WorldManager worldEvolver, 
            WorldDefinition worldDefinition,
            int maxCreationDelay,
            MazeNavigator navigator) {

        super(worldEvolver, worldDefinition, maxCreationDelay);

        this.enemyDefs = this.worldDefinition.asteroids;
        this.worldWidth = worldDefinition.worldWidth;
        this.worldHeight = worldDefinition.worldHeight;
        this.navigator = navigator;
    }

    // *** PROTECTED (alphabetical order) ***

    @Override
    protected String getThreadName() {
        return "KillerEnemySpawner";
    }

    @Override
    protected void onActivate() {
        // Initialize any resources needed for spawning
        // Silent: enemy spawner activated
    }

    @Override
    protected void onTick() {
        if (this.enemyDefs.isEmpty()) {
            System.err.println("[ERROR] No enemy definitions available!");
            return;
        }
        
        // Check current enemy count
        int currentEnemies = this.worldEvolver.getDynamicEnemyCount();
        
        // Don't spawn if we've reached the maximum
        if (currentEnemies >= MAX_ENEMIES) {
            // Max enemy limit reached - wait for an enemy to be destroyed
            return;
        }
        
        // Check if we can add more bodies to the world (total entity limit)
        if (!this.worldEvolver.canAddDynamicBody()) {
            // Max total entities reached - wait for space to become available
            return;
        }
        
        // Select a random enemy definition
        DefItem defItem = this.enemyDefs.get(
                this.rnd.nextInt(this.enemyDefs.size()));

        this.addEnemy(defItem);
    }

    // *** PRIVATE (alphabetic order) ***

    private void addEnemy(DefItem defItem) {
        // Convert prototype to DTO to resolve range-based properties
        DefItemDTO enemyDef = this.defItemToDTO(defItem);

        MazeNavigator.WorldPosition spawnPos = findRandomMazeSpawnPosition();
        double newPosX = spawnPos.x;
        double newPosY = spawnPos.y;
        
        // Use navigator to get a valid starting direction
        java.util.List<MazeNavigator.Direction> validDirs = navigator.getValidDirections(newPosX, newPosY);
        
        MazeNavigator.Direction startDir;
        if (validDirs.isEmpty()) {
            // Fallback to random direction if no valid paths (shouldn't happen in center)
            MazeNavigator.Direction[] allDirs = MazeNavigator.Direction.values();
            startDir = allDirs[rnd.nextInt(allDirs.length)];
            System.err.println("[WARN] No valid paths at spawn position, using random direction");
        } else {
            // Choose random valid direction
            startDir = validDirs.get(rnd.nextInt(validDirs.size()));
        }
        
        // Get velocity for chosen direction
        double speed = 150.0; // Matched with MazeAIController speed
        MazeNavigator.Velocity velocity = navigator.getVelocityForDirection(startDir, speed);
        double speedX = velocity.vx;
        double speedY = velocity.vy;
        double angle = Math.toDegrees(Math.atan2(speedY, speedX));

        double enemySize = 30.0; // Small enemy size to fit in maze corridors

        // Create new DTO with updated position and velocity
        DefItemDTO updatedEnemyDef = new DefItemDTO(
                enemyDef.assetId,
                enemySize,      // Small size to fit in corridors
                angle,          // Direction angle
                newPosX,
                newPosY,
                enemyDef.density,
                speedX,
                speedY,
                enemyDef.angularSpeed,
                0.0);           // No thrust for enemies

        // Inject enemy into the game
        this.addDynamicIntoTheGame(updatedEnemyDef);
    }

    private MazeNavigator.WorldPosition findRandomMazeSpawnPosition() {
        final int maxAttempts = 200;

        for (int i = 0; i < maxAttempts; i++) {
            double randomX = this.rnd.nextDouble() * this.worldWidth;
            double randomY = this.rnd.nextDouble() * this.worldHeight;

            MazeNavigator.GridPosition grid = this.navigator.worldToGrid(randomX, randomY);
            if (!this.navigator.isValidPath(grid.row, grid.col)) {
                continue;
            }

            // Ensure we spawn in a usable corridor cell (at least one outgoing path)
            MazeNavigator.WorldPosition worldPos = this.navigator.gridToWorld(grid.row, grid.col);
            if (!this.navigator.getValidDirections(worldPos.x, worldPos.y).isEmpty()) {
                return worldPos;
            }
        }

        // Fallback: center cell if random probing fails
        double centerX = this.worldWidth / 2.0;
        double centerY = this.worldHeight / 2.0;
        MazeNavigator.GridPosition centerGrid = this.navigator.worldToGrid(centerX, centerY);
        return this.navigator.gridToWorld(centerGrid.row, centerGrid.col);
    }
}
