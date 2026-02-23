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
        System.out.println("MazeAIController activated!");
    }
    
    public void deactivate() {
        this.running = false;
    }
    
    @Override
    public void run() {
        System.out.println("[MAZE-AI] MazeAIController thread started!");
        
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

        if (bodiesCopy == null || bodiesCopy.isEmpty()) {
            return; // No enemies to update
        }

        // System.out.println("[MAZE-AI] Updating " + bodiesCopy.size() + " enemies");

        // Update each enemy
        int updatedCount = 0;
        for (BodyData bodyData : bodiesCopy) {
            if (updateSingleEnemy(bodyData)) {
                updatedCount++;
            }
        }
        
        // Log periodically (less frequently for smoother output)
        if (updatedCount > 0 && System.currentTimeMillis() % 3000 < 100) { // Log roughly every 3 seconds
            System.out.println("[MAZE-AI] Smoothly updated " + updatedCount + "/" + bodiesCopy.size() + " enemies");
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
        
        if (navigator.isAtCellCenter(posX, posY)) {
            // Only when very close to center, check if we need to change direction
            nextDir = navigator.chooseNextDirection(posX, posY, currentDir);
        }
        
        // Calculate new velocity for the chosen direction
        Velocity newVelocity = navigator.getVelocityForDirection(nextDir, enemySpeed);
        
        // NO POSITION MODIFICATION - Let the enemy move completely naturally
        // Position is never modified, only velocity changes when direction changes
        
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
}
