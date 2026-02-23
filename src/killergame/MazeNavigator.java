package killergame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Maze navigator for enemy AI.
 * Provides grid-based pathfinding and navigation decisions.
 */
public class MazeNavigator {

    private static final int WALL = 0;
    private static final int PATH = 1;
    
    private final int[][] mazeGrid;
    private final double offsetX;
    private final double offsetY;
    private final int cellSize;
    private final int rows;
    private final int cols;
    private final Random random = new Random();
    
    public MazeNavigator(int[][] mazeGrid, double offsetX, double offsetY, int cellSize) {
        this.mazeGrid = mazeGrid;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.cellSize = cellSize;
        this.rows = mazeGrid.length;
        this.cols = mazeGrid[0].length;
    }
    
    /**
     * Converts world coordinates to grid coordinates.
     */
    public GridPosition worldToGrid(double worldX, double worldY) {
        int col = (int)((worldX - offsetX) / cellSize);
        int row = (int)((worldY - offsetY) / cellSize);
        return new GridPosition(row, col);
    }
    
    /**
     * Converts grid coordinates to world coordinates (center of cell).
     */
    public WorldPosition gridToWorld(int row, int col) {
        double worldX = offsetX + col * cellSize + cellSize / 2.0;
        double worldY = offsetY + row * cellSize + cellSize / 2.0;
        return new WorldPosition(worldX, worldY);
    }

    /**
     * Gets the center of the grid cell containing the given world position.
     */
    public WorldPosition getCellCenterForWorld(double worldX, double worldY) {
        GridPosition gridPos = worldToGrid(worldX, worldY);
        return gridToWorld(gridPos.row, gridPos.col);
    }
    
    /**
     * Checks if a grid position is a valid path (not wall, within bounds).
     */
    public boolean isValidPath(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return false;
        }
        return mazeGrid[row][col] == PATH;
    }

    /**
     * Checks if moving in a direction would hit a wall at the cell boundary.
     */
    public boolean isDirectionBlocked(double worldX, double worldY, Direction dir) {
        GridPosition gridPos = worldToGrid(worldX, worldY);
        GridPosition nextCell = getNextCell(gridPos, dir);

        if (isValidPath(nextCell.row, nextCell.col)) {
            return false;
        }

        WorldPosition center = gridToWorld(gridPos.row, gridPos.col);
        double half = cellSize * 0.5;
        double margin = cellSize * 0.1;

        return switch (dir) {
            case EAST -> worldX >= center.x + half - margin;
            case WEST -> worldX <= center.x - half + margin;
            case SOUTH -> worldY >= center.y + half - margin;
            case NORTH -> worldY <= center.y - half + margin;
        };
    }
    
    /**
     * Gets all valid directions from current position.
     * Returns list of directions: 0=North, 1=East, 2=South, 3=West
     */
    public List<Direction> getValidDirections(double worldX, double worldY) {
        GridPosition gridPos = worldToGrid(worldX, worldY);
        List<Direction> validDirs = new ArrayList<>();
        
        // North (up, -Y)
        if (isValidPath(gridPos.row - 1, gridPos.col)) {
            validDirs.add(Direction.NORTH);
        }
        
        // East (right, +X)
        if (isValidPath(gridPos.row, gridPos.col + 1)) {
            validDirs.add(Direction.EAST);
        }
        
        // South (down, +Y)
        if (isValidPath(gridPos.row + 1, gridPos.col)) {
            validDirs.add(Direction.SOUTH);
        }
        
        // West (left, -X)
        if (isValidPath(gridPos.row, gridPos.col - 1)) {
            validDirs.add(Direction.WEST);
        }
        
        return validDirs;
    }
    
    /**
     * Chooses next direction based on current direction and available paths.
     * Priority: continue forward > turn left/right > turn back
     */
    public Direction chooseNextDirection(double worldX, double worldY, Direction currentDir) {
        GridPosition currentCell = worldToGrid(worldX, worldY);
        
        // Get the next cell in current direction
        GridPosition nextCell = getNextCell(currentCell, currentDir);
        
        // Check if we can continue in current direction
        if (isValidPath(nextCell.row, nextCell.col)) {
            // Path ahead is clear, continue
            return currentDir;
        }
        
        // Can't continue forward, need to choose new direction
        List<Direction> validDirs = getValidDirections(worldX, worldY);
        
        if (validDirs.isEmpty()) {
            return currentDir; // Stuck, keep trying same direction
        }
        
        // Remove current direction (we know it's blocked)
        validDirs.remove(currentDir);
        
        // Avoid turning back if possible
        Direction opposite = getOpposite(currentDir);
        validDirs.remove(opposite);
        
        if (validDirs.isEmpty()) {
            return opposite; // Must turn back
        }
        
        // Choose random valid direction (not forward, not back)
        return validDirs.get(random.nextInt(validDirs.size()));
    }
    
    /**
     * Gets the next grid cell in a given direction.
     */
    private GridPosition getNextCell(GridPosition current, Direction dir) {
        return switch(dir) {
            case NORTH -> new GridPosition(current.row - 1, current.col);
            case SOUTH -> new GridPosition(current.row + 1, current.col);
            case EAST -> new GridPosition(current.row, current.col + 1);
            case WEST -> new GridPosition(current.row, current.col - 1);
        };
    }
    
    /**
     * Gets opposite direction.
     */
    private Direction getOpposite(Direction dir) {
        return switch(dir) {
            case NORTH -> Direction.SOUTH;
            case SOUTH -> Direction.NORTH;
            case EAST -> Direction.WEST;
            case WEST -> Direction.EAST;
        };
    }
    
    /**
     * Gets velocity vector for a direction.
     */
    public Velocity getVelocityForDirection(Direction dir, double speed) {
        return switch(dir) {
            case NORTH -> new Velocity(0, -speed);
            case SOUTH -> new Velocity(0, speed);
            case EAST -> new Velocity(speed, 0);
            case WEST -> new Velocity(-speed, 0);
        };
    }
    
    /**
     * Gets current direction based on velocity.
     */
    public Direction getCurrentDirection(double velocityX, double velocityY) {
        // Add a check for zero velocity (stationary)
        if (Math.abs(velocityX) < 0.1 && Math.abs(velocityY) < 0.1) {
            // If stationary, return a default direction (EAST)
            return Direction.EAST;
        }
        
        double absX = Math.abs(velocityX);
        double absY = Math.abs(velocityY);
        
        if (absX > absY) {
            return velocityX > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return velocityY > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }
    
    /**
     * Checks if entity is very close to cell center (for precise direction changes).
     */
    public boolean isAtCellCenter(double worldX, double worldY) {
        WorldPosition cellCenter = gridToWorld(
            worldToGrid(worldX, worldY).row,
            worldToGrid(worldX, worldY).col
        );
        
        double distX = Math.abs(worldX - cellCenter.x);
        double distY = Math.abs(worldY - cellCenter.y);
        double threshold = cellSize * 0.1; // Very tight threshold for smooth direction changes
        
        return distX < threshold && distY < threshold;
    }
    
    /**
     * Gets the cell size for distance calculations.
     */
    public int getCellSize() {
        return cellSize;
    }
    
    /**
     * Checks if entity is approximately centered in a cell.
     */
    public boolean isNearCellCenter(double worldX, double worldY) {
        WorldPosition cellCenter = gridToWorld(
            worldToGrid(worldX, worldY).row,
            worldToGrid(worldX, worldY).col
        );
        
        double distX = Math.abs(worldX - cellCenter.x);
        double distY = Math.abs(worldY - cellCenter.y);
        double threshold = cellSize * 0.3; // More lenient threshold for smoother direction changes
        
        return distX < threshold && distY < threshold;
    }
    
    // *** INNER CLASSES ***
    
    public enum Direction {
        NORTH, EAST, SOUTH, WEST
    }
    
    public static class GridPosition {
        public final int row;
        public final int col;
        
        public GridPosition(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }
    
    public static class WorldPosition {
        public final double x;
        public final double y;
        
        public WorldPosition(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
    
    public static class Velocity {
        public final double vx;
        public final double vy;
        
        public Velocity(double vx, double vy) {
            this.vx = vx;
            this.vy = vy;
        }
    }
}
