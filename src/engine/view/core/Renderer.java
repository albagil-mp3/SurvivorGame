package engine.view.core;

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import engine.controller.ports.EngineState;
import engine.utils.helpers.DoubleVector;
import engine.utils.images.ImageCache;
import engine.utils.images.Images;
import engine.view.hud.impl.PlayerHUD;
import engine.view.hud.impl.SpatialGridHUD;
import engine.view.hud.impl.SystemHUD;
import engine.view.renderables.impl.DynamicRenderable;
import engine.view.renderables.impl.Renderable;
import engine.view.renderables.ports.DynamicRenderDTO;
import engine.view.renderables.ports.PlayerRenderDTO;
import engine.view.renderables.ports.RenderDTO;
import engine.view.renderables.ports.SpatialGridStatisticsRenderDTO;

import java.awt.Toolkit;

/**
 * Renderer
 * --------
 *
 * Active rendering loop responsible for drawing the current frame to the
 * screen. This class owns the rendering thread and performs all drawing using
 * a BufferStrategy-based back buffer.
 *
 * Architectural role
 * ------------------
 * The Renderer is a pull-based consumer of visual snapshots provided by the
 * View.
 * It never queries or mutates the model directly.
 *
 * Rendering is decoupled from simulation through immutable snapshot DTOs
 * (EntityInfoDTO / DBodyInfoDTO), ensuring that rendering remains deterministic
 * and free of model-side race conditions.
 *
 * Threading model
 * ---------------
 * - A dedicated render thread drives the render loop (Runnable).
 * - Rendering is active only while the engine state is ALIVE.
 * - The loop terminates cleanly when the engine reaches STOPPED.
 *
 * Data access patterns
 * --------------------
 * Three different renderable collections are used, each with a consciously
 * chosen
 * concurrency strategy based on update frequency and thread ownership:
 *
 * 1) Dynamic bodies (DBodies)
 * - Stored in a plain HashMap.
 * - Updated and rendered exclusively by the render thread.
 * - No concurrent access → no synchronization required.
 *
 * 2) Static bodies (SBodies)
 * - Rarely updated, potentially from non-render threads
 * (model → controller → view).
 * - Stored using a copy-on-write strategy:
 * * Updates create a new Map instance.
 * * The reference is swapped atomically via a volatile field.
 * - The render thread only reads stable snapshots.
 *
 * 3) Decorators
 * - Same access pattern as static bodies.
 * - Uses the same copy-on-write + atomic swap strategy.
 *
 * This design avoids locks, minimizes contention, and guarantees that the
 * render thread always iterates over a fully consistent snapshot.
 *
 * Frame tracking
 * --------------
 * A monotonically increasing frame counter (currentFrame) is used to:
 * - Track renderable liveness.
 * - Remove obsolete renderables deterministically.
 *
 * Each update method captures a local frame snapshot to ensure internal
 * consistency, even if the global frame counter advances later.
 *
 * Rendering pipeline
 * ------------------
 * Per frame:
 * 1) Background is rendered to a VolatileImage for fast blitting.
 * 2) Decorators are drawn.
 * 3) Static bodies are drawn.
 * 4) Dynamic bodies are updated and drawn.
 * 5) HUD elements (FPS) are rendered last.
 *
 * Alpha compositing is used to separate opaque background rendering from
 * transparent entities.
 *
 * Performance considerations
 * --------------------------
 * - Triple buffering via BufferStrategy.
 * - VolatileImage used for background caching.
 * - Target frame rate ~60 FPS (16 ms delay).
 * - FPS is measured using a rolling one-second window.
 *
 * Design goals
 * ------------
 * - Deterministic rendering.
 * - Zero blocking in the render loop.
 * - Clear ownership of mutable state.
 * - Explicit, documented concurrency decisions.
 *
 * This class is intended to behave as a low-level rendering component suitable
 * for a small game engine rather than a UI-centric Swing renderer.
 */
public class Renderer extends Canvas implements Runnable {

    // region Monitoring
    private static final long MONITORING_PERIOD_NS = 500_000_000L;

    // Start times (timestamps)
    private volatile long monitoringPeriodStartNs = System.nanoTime();
    private volatile long drawStartNs;
    private volatile long frameStartNs = 0L;
    private volatile long lastFrameStartNs = 0L;

    // End times (timestamps)
    private volatile long drawEndNs;
    private volatile long frameEndNs;

    // Values for metrics
    private volatile long frameDtNs = 0L; // dt real: start-to-start
    private volatile long frameTimeNs = 0L;
    private volatile long acumDrawNsPerPeriod = 0L;
    private volatile long framesPerPeriod = 0L;
    private volatile long fpsPerPeriod = 0L;
    private volatile double avgDrawMsPerPeriod = 0.0D;
    // endregion

    // regoin Constants
    private static final int DELAY_IN_MILLIS = 16; // ~60 FPS
    // endregion

    // region Fields
    private DoubleVector viewDimension;
    private View view;
    private int delayInMillis = 5;
    private long currentFrame = 0;
    private Thread thread;

    private BufferedImage background;
    private Images images;
    private ImageCache imagesCache;
    private VolatileImage viBackground;
    private final PlayerHUD playerHUD = new PlayerHUD();
    private final SystemHUD systemHUD = new SystemHUD();
    private final SpatialGridHUD spatialGridHUD = new SpatialGridHUD();

    private double cameraX = 0.0d;
    private double cameraY = 0.0d;
    private double maxCameraClampY;
    private double maxCameraClampX;

    private double backgroundScrollSpeedX = 1.0;
    private double backgroundScrollSpeedY = 1.0;

    private final Map<String, DynamicRenderable> dynamicRenderables = new ConcurrentHashMap<>(2500);
    private volatile Map<String, Renderable> staticRenderables = new ConcurrentHashMap<>(100);
    // endregion

    // region Constructors
    public Renderer(View view) {
        this.view = view;

        this.setIgnoreRepaint(true);
        this.setCameraClampLimits();
    }
    // endregion

    // *** PUBLICS ***

    public boolean activate() {
        // Be sure all is ready to begin render!
        if (this.viewDimension == null) {
            throw new IllegalArgumentException("View dimensions not setted");
        }

        if ((this.viewDimension.x <= 0) || (this.viewDimension.y <= 0)) {
            throw new IllegalArgumentException("Canvas size error: ("
                    + this.viewDimension.x + "," + this.viewDimension.y + ")");
        }

        while (!this.isDisplayable()) {
            try {
                Thread.sleep(this.delayInMillis);
            } catch (InterruptedException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        this.setPreferredSize(
                new Dimension((int) this.viewDimension.x, (int) this.viewDimension.y));
        this.thread = new Thread(this);
        this.thread.setName("Renderer");
        this.thread.setPriority(Thread.NORM_PRIORITY + 2);
        this.thread.start();

        System.out.println("Renderer: Activated");
        return true;
    }

    // region adders (add***)
    public void addStaticRenderable(String entityId, String assetId) {
        Renderable renderable = new Renderable(entityId, assetId, this.imagesCache, this.currentFrame);
        this.staticRenderables.put(entityId, renderable);
    }

    public void addDynamicRenderable(String entityId, String assetId) {
        DynamicRenderable renderable = new DynamicRenderable(entityId, assetId, this.imagesCache, this.currentFrame);
        this.dynamicRenderables.put(entityId, renderable);
    }
    // endregion

    // region getters (get***)
    public Renderable getLocalPlayerRenderable() {
        String localPlayerId = this.view.getLocalPlayerId();

        if (localPlayerId == null || localPlayerId.isEmpty()) {
            return null; // ======= No player to follow =======>>
        }
        Renderable renderableLocalPlayer = this.dynamicRenderables.get(this.view.getLocalPlayerId());
        return renderableLocalPlayer;
    }
    // endregion

    // region notifiers (notify***)
    public void notifyDynamicIsDead(String entityId) {
        this.dynamicRenderables.remove(entityId);
    }
    // endregion

    // region setters (set***)
    public void setImages(BufferedImage background, Images images) {
        this.background = background;
        this.viBackground = null;

        this.images = images;
        this.imagesCache = new ImageCache(this.getGraphicsConfSafe(), this.images);
    }

    public void setViewDimension(DoubleVector viewDim) {
        this.viewDimension = viewDim;
        this.setCameraClampLimits();
        this.setPreferredSize(new Dimension((int) this.viewDimension.x, (int) this.viewDimension.y));
    }

    // endregion

    public void updateStaticRenderables(ArrayList<RenderDTO> renderablesData) {
        if (renderablesData == null) {
            return; // ========= Nothing to render by the moment ... =========>>
        }

        Map<String, Renderable> newRenderables = new java.util.concurrent.ConcurrentHashMap<>(this.staticRenderables);

        if (renderablesData.isEmpty()) {
            newRenderables.clear(); //
            this.staticRenderables = newRenderables;
            return;
        }

        // Update a renderable associated with each DBodyRenderInfoDTO
        long cFrame = this.currentFrame;
        for (RenderDTO renderableData : renderablesData) {
            String entityId = renderableData.entityId;
            if (entityId == null || entityId.isEmpty()) {
                continue;
            }

            Renderable renderable = newRenderables.get(entityId);
            if (renderable == null) {
                System.err.println("Renderer: Static renderable objet not found " + entityId);
            } else {
                renderable.update(renderableData, cFrame);
            }
        }

        newRenderables.entrySet().removeIf(e -> e.getValue().getLastFrameSeen() != cFrame);
        this.staticRenderables = newRenderables; // atomic swap
    }

    // *** PRIVATES ***

    // region drawers (draw***)
    private void drawDynamicRenderable(Graphics2D g) {
        // ArrayList<DynamicRenderDTO> renderablesData =
        // this.view.getDynamicRenderablesData(); // *+

        // this.updateDynamicRenderables(renderablesData);

        Map<String, DynamicRenderable> renderables = this.dynamicRenderables;
        for (DynamicRenderable renderable : renderables.values()) {
            renderable.paint(g, this.currentFrame);
        }
    }

    private void drawHUDs(Graphics2D g) {

        this.systemHUD.draw(g,
                this.fpsPerPeriod,
                String.format("%.0f", this.avgDrawMsPerPeriod) + " ms",
                this.imagesCache == null ? 0 : this.imagesCache.size(),
                String.format("%.0f", this.imagesCache == null ? 0 : this.imagesCache.getHitsPercentage()) + "%",
                this.view.getEntityAliveQuantity(),
                this.view.getEntityDeadQuantity(),
                this.currentFrame);

        PlayerRenderDTO playerData = this.view.getLocalPlayerRenderData();
        if (playerData != null) {
            this.playerHUD.draw(g, playerData.toObjectArray());
        }

        SpatialGridStatisticsRenderDTO spatialGridStats = this.view.getSpatialGridStatistics();
        if (spatialGridStats != null) {
            this.spatialGridHUD.draw(g, spatialGridStats.toObjectArray());
        }
    }

    private void drawStaticRenderables(Graphics2D g) {
        Map<String, Renderable> renderables = this.staticRenderables;

        for (Renderable renderable : renderables.values()) {
            renderable.paint(g, this.currentFrame);
        }
    }

    private void drawScene(BufferStrategy bs) {
        Graphics2D gg;

        this.drawStartNs = System.nanoTime();

        do {
            gg = (Graphics2D) bs.getDrawGraphics();
            try {
                // 1) BACKGROUND opaco (tileado) usando cameraX/cameraY ya actualizados
                gg.setComposite(AlphaComposite.Src); // Opaque

                this.drawTiledBackground(gg); // <- NUEVO: repinta VI cada frame
                gg.drawImage(this.viBackground, 0, 0, null);

                // 2) WORLD + HUD
                gg.setComposite(AlphaComposite.SrcOver); // With transparency

                this.drawWorldRenderables(gg); // <- no update aquí
                this.drawHUDs(gg);

            } finally {
                gg.dispose();
            }

            bs.show();
            Toolkit.getDefaultToolkit().sync();
        } while (bs.contentsLost());

        this.drawEndNs = System.nanoTime();
    }

    private void drawTiledBackground(Graphics2D g) {
        if (this.background == null || this.viewDimension == null)
            return;

        final int viewW = (int) this.viewDimension.x;
        final int viewH = (int) this.viewDimension.y;
        if (viewW <= 0 || viewH <= 0)
            return;

        final int tileW = this.background.getWidth(null);
        final int tileH = this.background.getHeight(null);
        if (tileW <= 0 || tileH <= 0)
            return;

        final double scrollX = this.cameraX * this.backgroundScrollSpeedX;
        final double scrollY = this.cameraY * this.backgroundScrollSpeedY;

        // Offset del tile en [-(tile-1)..0], estable con negativos
        final int offX = -Math.floorMod((int) Math.floor(scrollX), tileW);
        final int offY = -Math.floorMod((int) Math.floor(scrollY), tileH);

        // Empieza 1 tile antes para asegurar cobertura completa
        final int startX = offX - tileW;
        final int startY = offY - tileH;
        for (int x = startX; x < viewW + tileW; x += tileW) {
            for (int y = startY; y < viewH + tileH; y += tileH) {
                g.drawImage(this.background, x, y, null);
            }
        }
    }

    private void drawWorldRenderables(Graphics2D g) {
        // ** */

        AffineTransform defaultTransform = g.getTransform();
        g.translate(-this.cameraX, -this.cameraY);

        this.drawStaticRenderables(g);
        this.drawDynamicRenderable(g);

        g.setTransform(defaultTransform);
    }
    // endregion

    // region getters (get***)
    private GraphicsConfiguration getGraphicsConfSafe() {
        GraphicsConfiguration gc = getGraphicsConfiguration();
        if (gc == null) {
            gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration();
        }

        return gc;
    }

    private VolatileImage getVIBackground() {
        this.viBackground = this.getVolatileImage(
                this.viBackground,
                this.background,
                new Dimension((int) this.viewDimension.x, (int) this.viewDimension.y));

        return this.viBackground;

    }

    private VolatileImage getVolatileImage(
            VolatileImage vi, BufferedImage src, Dimension dim) {

        GraphicsConfiguration gc = this.getGraphicsConfSafe();

        if (vi == null || vi.getWidth() != dim.width || vi.getHeight() != dim.height
                || vi.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE) {
            // New volatile image
            vi = gc.createCompatibleVolatileImage(dim.width, dim.height, Transparency.OPAQUE);
        }

        int val;
        do {
            val = vi.validate(gc);
            if (val != VolatileImage.IMAGE_OK || vi.contentsLost()) {
                Graphics2D g = vi.createGraphics();
                g.drawImage(src, 0, 0, dim.width, dim.height, null);
                g.dispose();
            }
        } while (vi.contentsLost());

        return vi;
    }
    // endregion

    // Metrics updated per period
    private void monitoringPerPeriod() {

        this.framesPerPeriod++;
        long now = System.nanoTime();
        long elapsed = now - this.monitoringPeriodStartNs;

        // Acum draw time in the period
        long lastDrawTime = this.drawEndNs - this.drawStartNs;
        this.acumDrawNsPerPeriod += lastDrawTime;

        // Monitoring at especific rate ..
        if (elapsed >= MONITORING_PERIOD_NS) {
            // Update period metrics
            this.fpsPerPeriod = (int) Math.round(framesPerPeriod * (1_000_000_000.0 / elapsed));
            this.avgDrawMsPerPeriod = (this.acumDrawNsPerPeriod / (double) this.framesPerPeriod) / 1_000_000.0D;

            // Reset acumulators to start new period
            this.framesPerPeriod = 0L;
            this.acumDrawNsPerPeriod = 0L;
            this.monitoringPeriodStartNs = now;

        }
    }

    // region setters (set***)
    private void setCameraClampLimits() {
        DoubleVector woldDim = this.view.getWorldDimension();

        if (woldDim == null || this.viewDimension == null) {
            this.maxCameraClampX = 0.0;
            this.maxCameraClampY = 0.0;
            return; // ======= No world or view dimensions info ======= >>
        }

        this.maxCameraClampX = Math.max(0.0, woldDim.x - this.viewDimension.x);
        this.maxCameraClampY = Math.max(0.0, woldDim.y - this.viewDimension.y);
    }
    // endregion

    // region updaters (update***)
    private void updateCamera() {
        Renderable localPlayerRenderable = this.getLocalPlayerRenderable();
        DoubleVector worldDim = this.view.getWorldDimension();

        if (localPlayerRenderable == null || this.viewDimension == null || worldDim == null) {
            return; // ======== No player or data to follow =======>>
        }

        RenderDTO playerData = localPlayerRenderable.getRenderableValues();
        double desiredX = playerData.posX - (this.viewDimension.x / 2.0d);
        double desiredY = playerData.posY - (this.viewDimension.y / 2.0d);

        this.cameraX += (desiredX - this.cameraX);
        this.cameraY += (desiredY - this.cameraY);

        // // Clamp when camera goes out of world limits
        this.cameraX = clamp(cameraX, 0.0, this.maxCameraClampX);
        this.cameraY = clamp(cameraY, 0.0, this.maxCameraClampY);
    }

    private void updateDynamicRenderables(ArrayList<DynamicRenderDTO> renderablesData) {
        if (renderablesData == null || renderablesData.isEmpty()) {
            // If no objects are alive this frame, clear the snapshot entirely
            this.dynamicRenderables.clear();
            return; // ========= Nothing to render by the moment ... =========>>
        }

        // Update or create a renderable associated with each DBodyRenderInfoDTO
        long cFrame = this.currentFrame;
        for (DynamicRenderDTO renderableData : renderablesData) {
            String entityId = renderableData.entityId;
            if (entityId == null || entityId.isEmpty()) {
                continue;
            }

            DynamicRenderable renderable = this.dynamicRenderables.get(entityId);
            if (renderable != null) {
                // Existing renderable → update its snapshot and sprite if needed
                renderable.update(renderableData, cFrame);
            }
        }

        // Remove renderables not updated this frame (i.e., objects no longer alive)
        this.dynamicRenderables.entrySet().removeIf(entry -> entry.getValue().getLastFrameSeen() != cFrame);
    }
    // endregion

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    // *** PRIVATE STATIC ***

    // *** INTERFACE IMPLEMENTATIONS ***

    // region Runnable
    @Override
    public void run() {
        this.createBufferStrategy(3);
        BufferStrategy bs = getBufferStrategy();

        // Max one frame per loop
        this.lastFrameStartNs = System.nanoTime();
        while (true) {
            this.frameStartNs = System.nanoTime();
            this.frameDtNs = this.frameStartNs - this.lastFrameStartNs;
            this.lastFrameStartNs = this.frameStartNs;

            EngineState engineState = this.view.getEngineState();
            if (engineState == EngineState.STOPPED) {
                break;
            }

            if (engineState == EngineState.ALIVE) { // TO-DO Pause condition
                this.currentFrame++;

                // Recover snapshot of dynamic renderables data
                ArrayList<DynamicRenderDTO> renderablesData = this.view.getDynamicRenderablesData();

                // Update dynamic renderables states using the snapshot
                this.updateDynamicRenderables(renderablesData);

                // Update camera position to follow local player using the latest data
                this.updateCamera();

                this.drawScene(bs);

                this.monitoringPerPeriod();
            }

            try {
                Thread.sleep(DELAY_IN_MILLIS);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }

            // Monitoring frame time per frame (inclusive when paused)
            this.frameEndNs = System.nanoTime();
            this.frameTimeNs = this.frameEndNs - this.frameStartNs;
        }
    }
    // endregion
}
