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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Image;
import java.awt.IllegalComponentStateException;

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
    private ImagePanel backgroundPanel;
    private volatile boolean menuLocked = false;
    private boolean activated = false;
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
    private PauseOverlay pauseOverlay;
    private JButton pauseButton;

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
        if (this.activated) {
            // Already activated; avoid re-packing/resizing the window
            this.renderer.setViewDimension(this.viewportDimension == null
                    ? this.viewDimension
                    : this.viewportDimension);
            return;
        }
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
        this.activated = true;
        // Silent: View activated
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
        // Silent: local player set
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
            // When menu is visible, hide pause controls and overlay
            try {
                if (this.pauseButton != null) this.pauseButton.setVisible(false);
                if (this.pauseOverlay != null) this.pauseOverlay.setVisible(false);
                // Hide Play Again if it was shown during Game Over
                this.hidePlayAgainButton();
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    public void setMenuLocked(boolean locked) {
        this.menuLocked = locked;
        if (locked) {
            this.showMenu();
        }
    }

    public void showGame() {
        if (this.menuLocked) {
            this.showMenu();
            return;
        }
        if (this.cardLayout != null && this.rootPanel != null) {
            this.cardLayout.show(this.rootPanel, "game");
            SwingUtilities.invokeLater(this::updateViewportToFrame);
            // Ensure pause button visible when in game
            try {
                if (this.pauseButton != null) this.pauseButton.setVisible(true);
            } catch (Throwable t) {
                // ignore
            }
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
        // Silent: view assets loaded

        // Setting background
        String backgroundId = assets.randomId(AssetType.BACKGROUND);
        // Silent: setting background image
        this.background = this.images.getImage(backgroundId).image;

        if (this.background == null) {
            throw new IllegalArgumentException("Background image could not be loaded");
        }
        
        // Pass asset catalog to renderer for animation support
        this.renderer.setAssetCatalog(assets);

        this.renderer.setImages(this.background, this.images);
        
        // Set full-window background image: try multiple sources until one works
        try {
            java.awt.image.BufferedImage bgImg = null;
            
            // Try menu-back first
            if (this.images.getImage("menu-back") != null) {
                bgImg = this.images.getImage("menu-back").image;
                // Silent: using menu-back for background
            } 
            // Try theme_back
            else if (this.images.getImage("theme_back") != null) {
                bgImg = this.images.getImage("theme_back").image;
                // Silent: using theme_back for background
            }
            // Use primary background
            else if (this.background != null) {
                bgImg = this.background;
                // Silent: using primary background image
            }
            // Try any available background asset
            else {
                try {
                    String randomBgId = assets.randomId(AssetType.BACKGROUND);
                    if (randomBgId != null && this.images.getImage(randomBgId) != null) {
                        bgImg = this.images.getImage(randomBgId).image;
                        // Silent: using random background
                    }
                } catch (Exception e) {
                    // Silent: no background assets available
                }
            }

            if (this.backgroundPanel != null) {
                if (bgImg != null) {
                    this.backgroundPanel.setImage(bgImg);
                    // Silent: background image applied to backgroundPanel
                } else {
                    // Silent: no background image found, keeping black background
                }
                this.backgroundPanel.revalidate();
                this.backgroundPanel.repaint();
            }
        } catch (Throwable t) {
            System.err.println("View.loadAssets: could not set background image: " + t.getMessage());
            t.printStackTrace();
        }
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
        // Renderer centered with fixed preferred size inside the game panel
        this.renderer.setPreferredSize(new Dimension(800, 800));
        
        // Wrap renderer in a panel to add border (Canvas doesn't support setBorder)
        JPanel rendererWrapper = new JPanel(new BorderLayout());
        rendererWrapper.setOpaque(false);
        rendererWrapper.setBorder(javax.swing.BorderFactory.createLineBorder(Color.WHITE, 3));
        rendererWrapper.add(this.renderer, BorderLayout.CENTER);

        GridBagConstraints center = new GridBagConstraints();
        center.anchor = GridBagConstraints.CENTER;
        center.fill = GridBagConstraints.NONE; // keep renderer fixed size
        center.gridx = 0;
        center.gridy = 0;
        center.weightx = 1.0;
        center.weighty = 1.0;
        container.add(rendererWrapper, center);
    }

    private void addControlPanel(Container container) {
        GridBagConstraints c = new GridBagConstraints();

        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.VERTICAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0F;
        c.weighty = 0F;
        c.gridheight = 10;
        c.gridwidth = 1;

        container.add(this.controlPanel, c);
    }

    private void createFrame() {
        Container panel;
        this.backgroundPanel = new ImagePanel();
        this.backgroundPanel.setBackground(Color.BLACK);
        // Default black background until assets load
        this.backgroundPanel.setOpaque(true);

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
        this.rootPanel.setOpaque(false);
        this.gamePanel = new JPanel(new GridBagLayout()); 
        this.gamePanel.setOpaque(false);
        this.menuPanel = new JPanel(new BorderLayout());
        this.menuPanel.setOpaque(false);

        this.addRenderer(this.gamePanel);

        this.rootPanel.add(this.menuPanel, "menu");
        this.rootPanel.add(this.gamePanel, "game");
        this.cardLayout.show(this.rootPanel, "menu");

        // Set backgroundPanel as content pane and add rootPanel into it
        this.setContentPane(this.backgroundPanel);
        panel = this.getContentPane();
        panel.setLayout(new BorderLayout());
        panel.add(this.rootPanel, BorderLayout.CENTER);
        
        // Ensure backgroundPanel expands to fill entire frame
        this.backgroundPanel.setPreferredSize(null);
        this.backgroundPanel.setMinimumSize(new Dimension(800, 800));

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

        // Create pause overlay and button on layered pane
        try {
            this.pauseOverlay = new PauseOverlay(this);
            this.pauseOverlay.setVisible(false);
            this.pauseOverlay.setBounds(0, 0, this.getWidth(), this.getHeight());
            this.getLayeredPane().add(this.pauseOverlay, javax.swing.JLayeredPane.MODAL_LAYER);

            this.pauseButton = new JButton("||");
            this.pauseButton.setFocusable(false);
            this.pauseButton.setToolTipText("Pause");
            this.pauseButton.setBounds(this.getWidth() - 80, 24, 48, 32);
            this.pauseButton.addActionListener(e -> {
                try {
                    if (this.controller != null) this.controller.enginePause();
                } catch (Throwable t) {}
                this.pauseOverlay.showOverlay();
            });
            this.getLayeredPane().add(this.pauseButton, javax.swing.JLayeredPane.PALETTE_LAYER);
        } catch (Throwable t) {
            // ignore overlay creation errors
        }

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
        this.renderer.setPreferredSize(new Dimension(gameSize, gameSize));

        DoubleVector viewport = new DoubleVector(gameSize, gameSize);
        this.viewportDimension = viewport;
        this.renderer.setViewDimension(viewport);
        this.renderer.revalidate();
        this.renderer.repaint();

        // Update pause overlay and button bounds when frame resizes
        try {
            if (this.pauseOverlay != null) {
                this.pauseOverlay.setBounds(0, 0, size.width, size.height);
            }
            if (this.pauseButton != null) {
                int btnW = 48;
                int btnH = 32;
                int x = Math.max(16, size.width - btnW - 20);
                int y = 20;
                this.pauseButton.setBounds(x, y, btnW, btnH);
            }
        } catch (Throwable t) {
            // ignore layout errors
        }
    }

    /**
     * Show the "Play again" button and attach the action to run when clicked.
     * The provided runnable will be executed in a new thread to avoid blocking
     * the Swing EDT.
     */
    public void showPlayAgainButton(Runnable onPlayAgain) {
        SwingUtilities.invokeLater(() -> {
            // Configure action on the control panel button
            this.controlPanel.setPlayAgainAction(e -> {
                // Hide overlay/button immediately
                this.hidePlayAgainButton();
                if (onPlayAgain != null) new Thread(onPlayAgain, "PlayAgainAction").start();
            });

            // Move the button to the layered pane so it appears OVER the renderer
            try {
                JButton btn = this.controlPanel.getPlayAgainButton();
                java.awt.Container parent = btn.getParent();
                if (parent != null) parent.remove(btn);

                // Center button under the GAME OVER / Final score text using same fonts as Renderer
                java.awt.Container layer = this.getLayeredPane();
                int renderW = this.renderer.getWidth();
                int renderH = this.renderer.getHeight();

                int offsetX;
                int offsetY;
                try {
                    java.awt.Point rOnScreen = this.renderer.getLocationOnScreen();
                    java.awt.Point lOnScreen = layer.getLocationOnScreen();
                    offsetX = rOnScreen.x - lOnScreen.x;
                    offsetY = rOnScreen.y - lOnScreen.y;
                } catch (IllegalComponentStateException ex) {
                    java.awt.Point p = javax.swing.SwingUtilities.convertPoint(this.renderer, 0, 0, layer);
                    offsetX = p.x;
                    offsetY = p.y;
                }

                if (renderW <= 0 || renderH <= 0) {
                    renderW = (int) (this.viewDimension == null ? this.getWidth() : this.viewDimension.x);
                    renderH = (int) (this.viewDimension == null ? this.getHeight() : this.viewDimension.y);
                    offsetX = (this.getWidth() - renderW) / 2;
                    offsetY = (this.getHeight() - renderH) / 2;
                }

                int cx = offsetX + renderW / 2;
                int cy = offsetY + renderH / 2;

                // Compute title and score font ascents using this component's FontMetrics
                java.awt.Font titleFont = new java.awt.Font("SansSerif", java.awt.Font.BOLD, 72);
                java.awt.Font scoreFont = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 36);
                int titleAscent = this.getFontMetrics(titleFont).getAscent();
                int scoreAscent = this.getFontMetrics(scoreFont).getAscent();

                // Renderer draws title at (cy - titleAscent/2) and score at (cy + titleAscent)
                // Place button a few pixels below the score baseline
                int scoreY = cy + titleAscent;
                int padding = 14;
                java.awt.Dimension pref = btn.getPreferredSize();
                int btnW = pref == null ? 200 : pref.width;
                int btnH = pref == null ? 50 : pref.height;
                int x = cx - btnW / 2;
                int y = scoreY + scoreAscent + padding;

                btn.setBounds(x, y, btnW, btnH);
                btn.setVisible(true);

                layer.add(btn, javax.swing.JLayeredPane.PALETTE_LAYER);
                layer.revalidate();
                layer.repaint();
            } catch (Throwable t) {
                // Fallback to showing in control panel if layered pane fails
                this.controlPanel.showPlayAgain(true);
            }
        });
    }

    public void hidePlayAgainButton() {
        SwingUtilities.invokeLater(() -> {
            try {
                JButton btn = this.controlPanel.getPlayAgainButton();
                java.awt.Container parent = btn.getParent();
                if (parent != null) parent.remove(btn);
                this.getLayeredPane().revalidate();
                this.getLayeredPane().repaint();
            } catch (Throwable t) {
                // ignore
            }
            this.controlPanel.showPlayAgain(false);
        });
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
                // Silent: Window not focused but keys tracked; clearing

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

        // Silent: Window lost focus - pressed keys cleared
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        this.wasWindowFocused = true;
        // Silent: Window gained focus
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

    // Simple panel that paints a scaled background image
    private static class ImagePanel extends JPanel {
        private java.awt.image.BufferedImage image;

        public void setImage(java.awt.image.BufferedImage img) {
            this.image = img;
            this.repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            int w = this.getWidth();
            int h = this.getHeight();
            
            if (this.image == null) {
                // Fill with background color if no image
                g.setColor(this.getBackground());
                g.fillRect(0, 0, w, h);
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int imgW = this.image.getWidth();
            int imgH = this.image.getHeight();

            if (imgW <= 0 || imgH <= 0) {
                g2.drawImage(this.image, 0, 0, w, h, null);
            } else {
                // Scale image to COVER the panel (may crop)
                double scale = Math.max((double) w / imgW, (double) h / imgH);
                int drawW = (int) Math.ceil(imgW * scale);
                int drawH = (int) Math.ceil(imgH * scale);
                int x = (w - drawW) / 2;
                int y = (h - drawH) / 2;
                g2.drawImage(this.image, x, y, drawW, drawH, null);
            }

            g2.dispose();
        }
    }

    // Pause overlay helpers
    public void setPauseResetHandler(Runnable r) {
        if (this.pauseOverlay != null) this.pauseOverlay.setOnReset(r);
    }

    public void setPauseExitHandler(Runnable r) {
        if (this.pauseOverlay != null) this.pauseOverlay.setOnExit(r);
    }

    public void showPauseOverlay() {
        try {
            if (this.controller != null) this.controller.enginePause();
            if (this.pauseOverlay != null) this.pauseOverlay.showOverlay();
        } catch (Throwable t) {}
    }

    public void hidePauseOverlay() {
        try {
            if (this.pauseOverlay != null) this.pauseOverlay.hideOverlay();
            if (this.controller != null) this.controller.engineResume();
        } catch (Throwable t) {}
    }

}
