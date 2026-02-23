package killergame;

import java.util.ArrayList;

import engine.model.bodies.ports.BodyData;
import engine.model.bodies.ports.BodyType;
import engine.model.impl.Model;
import engine.model.physics.ports.PhysicsValuesDTO;
import killergame.MazeNavigator.Direction;
import killergame.MazeNavigator.Velocity;

/**
 * AI Controller for maze navigation.
 * Periodically updates enemy velocities based on grid-based pathfinding.
 */
public class MazeAIController implements Runnable {

    private static final int FLEE_RADIUS_CELLS = 4;
    private static final double UNSTUCK_CENTER_SPEED_FACTOR = 0.45;

    private final Model model;
    private final MazeNavigator navigator;
    private final double enemySpeed = 150.0; 
    private final Thread thread;
    private volatile boolean running = false;
    
    public MazeAIController(Model model, MazeNavigator navigator) {
        this.model = model;
        this.navigator = navigator;
        this.thread = new Thread(this, "MazeAIController");
    }
    
    public void activate() {
        this.running = true;
        this.thread.start();
    }
    
    public void deactivate() {
        this.running = false;
    }
    
    @Override
    public void run() {
        
        int updateCount = 0;
        while (this.running) {
            try {
                // Update enemy directions
                updateEnemyDirections();
                updateCount++;
                
                // Log every 60 updates (~6 seconds) with less frequency
                if (updateCount % 60 == 0) {
                    int enemyCount = model.getDynamicEnemyCount();
                    System.out.println("[MAZE-AI] Update #" + updateCount + " - Active enemies: " + enemyCount + 
                                     " - Smooth navigation active");
                }
                
                // Sleep for 150ms (about 7 updates per second) - slightly slower for smoother visuals
                Thread.sleep(150);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[ERROR] MazeAIController exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("[MAZE-AI] MazeAIController thread stopped.");
    }
    
    private void updateEnemyDirections() {
        // Get only DYNAMIC enemy bodies via thread-safe snapshot
        // (avoids corrupting the shared scratchDynamicsBuffer used by the Renderer)
        ArrayList<BodyData> bodiesCopy = model.snapshotDynamicEnemies();
        MazeNavigator.GridPosition playerGrid = getNearestPlayerGridPosition();

        if (bodiesCopy == null || bodiesCopy.isEmpty()) {
            return; // No enemies to update
        }

        // System.out.println("[MAZE-AI] Updating " + bodiesCopy.size() + " enemies");

        // Update each enemy
        int updatedCount = 0;
        for (BodyData bodyData : bodiesCopy) {
            if (updateSingleEnemy(bodyData, playerGrid)) {
                updatedCount++;
            }
        }
        
        // Log periodically (less frequently for smoother output)
        if (updatedCount > 0 && System.currentTimeMillis() % 3000 < 100) { // Log roughly every 3 seconds
            System.out.println("[MAZE-AI] Smoothly updated " + updatedCount + "/" + bodiesCopy.size() + " enemies");
        }
    }
    
    private boolean updateSingleEnemy(BodyData bodyData, MazeNavigator.GridPosition playerGrid) {
        PhysicsValuesDTO phyValues = bodyData.getPhysicsValues();
        if (phyValues == null) {
            System.out.println("[MAZE-AI] Enemy has null physics values");
            return false;
        }
        
        double posX = phyValues.posX;
        double posY = phyValues.posY;
        double speedX = phyValues.speedX;
        double speedY = phyValues.speedY;
        
        // Get current direction based on velocity
        Direction currentDir = navigator.getCurrentDirection(speedX, speedY);
        MazeNavigator.GridPosition enemyGrid = navigator.worldToGrid(posX, posY);
        boolean playerNear = isPlayerNear(enemyGrid, playerGrid);
       
        // Only consider changing direction when very close to the center of the current cell
        Direction nextDir = currentDir; // Default: keep current direction
        
        if (navigator.isAtCellCenter(posX, posY)) {
            // Only when very close to center, check if we need to change direction
            nextDir = playerNear
                    ? chooseFleeDirection(posX, posY, currentDir, playerGrid)
                    : navigator.chooseNextDirection(posX, posY, currentDir);
        }

        // Prevent movement into walls even if we're off-center
        boolean blocked = navigator.isDirectionBlocked(posX, posY, nextDir);
        if (blocked) {
            Direction alternative = findViableDirection(posX, posY, currentDir, playerNear, playerGrid);
            if (alternative != null) {
                nextDir = alternative;
                blocked = false;
            }
        }

        // Calculate new velocity for the chosen direction
        Velocity newVelocity;
        if (!blocked) {
            newVelocity = navigator.getVelocityForDirection(nextDir, enemySpeed);
        } else {
            // Last-resort anti-stuck: gently steer towards cell center instead of freezing.
            MazeNavigator.WorldPosition center = navigator.getCellCenterForWorld(posX, posY);
            double toCenterX = center.x - posX;
            double toCenterY = center.y - posY;
            double len = Math.sqrt(toCenterX * toCenterX + toCenterY * toCenterY);

            if (len > 0.001) {
                double unstuckSpeed = enemySpeed * UNSTUCK_CENTER_SPEED_FACTOR;
                newVelocity = new Velocity((toCenterX / len) * unstuckSpeed, (toCenterY / len) * unstuckSpeed);
            } else {
                newVelocity = new Velocity(0.0, 0.0);
            }
        }
        
        // Always update velocity to maintain movement (even if not changing position)
        PhysicsValuesDTO newPhyValues = new PhysicsValuesDTO(
            phyValues.timeStamp,
            posX,
            posY,
            phyValues.angle,
            phyValues.size,
            newVelocity.vx,  // New velocity X
            newVelocity.vy,  // New velocity Y
            0.0,  // No acceleration
            0.0,  // No acceleration
            phyValues.angularSpeed,
            phyValues.angularAcc,
            phyValues.thrust
        );
        
        // Apply new physics values to the body
        var body = model.getBody(bodyData.entityId, bodyData.type);
        if (body != null) {
            body.doMovement(newPhyValues);
            return true;
        } else {
            System.out.println("[MAZE-AI] Could not find body for enemy " + bodyData.entityId);
        }
        return false;
    }

    private MazeNavigator.GridPosition getNearestPlayerGridPosition() {
        ArrayList<BodyData> allDynamics = model.snapshotRenderData();
        if (allDynamics == null || allDynamics.isEmpty()) {
            return null;
        }

        for (BodyData bodyData : allDynamics) {
            if (bodyData == null) {
                continue;
            }
            if (bodyData.type != BodyType.PLAYER) {
                continue;
            }

            PhysicsValuesDTO playerPhy = bodyData.getPhysicsValues();
            if (playerPhy == null) {
                continue;
            }

            return navigator.worldToGrid(playerPhy.posX, playerPhy.posY);
        }

        return null;
    }

    private boolean isPlayerNear(MazeNavigator.GridPosition enemyGrid, MazeNavigator.GridPosition playerGrid) {
        if (enemyGrid == null || playerGrid == null) {
            return false;
        }

        int manhattan = Math.abs(enemyGrid.row - playerGrid.row) + Math.abs(enemyGrid.col - playerGrid.col);
        return manhattan <= FLEE_RADIUS_CELLS;
    }

    private Direction chooseFleeDirection(
            double enemyWorldX,
            double enemyWorldY,
            Direction currentDir,
            MazeNavigator.GridPosition playerGrid) {

        if (playerGrid == null) {
            return navigator.chooseNextDirection(enemyWorldX, enemyWorldY, currentDir);
        }

        java.util.List<Direction> validDirs = navigator.getValidDirections(enemyWorldX, enemyWorldY);
        if (validDirs.isEmpty()) {
            return currentDir;
        }

        MazeNavigator.GridPosition enemyGrid = navigator.worldToGrid(enemyWorldX, enemyWorldY);
        Direction opposite = getOpposite(currentDir);

        Direction bestDir = validDirs.get(0);
        int bestDistance = Integer.MIN_VALUE;

        for (Direction dir : validDirs) {
            int nextRow = enemyGrid.row;
            int nextCol = enemyGrid.col;

            switch (dir) {
                case NORTH:
                    nextRow--;
                    break;
                case SOUTH:
                    nextRow++;
                    break;
                case EAST:
                    nextCol++;
                    break;
                case WEST:
                    nextCol--;
                    break;
                default:
                    break;
            }

            int distance = Math.abs(nextRow - playerGrid.row) + Math.abs(nextCol - playerGrid.col);

            if (distance > bestDistance) {
                bestDistance = distance;
                bestDir = dir;
            } else if (distance == bestDistance && dir == opposite) {
                // Tie-breaker: prefer reversing direction to quickly escape in corridors.
                bestDir = dir;
            }
        }

        return bestDir;
    }

    private Direction getOpposite(Direction dir) {
        switch (dir) {
            case NORTH:
                return Direction.SOUTH;
            case SOUTH:
                return Direction.NORTH;
            case EAST:
                return Direction.WEST;
            case WEST:
                return Direction.EAST;
            default:
                return dir;
        }
    }

    private Direction findViableDirection(
            double worldX,
            double worldY,
            Direction currentDir,
            boolean playerNear,
            MazeNavigator.GridPosition playerGrid) {

        Direction preferred = playerNear
                ? chooseFleeDirection(worldX, worldY, currentDir, playerGrid)
                : navigator.chooseNextDirection(worldX, worldY, currentDir);

        if (preferred != null && !navigator.isDirectionBlocked(worldX, worldY, preferred)) {
            return preferred;
        }

        Direction opposite = getOpposite(currentDir);
        if (opposite != null && !navigator.isDirectionBlocked(worldX, worldY, opposite)) {
            return opposite;
        }

        for (Direction dir : navigator.getValidDirections(worldX, worldY)) {
            if (!navigator.isDirectionBlocked(worldX, worldY, dir)) {
                return dir;
            }
        }

        return null;
    }
}
