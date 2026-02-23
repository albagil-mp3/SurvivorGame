package killergame;

import java.util.ArrayList;
import java.util.Random;

import engine.controller.ports.WorldManager;
import engine.generators.AbstractLevelGenerator;
import engine.world.ports.DefItemDTO;
import engine.world.ports.WorldDefinition;

/**
 * Level generator for Killer Game.
 * Creates a single procedural maze covering the entire world with 4 structural variants.
 * Uses DFS algorithm for maze generation.
 */
public class KillerLevelGenerator extends AbstractLevelGenerator {

    private static final Random random = new Random();
    private double wallSize;
    private double wallSpacing;
    private double centerX;
    private double centerY;
    private double spawnBoxHalf;
    private double spawnWallSize;
    private double worldWidth;
    private double worldHeight;
    private double mazeOffsetX;
    private double mazeOffsetY;
    private int mazeCellSize;
    private int[][] mazeGrid; // Store the maze grid for navigation
    
    // Maze generation constants
    private static final int WALL = 0;
    private static final int PATH = 1;
    
    // Maze variant types
    private enum MazeType {
        PERFECT,           // Perfect maze (no loops)
        LOOPS,             // Maze with controlled loops
        HORIZONTAL_BIAS,   // Horizontal corridor preference
        VERTICAL_BIAS      // Vertical corridor preference
    }

    // *** CONSTRUCTORS ***

    public KillerLevelGenerator(WorldManager worldManager, WorldDefinition worldDef) {
        super(worldManager, worldDef);
    }

    // *** PROTECTED (alphabetic order) ***

    @Override
    protected void createDecorators() {
        // No decorators for the maze
    }

    @Override
    protected void createDynamics() {
        // No initial dynamic bodies
    }

    @Override
    protected void createStatics() {
        // Initialize maze parameters
        WorldDefinition worldDef = this.getWorldDefinition();
        this.worldWidth = worldDef.worldWidth;
        this.worldHeight = worldDef.worldHeight;
        this.wallSize = 40.0;
        this.wallSpacing = 20.0;
        this.centerX = worldWidth / 2;
        this.centerY = worldHeight / 2;
        int spawnCells = 5; 
        double spawnBoxSize = spawnCells * wallSize;
        this.spawnBoxHalf = spawnBoxSize / 2;
        this.spawnWallSize = wallSize / 2.0;
        
        // Select random maze variant
        MazeType[] allVariants = MazeType.values();
        MazeType selectedVariant = allVariants[random.nextInt(allVariants.length)];
        
        System.out.println("Creating global procedural maze");
        System.out.println("Selected variant: " + selectedVariant);
        
        // Generate global maze
        generateGlobalMaze(selectedVariant);
        
        // Create border walls
        createBorderWalls();
        
        // Create central spawn box
        createSpawnBox();
    }
    
    @Override
    protected void createPlayers() {
        java.util.ArrayList<engine.world.ports.DefItem> shipDefs = this.getWorldDefinition().spaceships;
        java.util.ArrayList<engine.world.ports.DefEmitterDTO> weaponDefs = this.getWorldDefinition().weapons;
        for (engine.world.ports.DefItem def : shipDefs) {
            engine.world.ports.DefItemDTO body = this.defItemToDTO(def);
            this.addLocalPlayerIntoTheGame(body, weaponDefs);
        }
    }
    
    // *** PUBLIC METHODS ***
    
    /**
     * Creates a MazeNavigator for AI pathfinding.
     * Call this after level generation is complete.
     */
    public MazeNavigator createMazeNavigator() {
        if (mazeGrid == null) {
            throw new IllegalStateException("Maze not generated yet. Call createStatics() first.");
        }
        return new MazeNavigator(mazeGrid, mazeOffsetX, mazeOffsetY, mazeCellSize);
    }
    
    // *** PRIVATE HELPERS - Global Maze Generation ***
    
    /**
     * Generates a single global maze covering the entire world.
     */
   private void generateGlobalMaze(MazeType variant) {

        int cellSize = 40;

        // Calcular cuántas celdas caben en el mundo
        int cols = (int)(worldWidth / cellSize);
        int rows = (int)(worldHeight / cellSize);

        // Forzar impares
        if (cols % 2 == 0) cols--;
        if (rows % 2 == 0) rows--;

        if (cols < 3 || rows < 3) return;

        int[][] maze = new int[rows][cols];

        // Inicializar todo como WALL
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                maze[r][c] = WALL;

        // Generar estructura
        switch (variant) {
            case PERFECT:
                dfsMazeGeneration(maze, 1, 1);
                break;
            case LOOPS:
                dfsMazeGeneration(maze, 1, 1);
                addControlledLoops(maze, 0.12);
                break;
            case HORIZONTAL_BIAS:
                dfsMazeGenerationWithBias(maze, 1, 1, 0.70, true);
                break;
            case VERTICAL_BIAS:
                dfsMazeGenerationWithBias(maze, 1, 1, 0.70, false);
                break;
        }

        // Calcular tamaño REAL del maze en píxeles
        double realWidth = cols * cellSize;
        double realHeight = rows * cellSize;

        // Calcular offset para centrarlo
        double offsetX = (worldWidth - realWidth) / 2.0;
        double offsetY = (worldHeight - realHeight) / 2.0;

        // Vaciar zona central
        clearCentralSpawnArea(maze, cellSize);

        // Guardar datos del grid para alinear el spawn y navegación
        this.mazeOffsetX = offsetX;
        this.mazeOffsetY = offsetY;
        this.mazeCellSize = cellSize;
        this.mazeGrid = maze; // Store for navigation

        // Renderizar centrado
        renderMazeGrid(maze, offsetX, offsetY, cellSize, "wall_01");
    }
    
    /**
     * Clears the central spawn area in the maze grid.
     */
    private void clearCentralSpawnArea(int[][] maze, int cellSize) {

        int rows = maze.length;
        int cols = maze[0].length;

        int centerRow = rows / 2;
        int centerCol = cols / 2;

        int spawnCells = 5;          // tamaño spawn
        int separationCells = 1;     // EXACTAMENTE 1 pasillo

        int totalHalf = (spawnCells / 2) + separationCells;

        for (int r = centerRow - totalHalf; r <= centerRow + totalHalf; r++) {
            for (int c = centerCol - totalHalf; c <= centerCol + totalHalf; c++) {

                if (r >= 0 && r < rows && c >= 0 && c < cols) {
                    maze[r][c] = PATH;
                }
            }
        }
    }

    // *** PRIVATE HELPERS - Infrastructure ***
    
    private void createBorderWalls() {
        double margin = 20.0;
        
        // Top wall
        for (double x = margin; x < worldWidth - margin; x += wallSpacing) {
            addWallSegment("wall_01", wallSize , x, margin);
        }
        
        // Bottom wall
        for (double x = margin; x < worldWidth - margin; x += wallSpacing) {
            addWallSegment("wall_01", wallSize, x, worldHeight - margin);
        }
        
        // Left wall
        for (double y = margin; y < worldHeight - margin; y += wallSpacing) {
            addWallSegment("wall_01", wallSize, margin, y);
        }
        
        // Right wall
        for (double y = margin; y < worldHeight - margin; y += wallSpacing) {
            addWallSegment("wall_01", wallSize, worldWidth - margin, y);
        }
    }
    
   private void createSpawnBox() {

        int spawnCells = 5;   // siempre impar
        int half = spawnCells / 2;

        int centerCol = (int)((centerX - mazeOffsetX) / mazeCellSize);
        int centerRow = (int)((centerY - mazeOffsetY) / mazeCellSize);

        int topRow = centerRow - half;
        int bottomRow = centerRow + half;
        int leftCol = centerCol - half;
        int rightCol = centerCol + half;

        // --- PARED SUPERIOR E INFERIOR ---
        for (int c = leftCol; c <= rightCol; c++) {

            // hueco central
            if (c == centerCol) continue;

            double x = mazeOffsetX + c * mazeCellSize + mazeCellSize / 2.0;

            double topY = mazeOffsetY + topRow * mazeCellSize + mazeCellSize / 2.0;
            double bottomY = mazeOffsetY + bottomRow * mazeCellSize + mazeCellSize / 2.0;

            addWallSegment("wall_02", mazeCellSize, x, topY);
            addWallSegment("wall_02", mazeCellSize, x, bottomY);
            
            // UPDATE MAZE GRID - mark these cells as walls for MazeNavigator
            if (topRow >= 0 && topRow < mazeGrid.length && c >= 0 && c < mazeGrid[0].length) {
                mazeGrid[topRow][c] = WALL;
            }
            if (bottomRow >= 0 && bottomRow < mazeGrid.length && c >= 0 && c < mazeGrid[0].length) {
                mazeGrid[bottomRow][c] = WALL;
            }
        }

        // --- PARED IZQUIERDA Y DERECHA ---
        for (int r = topRow; r <= bottomRow; r++) {

            // hueco central
            if (r == centerRow) continue;

            double y = mazeOffsetY + r * mazeCellSize + mazeCellSize / 2.0;

            double leftX = mazeOffsetX + leftCol * mazeCellSize + mazeCellSize / 2.0;
            double rightX = mazeOffsetX + rightCol * mazeCellSize + mazeCellSize / 2.0;

            addWallSegment("wall_02", mazeCellSize, leftX, y);
            addWallSegment("wall_02", mazeCellSize, rightX, y);
            
            // UPDATE MAZE GRID - mark these cells as walls for MazeNavigator
            if (r >= 0 && r < mazeGrid.length && leftCol >= 0 && leftCol < mazeGrid[0].length) {
                mazeGrid[r][leftCol] = WALL;
            }
            if (r >= 0 && r < mazeGrid.length && rightCol >= 0 && rightCol < mazeGrid[0].length) {
                mazeGrid[r][rightCol] = WALL;
            }
        }
    }

    // *** MAZE GENERATION CORE ALGORITHMS ***
    
    /**
     * Classic DFS recursive backtracking algorithm.
     * Guarantees a perfect maze (no loops, fully connected).
     */
    private void dfsMazeGeneration(int[][] maze, int row, int col) {
        int rows = maze.length;
        int cols = maze[0].length;
        
        // Mark current cell as path
        maze[row][col] = PATH;
        
        // Directions: up, right, down, left
        int[][] directions = {{-2, 0}, {0, 2}, {2, 0}, {0, -2}};
        
        // Shuffle directions for randomness
        shuffleArray(directions);
        
        // Try each direction
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            
            // Check bounds
            if (newRow > 0 && newRow < rows && newCol > 0 && newCol < cols) {
                // If unvisited
                if (maze[newRow][newCol] == WALL) {
                    // Carve path between current and new cell
                    maze[row + dir[0] / 2][col + dir[1] / 2] = PATH;
                    // Recursively visit new cell
                    dfsMazeGeneration(maze, newRow, newCol);
                }
            }
        }
    }
    
    /**
     * DFS with directional bias.
     * @param bias Probability (0.0-1.0) of preferring biased direction
     * @param horizontal true for horizontal bias, false for vertical
     */
    private void dfsMazeGenerationWithBias(int[][] maze, int row, int col, double bias, boolean horizontal) {
        int rows = maze.length;
        int cols = maze[0].length;
        
        maze[row][col] = PATH;
        
        // Separate preferred and non-preferred directions
        ArrayList<int[]> preferred = new ArrayList<>();
        ArrayList<int[]> others = new ArrayList<>();
        
        int[][] allDirections = {{-2, 0}, {0, 2}, {2, 0}, {0, -2}};
        
        for (int[] dir : allDirections) {
            if (horizontal && (dir[1] != 0)) {
                preferred.add(dir); // left/right
            } else if (!horizontal && (dir[0] != 0)) {
                preferred.add(dir); // up/down
            } else {
                others.add(dir);
            }
        }
        
        // Build biased direction list
        ArrayList<int[]> directions = new ArrayList<>();
        
        // Add preferred directions with higher probability
        for (int[] dir : preferred) {
            if (random.nextDouble() < bias) {
                directions.add(dir);
            }
        }
        directions.addAll(preferred); // Always add at least once
        directions.addAll(others);
        
        // Shuffle the combined list
        for (int i = directions.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int[] temp = directions.get(i);
            directions.set(i, directions.get(j));
            directions.set(j, temp);
        }
        
        // Try each direction
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            
            if (newRow > 0 && newRow < rows && newCol > 0 && newCol < cols) {
                if (maze[newRow][newCol] == WALL) {
                    maze[row + dir[0] / 2][col + dir[1] / 2] = PATH;
                    dfsMazeGenerationWithBias(maze, newRow, newCol, bias, horizontal);
                }
            }
        }
    }
    
    /**
     * Adds controlled loops by removing random walls.
     * @param percentage Percentage of walls to remove (0.0-1.0)
     */
    private void addControlledLoops(int[][] maze, double percentage) {
        int rows = maze.length;
        int cols = maze[0].length;
        
        // Count total walls that could be removed (interior walls only)
        int removableWalls = 0;
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                if (maze[r][c] == WALL && hasAdjacentPaths(maze, r, c)) {
                    removableWalls++;
                }
            }
        }
        
        int wallsToRemove = (int)(removableWalls * percentage);
        int removed = 0;
        
        // Remove random walls
        while (removed < wallsToRemove) {
            int r = 1 + random.nextInt(rows - 2);
            int c = 1 + random.nextInt(cols - 2);
            
            if (maze[r][c] == WALL && hasAdjacentPaths(maze, r, c)) {
                maze[r][c] = PATH;
                removed++;
            }
        }
    }
    
    /**
     * Checks if a cell has adjacent path cells.
     */
    private boolean hasAdjacentPaths(int[][] maze, int row, int col) {
        int pathCount = 0;
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        for (int[] dir : dirs) {
            int r = row + dir[0];
            int c = col + dir[1];
            if (r >= 0 && r < maze.length && c >= 0 && c < maze[0].length) {
                if (maze[r][c] == PATH) pathCount++;
            }
        }
        
        return pathCount >= 2;
    }
    
    // *** UTILITY METHODS ***
    
    /**
     * Renders a maze grid (int[][]) as physical walls in the game world.
     */
    private void renderMazeGrid(int[][] maze, double offsetX, double offsetY, int cellSize, String assetId) {
    int rows = maze.length;
    int cols = maze[0].length;

    for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {

            if (maze[r][c] == WALL) {

                double x = offsetX + c * cellSize + cellSize / 2.0;
                double y = offsetY + r * cellSize + cellSize / 2.0;

                // UNA sola pared grande
                this.addStaticIntoTheGame(new DefItemDTO(
                        assetId,
                        cellSize,   // tamaño completo
                        0.0,
                        x,
                        y,
                        100.0
                ));
            }
        }
    }
}
    
    /**
     * Shuffles an array using Fisher-Yates algorithm.
     */
    private void shuffleArray(int[][] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int[] temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }
    
    private void addWallSegment(String assetId, double size, double x, double y) {
    this.addStaticIntoTheGame(new DefItemDTO(
            assetId,
            size,
            0.0,
            x,
            y,
            100.0));
}
}
