package killergame;

import java.util.ArrayList;

import engine.controller.impl.Controller;
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
        // Get all dynamic bodies (enemies)
        ArrayList<BodyData> bodies = model.snapshotRenderData();
        
        if (bodies == null || bodies.isEmpty()) {
            return;
        }
        
        // IMPORTANT: Make a defensive copy and filter out nulls
        // The snapshot buffer is shared and can be cleared by other threads
        ArrayList<BodyData> bodiesCopy = new ArrayList<>();
        synchronized (bodies) {
            for (BodyData body : bodies) {
                if (body != null && body.type == engine.model.bodies.ports.BodyType.DYNAMIC) {
                    bodiesCopy.add(body);
                }
            }
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
       
        // Choose next direction based on available paths
        Direction nextDir = navigator.chooseNextDirection(posX, posY, currentDir);
        
        // Calculate new velocity for the chosen direction
        Velocity newVelocity = navigator.getVelocityForDirection(nextDir, enemySpeed);
        
        // Check if direction actually changed
        boolean directionChanged = Math.abs(newVelocity.vx - speedX) > 5.0 || 
                                   Math.abs(newVelocity.vy - speedY) > 5.0;
        
        // CENTER the enemy in the current cell when changing direction
        // This prevents wall clipping when turning
        if (directionChanged) {
            MazeNavigator.GridPosition gridPos = navigator.worldToGrid(posX, posY);
            MazeNavigator.WorldPosition cellCenter = navigator.gridToWorld(gridPos.row, gridPos.col);
            posX = cellCenter.x;
            posY = cellCenter.y;
        }
        
        // Always update velocity to counteract friction/collisions
        // Even if direction didn't change, we need to maintain speed
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
        }
        return false;
    }
}
