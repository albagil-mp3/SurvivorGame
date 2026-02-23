package killergame;

import java.util.ArrayList;

import engine.model.bodies.ports.BodyData;
import engine.model.impl.Model;
import engine.model.physics.ports.PhysicsValuesDTO;
import killergame.MazeNavigator.Direction;
import killergame.MazeNavigator.Velocity;

/**
 * AI Controller for maze navigation.
 * Periodically updates enemy velocities based on grid-based pathfinding.
 */
public class MazeAIController implements Runnable {

    private final Model model;
    private final MazeNavigator navigator;
    private final double enemySpeed = 40.0; // Constant speed for enemies
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
                
                // Log every 50 updates (~5 seconds)
                if (updateCount % 50 == 0) {
                    int enemyCount = model.getDynamicEnemyCount();
                    System.out.println("[MAZE-AI] Update #" + updateCount + " - Active enemies: " + enemyCount);
                }
                
                // Sleep for 100ms (10 updates per second)
                Thread.sleep(100);
                
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

        if (bodiesCopy == null || bodiesCopy.isEmpty()) {
            return;
        }

        // Update each enemy
        for (BodyData bodyData : bodiesCopy) {
            updateSingleEnemy(bodyData);
        }
    }
    
    private boolean updateSingleEnemy(BodyData bodyData) {
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
       
        // Only consider changing direction when very close to the center of the current cell
        Direction nextDir = currentDir; // Default: keep current direction
        
        // Check if we need to change direction (always attempt to choose next direction)
        nextDir = navigator.chooseNextDirection(posX, posY, currentDir);

        // Prevent movement into walls even if we're off-center
        boolean blocked = navigator.isDirectionBlocked(posX, posY, nextDir);
        if (blocked) {
            Direction alternative = navigator.chooseNextDirection(posX, posY, currentDir);
            if (!navigator.isDirectionBlocked(posX, posY, alternative)) {
                nextDir = alternative;
                blocked = false;
            }
        }

        // Calculate new velocity for the chosen direction
        Velocity newVelocity = blocked
                ? new Velocity(0.0, 0.0)
                : navigator.getVelocityForDirection(nextDir, enemySpeed);

        double newPosX = posX;
        double newPosY = posY;
        if (blocked) {
            var center = navigator.getCellCenterForWorld(posX, posY);
            newPosX = center.x;
            newPosY = center.y;
        }

        // Keep enemies centered on the corridor axis to avoid clipping walls
        if (!blocked) {
            var center = navigator.getCellCenterForWorld(posX, posY);
            if (nextDir == Direction.EAST || nextDir == Direction.WEST) {
                newPosY = center.y;
            } else if (nextDir == Direction.NORTH || nextDir == Direction.SOUTH) {
                newPosX = center.x;
            }
        }
        
        // NO POSITION MODIFICATION - Let the enemy move completely naturally
        // Position is never modified, only velocity changes when direction changes
        
        // Always update velocity to counteract friction/collisions
        // Even if direction didn't change, we need to maintain speed
        PhysicsValuesDTO newPhyValues = new PhysicsValuesDTO(
            phyValues.timeStamp,
            newPosX,
            newPosY,
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
        }
        return false;
    }
}
