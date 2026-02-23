package engine.view.core;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JButton;

import engine.assets.core.AssetCatalog;
import engine.assets.ports.AssetType;
import engine.controller.impl.Controller;
import engine.controller.mappers.DynamicRenderableMapper;
import engine.controller.ports.EngineState;
import engine.utils.helpers.DoubleVector;
import engine.utils.images.Images;
import engine.view.renderables.ports.DynamicRenderDTO;
import engine.view.renderables.ports.PlayerRenderDTO;
import engine.view.renderables.ports.RenderDTO;
import engine.view.renderables.ports.RenderMetricsDTO;
import engine.view.renderables.ports.SpatialGridStatisticsRenderDTO;

/**
 * View
 * ----
 *
 * Swing top-level window that represents the presentation layer of the engine.
 * This class wires together:
 * - The rendering surface (Renderer)
 * - Asset loading and image catalogs (Images)
 * - User input (KeyListener) and command dispatch to the Controller
 *
 * Architectural role
 * ------------------
 * View is a thin façade over rendering + input:
 * - It does not simulate anything.
 * - It does not own world state.
 * - It communicates with the model exclusively through the Controller.
 *
 * The Renderer pulls dynamic snapshots every frame (via View -> Controller),
 * while static/decorator snapshots are pushed into the View/Renderer only when
 * they change (to avoid redundant per-frame updates for entities that do not
 * move).
 *
 * Lifecycle
 * ---------
 * Construction:
 * - Creates the ControlPanel (UI controls, if any).
 * - Creates the Renderer (Canvas).
 * - Builds the JFrame layout and attaches the key listener.
 *
 * Activation (activate()):
 * - Validates mandatory dependencies (dimensions, background, image catalogs).
 * - Injects view dimensions and images into the Renderer.
 * - Starts the Renderer thread (active rendering loop).
 *
 * Asset management
 * ----------------
 * loadAssets(...) loads and registers all visual resources required by the
 * world:
 * - Background image (single BufferedImage).
 * - Dynamic body sprites (ships, asteroids, missiles, etc.).
 * - Static body sprites (gravity bodies, bombs, etc.).
 * - Decorator sprites (parallax / space decor).
 *
 * The View stores catalogs as Images collections, which are later converted
 * into GPU/compatible caches inside the Renderer (ImageCache).
 *
 * Engine state delegation
 * -----------------------
 * View exposes getEngineState() as a convenience bridge for the Renderer.
 * The render loop can stop or pause based on Controller-owned engine state.
 *
 * Input handling
 * --------------
 * Keyboard input is captured at the rendering Canvas level (Renderer is
 * focusable and receives the KeyListener) and translated into high-level
 * Controller commands:
 * - Thrust on/off (forward uses positive thrust; reverse thrust is handled
 * as negative thrust, and both are stopped via the same thrustOff command).
 * - Rotation left/right and rotation off.
 * - Fire: handled as an edge-triggered action using fireKeyDown to prevent
 * key repeat from generating continuous shots while SPACE is held.
 *
 * Focus and Swing considerations
 * -------------------------------
 * The Renderer is the focus owner for input. Focus is requested after the frame
 * becomes visible using SwingUtilities.invokeLater(...) to improve reliability
 * with Swing's event dispatch timing.
 *
 * Threading considerations
 * ------------------------
 * Swing is single-threaded (EDT), while rendering runs on its own thread.
 * This class keeps its responsibilities minimal:
 * - It only pushes static/decorator updates when needed.
 * - Dynamic snapshot pulling is done inside the Renderer thread through
 * View -> Controller getters.
 *
 * Design goals
 * ------------
 * - Keep the View as a coordinator, not a state holder.
 * - Keep rendering independent and real-time (active rendering).
 * - Translate user input into controller commands cleanly and predictably.
 */
public class View extends JFrame implements KeyListener, WindowFocusListener, MouseMotionListener {

    // region Fields
    private BufferedImage background;
    private AssetCatalog assetCatalog;
    private Controller controller;
    private final ControlPanel controlPanel;
    private final Images images;
    private String localPlayerId;
    private final Renderer renderer;
    private CardLayout cardLayout;
    private JPanel rootPanel;
    private JPanel gamePanel;
    private JPanel menuPanel;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private DoubleVector viewDimension;
    private DoubleVector viewportDimension;
    private DoubleVector worldDimension;
    private AtomicBoolean fireKeyDown = new AtomicBoolean(false);
    
    // Mouse tracking for ship rotation
    private volatile int mouseX = 0;
    private volatile int mouseY = 0;

    // Key state tracking (OS may consume key events without firing keyReleased)
    private final Set<Integer> pressedKeys = new HashSet<>();
    private boolean wasWindowFocused = true;
    // endregion Fields

    // region Constructors
    public View() {
        this.images = new Images("");
        this.controlPanel = new ControlPanel(this);
        this.renderer = new Renderer(this);
        this.createFrame();
    }

    public View(DoubleVector worldDimension, DoubleVector viewDimension) {
        this();
        this.worldDimension = new DoubleVector(worldDimension);
        this.viewDimension = new DoubleVector(viewDimension);
        this.createFrame();
    }
    // endregion

    // *** PUBLIC ***

    public void activate() {
        if (this.viewDimension == null) {
            throw new IllegalArgumentException("View dimensions not setted");
        }
        if (this.background == null) {
            // throw new IllegalArgumentException("Background image not setted");
        }
        if (this.images.getSize() == 0) {
            // throw new IllegalArgumentException("Images catalog is empty");
        }
        if (this.controller == null) {
            throw new IllegalArgumentException("Controller not setted");
        }
        if (this.worldDimension == null) {
            throw new IllegalArgumentException("World dimensions not setted");
        }

        // new
        DoubleVector renderDimension = this.viewportDimension == null
                ? this.viewDimension
                : this.viewportDimension;
        this.renderer.setViewDimension(renderDimension);

        // this.renderer.setViewDimension(this.viewDimension);
        this.renderer.activate();
        this.pack();
        System.out.println("View: Activated");
    }

    // region adders (add***)
    public void addDynamicRenderable(String entityId, String assetId) {
        this.renderer.addDynamicRenderable(entityId, assetId);
    }

    public void addStaticRenderable(String entityId, String assetId) {
        this.renderer.addStaticRenderable(entityId, assetId);
    }
    // endregion

    // region Getters (get***)
    public DoubleVector getWorldDimension() {
        if (this.worldDimension == null) {
            return null;
        }

        return new DoubleVector(this.worldDimension);
    }

    public DoubleVector getViewDimension() {
        return new DoubleVector(this.viewDimension);
    }
    // endregion

    // region Setters (set***)
    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void setLocalPlayer(String localPlayerId) {
        this.localPlayerId = localPlayerId;
        System.out.println("Viewer: Local player setted " + localPlayerId);
    }

    public void setViewDimension(DoubleVector viewDim) {
        this.viewDimension = viewDim;
    }

    public void setViewportDimension(DoubleVector viewportDim) {
        this.viewportDimension = viewportDim;
    }

    public void setMenuPanel(JPanel menuPanel) {
        if (menuPanel == null || this.cardLayout == null || this.rootPanel == null) {
            return;
        }
        this.menuPanel.removeAll();
        this.menuPanel.setLayout(new BorderLayout());
        this.menuPanel.add(menuPanel, BorderLayout.CENTER);
        this.menuPanel.revalidate();
        this.menuPanel.repaint();
    }

    public void showMenu() {
        if (this.cardLayout != null && this.rootPanel != null) {
            this.cardLayout.show(this.rootPanel, "menu");
        }
    }

    public void showGame() {
        if (this.cardLayout != null && this.rootPanel != null) {
            this.cardLayout.show(this.rootPanel, "game");
            SwingUtilities.invokeLater(this::updateViewportToFrame);
        }
    }

    public void setWorldDimension(DoubleVector worldDim) {
        this.worldDimension = worldDim;
    }
    // endregion

    public void loadAssets(AssetCatalog assets) {
        this.assetCatalog = assets;
        
        String fileName;
        String path = assets.getPath();

        for (String assetId : assets.getAssetIds()) {
            fileName = assets.get(assetId).fileName;
            this.images.add(assetId, path + fileName);
        }
        System.out.println("View.loadAssets: loaded " + this.images.getSize() + " images: " + this.images.getAssetIds());

        // Setting background
        String backgroundId = assets.randomId(AssetType.BACKGROUND);
        System.out.println("View: Setting background image <" + backgroundId + ">");
        this.background = this.images.getImage(backgroundId).image;

        if (this.background == null) {
            throw new IllegalArgumentException("Background image could not be loaded");
        }
        
        // Pass asset catalog to renderer for animation support
        this.renderer.setAssetCatalog(assets);

        this.renderer.setImages(this.background, this.images);
    }

    // region notifiers (notify***)
    public void notifyDynamicIsDead(String entityId) {
        this.renderer.notifyDynamicIsDead(entityId);
    }

    public void notifyPlayerIsDead(String entityId) {
        this.setLocalPlayer(null);
    }
    // endregion

    public void updateStaticRenderables(ArrayList<RenderDTO> renderablesData) {
        this.renderer.updateStaticRenderables(renderablesData);
    }

    // *** PROTECTED ***

    // region protected Getters (get***)
    protected ArrayList<DynamicRenderDTO> snapshotRenderData() {
        if (this.controller == null) {
            throw new IllegalArgumentException("Controller not setted");
        }

        return this.controller.snapshotRenderData();
    }

    protected ArrayList<DynamicRenderDTO> snapshotRenderData(DynamicRenderableMapper mapper) {
        if (this.controller == null) {
            throw new IllegalArgumentException("Controller not setted");
        }

        return this.controller.snapshotRenderData(mapper);
    }

    protected EngineState getEngineState() {
        return this.controller.getEngineState();
    }

    protected int getEntityAliveQuantity() {
        return this.controller.getEntityAliveQuantity();
    }

    protected int getEntityCreatedQuantity() {
        return this.controller.getEntityCreatedQuantity();
    }

    protected int getEntityDeadQuantity() {
        return this.controller.getEntityDeadQuantity();
    }

    protected PlayerRenderDTO getLocalPlayerRenderData() {
        if (this.localPlayerId == null || this.localPlayerId.isEmpty()) {
            return null;
        }

        return this.controller.getPlayerRenderData(this.localPlayerId);
    }

    public String getLocalPlayerId() {
        return this.localPlayerId;
    }

    protected Object[] getProfilingHUDValues(long fps) {
        return this.controller.getProfilingHUDValues(fps);
    }

    protected SpatialGridStatisticsRenderDTO getSpatialGridStatistics() {
        return this.controller.getSpatialGridStatistics();
    }

    protected RenderMetricsDTO getRenderMetrics() {
        return this.renderer.getRenderMetrics();
    }
    // endregion

    /**
     * Queries the model via controller for entities visible in the specified
     * region.
     * Fills the provided buffers with results.
     * 
     * @param minX               left edge of query region
     * @param maxX               right edge of query region
     * @param minY               top edge of query region
     * @param maxY               bottom edge of query region
     * @param scratchCellIndices buffer for spatial grid cell indices
     * @param scratchEntityIds   buffer to fill with visible entity IDs
     * @return list of entity IDs in region
     */
    public ArrayList<String> queryEntitiesInRegion(
            double minX, double maxX, double minY, double maxY,
            int[] scratchCellIndices, ArrayList<String> scratchEntityIds) {

        // Relay al controller (que tiene acceso al modelo)
        return this.controller.queryEntitiesInRegion(
                minX, maxX, minY, maxY,
                scratchCellIndices, scratchEntityIds);
    }

    // *** PRIVATE ***

    private void addRenderer(Container container) {
        this.leftPanel = new JPanel();
        this.leftPanel.setBackground(Color.BLACK);
        this.leftPanel.setPreferredSize(new Dimension(220, 1));

        this.rightPanel = new JPanel();
        this.rightPanel.setBackground(Color.BLACK);
        this.rightPanel.setPreferredSize(new Dimension(220, 1));

        this.renderer.setPreferredSize(new Dimension(1000, 1000));

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 1.0;
        container.add(this.leftPanel, c);

        GridBagConstraints center = new GridBagConstraints();
        center.anchor = GridBagConstraints.CENTER;
        center.fill = GridBagConstraints.NONE;
        center.gridx = 1;
        center.gridy = 0;
        center.weightx = 0.0;
        center.weighty = 1.0;
        container.add(this.renderer, center);

        GridBagConstraints right = new GridBagConstraints();
        right.anchor = GridBagConstraints.CENTER;
        right.fill = GridBagConstraints.BOTH;
        right.gridx = 2;
        right.gridy = 0;
        right.weightx = 1.0;
        right.weighty = 1.0;
        container.add(this.rightPanel, right);
    }

    private void createFrame() {
        Container panel;

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new GridBagLayout());

        this.setResizable(true);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setSize(screenSize);
        this.setMinimumSize(new Dimension(800, 800));
        this.setLocationRelativeTo(null);

        this.cardLayout = new CardLayout();
        this.rootPanel = new JPanel(this.cardLayout);
        this.gamePanel = new JPanel(new GridBagLayout());
        this.menuPanel = new JPanel(new BorderLayout());
        this.menuPanel.setBackground(Color.BLACK);

        this.addRenderer(this.gamePanel);

        this.rootPanel.add(this.menuPanel, "menu");
        this.rootPanel.add(this.gamePanel, "game");
        this.cardLayout.show(this.rootPanel, "menu");

        panel = this.getContentPane();
        panel.setLayout(new BorderLayout());
        panel.add(this.rootPanel, BorderLayout.CENTER);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateViewportToFrame();
            }
        });

        this.setFocusable(true);
        this.addKeyListener(this);
        this.addWindowFocusListener(this);

        this.renderer.setFocusable(false); // El Renderer NO necesita foco
        this.renderer.setIgnoreRepaint(true); // Mejor performance
        this.renderer.addMouseMotionListener(this); // Mouse tracking on renderer canvas

        this.pack();
        this.setVisible(true);

        SwingUtilities.invokeLater(() -> {
            this.setExtendedState(JFrame.MAXIMIZED_BOTH);
            updateViewportToFrame();
            this.requestFocusInWindow();
        });
    }

    private void updateViewportToFrame() {
        Dimension size = this.gamePanel == null ? this.getContentPane().getSize() : this.gamePanel.getSize();
        if (size.width <= 0 || size.height <= 0) {
            return;
        }

        int gameSize = 800;
        int sideWidth = Math.max(0, (size.width - gameSize) / 2);
        if (this.leftPanel != null) {
            this.leftPanel.setPreferredSize(new Dimension(sideWidth, size.height));
        }
        if (this.rightPanel != null) {
            this.rightPanel.setPreferredSize(new Dimension(sideWidth, size.height));
        }
        this.renderer.setPreferredSize(new Dimension(gameSize, gameSize));

        DoubleVector viewport = new DoubleVector(gameSize, gameSize);
        this.viewportDimension = viewport;
        this.renderer.setViewDimension(viewport);
        this.renderer.revalidate();
        this.renderer.repaint();
    }

    private void resetAllKeyStates() {
        if (this.localPlayerId == null || this.controller == null) {
            return;
        }

        try {
            // Resetear TODOS los controles activos
            this.controller.playerThrustOff(this.localPlayerId);
            this.controller.playerRotateOff(this.localPlayerId);
            this.fireKeyDown.set(false);
        } catch (Exception ex) {
            throw new RuntimeException("Error resetting key states: " + ex.getMessage(), ex);
        }
    }

    /**
     * Sync input state each frame.
     * OS may consume keyboard events (Alt+Tab, Win+X, etc) without firing
     * keyReleased(),
     * causing tracking to become inconsistent. Called from Renderer each frame.
     */
    public void syncInputState() {
        // Update ship angle to face mouse cursor
        this.updateShipAngleToMouse();
        
        if (this.localPlayerId == null || this.controller == null || this.pressedKeys.isEmpty()) {
            return;
        }

        // When window lacks focus, all keys should be released
        if (!this.wasWindowFocused) {
            if (!this.pressedKeys.isEmpty()) {
                System.out.println("View.syncInputState: Window not focused but keys tracked: "
                        + this.pressedKeys + " - clearing");

                Set<Integer> keysToRelease = new HashSet<>(this.pressedKeys);
                this.pressedKeys.clear();

                for (int keyCode : keysToRelease) {
                    try {
                        this.processKeyRelease(keyCode);
                    } catch (Exception ex) {
                        throw new RuntimeException("View: Key release failed during focus loss: " + keyCode, ex);
                    }
                }
            }
            return;
        }
    }

    // *** INTERFACE IMPLEMENTATIONS ***

    // region WindowFocusListener
    /**
     * Detectamos pérdida de foco para resetear estado de teclas.
     * Esto es crítico porque si el usuario presiona Alt+Tab,
     * el keyReleased() nunca se genera.
     */
    @Override
    public void windowLostFocus(WindowEvent e) {
        this.wasWindowFocused = false;

        // Clear pressed keys (won't receive keyReleased for them)
        Set<Integer> keysToRelease = new HashSet<>(this.pressedKeys);
        this.pressedKeys.clear();

        for (int keyCode : keysToRelease) {
            try {
                this.processKeyRelease(keyCode);
            } catch (Exception ex) {
                throw new RuntimeException("View: Key release failed on focus lost: " + keyCode, ex);
            }
        }

        System.out.println("View: Window lost focus - pressed keys cleared: " + keysToRelease);
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        this.wasWindowFocused = true;
        System.out.println("View: Window gained focus");
    }
    // endregion

    // region KeyListener
    @Override
    public void keyPressed(KeyEvent e) {
        try {
            if (this.localPlayerId == null || this.controller == null) {
                return;
            }

            int keyCode = e.getKeyCode();

            // Agregar a tracking si ya no estaba presionada
            if (!this.pressedKeys.contains(keyCode)) {
                this.pressedKeys.add(keyCode);

                // Process only first press (not OS key repeat)
                this.processKeyPress(keyCode);
            }
        } catch (Exception ex) {
            resetAllKeyStates();
            throw new RuntimeException("View: keyPressed event failed", ex);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        try {
            if (this.localPlayerId == null || this.controller == null) {
                return;
            }

            int keyCode = e.getKeyCode();

            this.pressedKeys.remove(keyCode);

            this.processKeyRelease(keyCode);
        } catch (Exception ex) {
            throw new RuntimeException("View: keyReleased event failed", ex);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Nothing to do
    }

    /**
     * Procesamiento de keyPress (se llama solo una vez cuando se presiona).
     * NO se llama en key repeat.
     */
    private void processKeyPress(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                this.controller.playerMoveUpOn(this.localPlayerId);
                break;

            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_X:
            case KeyEvent.VK_S:
                this.controller.playerMoveDownOn(this.localPlayerId);
                break;

            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                this.controller.playerMoveLeftOn(this.localPlayerId);
                break;

            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                this.controller.playerMoveRightOn(this.localPlayerId);
                break;

            case KeyEvent.VK_SPACE:
                if (!this.fireKeyDown.get()) {
                    this.fireKeyDown.set(true);
                    this.controller.playerFire(this.localPlayerId);
                }
                break;

            case KeyEvent.VK_1:
                this.controller.playerSelectNextWeapon(this.localPlayerId);
                break;
        }
    }

    /**
     * Procesamiento de keyRelease (se llama cuando se libera la tecla).
     * Puede no llamarse si el OS consume el evento.
     */
    private void processKeyRelease(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                this.controller.playerMoveUpOff(this.localPlayerId);
                break;

            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_X:
            case KeyEvent.VK_S:
                this.controller.playerMoveDownOff(this.localPlayerId);
                break;

            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                this.controller.playerMoveLeftOff(this.localPlayerId);
                break;

            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                this.controller.playerMoveRightOff(this.localPlayerId);
                break;

            case KeyEvent.VK_SPACE:
                this.fireKeyDown.set(false);
                break;
        }
    }
    // endregion

    // region MouseMotionListener
    @Override
    public void mouseMoved(MouseEvent e) {
        this.mouseX = e.getX();
        this.mouseY = e.getY();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        this.mouseX = e.getX();
        this.mouseY = e.getY();
    }
    // endregion
    
    // *** PRIVATE ***
    
    /**
     * Update ship angle to face the mouse cursor.
     * Called each frame from syncInputState.
     * The ship follows the mouse cursor continuously as it moves.
     */
    private void updateShipAngleToMouse() {
        if (this.localPlayerId == null || this.controller == null) {
            return;
        }
        
        // Get player position in world coordinates
        DoubleVector playerPos = this.controller.getPlayerPosition(this.localPlayerId);
        if (playerPos == null) {
            return;
        }
        
        // Get camera position from renderer
        double cameraX = this.renderer.getCameraX();
        double cameraY = this.renderer.getCameraY();
        
        // Convert ship world position to screen position
        double shipScreenX = playerPos.x - cameraX;
        double shipScreenY = playerPos.y - cameraY;
        
        // Calculate angle from ship screen position to mouse cursor
        double dx = this.mouseX - shipScreenX;
        double dy = this.mouseY - shipScreenY;
        
        double angleRadians = Math.atan2(dy, dx);
        double angleDegrees = Math.toDegrees(angleRadians);
        
        // Update player angle - this happens every frame so ship always points to mouse
        this.controller.playerSetAngle(this.localPlayerId, angleDegrees);
    }

}
