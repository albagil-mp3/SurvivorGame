package engine.view.renderables.impl;

import java.awt.Graphics2D;

import engine.assets.ports.AnimatedAssetInfoDTO;
import engine.utils.images.ImageCache;
import engine.view.renderables.ports.DynamicRenderDTO;
import engine.view.renderables.ports.RenderDTO;

/**
 * AnimatedRenderable
 * 
 * Extends DynamicRenderable to support animated sprites with multiple frames.
 * Updates the displayed frame based on elapsed time and the animation's frame duration.
 */
public class AnimatedRenderable extends DynamicRenderable {

    private final AnimatedAssetInfoDTO animationInfo;
    private long animationStartTime;
    private String currentFrameAssetId;
    private int lastFrameIndex = -1;
    
    // Weapon overlay support for player ships
    private boolean isPlayerShip = false;
    private String weaponAssetId = null;
    private int weaponSize = 0;
    
    /**
     * Creates an animated renderable for dynamic entities
     * 
     * @param renderData Initial render data
     * @param animationInfo Animation information with frames
     * @param cache Image cache for frame lookups
     * @param currentFrame Current game frame number
     */
    public AnimatedRenderable(
            DynamicRenderDTO renderData, 
            AnimatedAssetInfoDTO animationInfo, 
            ImageCache cache, 
            long currentFrame) {
        
        // Initialize parent with first frame of animation
        super(renderData, animationInfo.getFrameAssetId(0), cache, currentFrame);
        
        this.animationInfo = animationInfo;
        this.animationStartTime = System.currentTimeMillis();
        this.currentFrameAssetId = animationInfo.getFrameAssetId(0);
    }
    
    /**
     * Creates an animated renderable for static entities
     * 
     * @param entityId Entity identifier
     * @param animationInfo Animation information with frames
     * @param cache Image cache for frame lookups
     * @param currentFrame Current game frame number
     */
    public AnimatedRenderable(
            String entityId,
            AnimatedAssetInfoDTO animationInfo,
            ImageCache cache,
            long currentFrame) {
        
        // Initialize parent with first frame of animation
        super(entityId, animationInfo.getFrameAssetId(0), cache, currentFrame);
        
        this.animationInfo = animationInfo;
        this.animationStartTime = System.currentTimeMillis();
        this.currentFrameAssetId = animationInfo.getFrameAssetId(0);
        
        // Pre-load first frame at a default size to avoid null image on first paint
        // The actual size will be set when update() is called with proper renderData
        // This prevents blank screen while waiting for first update
        // Note: This initial image will be replaced on first update() call
    }
    
    /**
     * Update the renderable with new render data and advance animation
     */
    @Override
    public void update(DynamicRenderDTO renderInfo, long currentFrame) {
        updateAnimationFrame();
        
        // Handle the same way as DynamicRenderable parent class
        DynamicRenderDTO current = (DynamicRenderDTO) this.getRenderData();
        if (current != null && renderInfo != null) {
            current.updateFrom(renderInfo);
            // Update image cache with current animation frame - always angle 0 for player ship
            double imageAngle = isPlayerShip ? 0.0 : current.angle;
            this.updateImageFromCache(this.currentFrameAssetId, (int) current.size, imageAngle);
            this.lastFrameSeen = currentFrame;
            this.renderData = current;
            return;
        }
        
        // First time: renderInfo is from pool, keep it and update image
        double imageAngle = isPlayerShip ? 0.0 : renderInfo.angle;
        this.updateImageFromCache(this.currentFrameAssetId, (int) renderInfo.size, imageAngle);
        this.lastFrameSeen = currentFrame;
        this.renderData = renderInfo;
    }
    
    /**
     * Paint the current animation frame
     */
    @Override
    public void paint(Graphics2D g, long currentFrame) {
        updateAnimationFrame();
        
        // Draw ship (always at angle 0 if it's a player ship, otherwise use actual angle)
        RenderDTO renderData = this.getRenderData();
        if (renderData == null || this.image == null) {
            return;
        }
        
        int x = (int) (renderData.posX - (this.image.getWidth(null) * 0.5d));
        int y = (int) (renderData.posY - (this.image.getHeight(null) * 0.5d));
        
        g.drawImage(this.image, x, y, null);
        
        // If this is a player ship with a weapon, draw weapon overlay rotated
        if (isPlayerShip && weaponAssetId != null && weaponSize > 0 && this.cache != null) {
            drawWeaponOverlay(g, renderData);
        }
    }
    
    /**
     * Draw weapon overlay rotated to match player angle
     */
    private void drawWeaponOverlay(Graphics2D g, RenderDTO renderData) {
        // Get weapon image rotated to player angle
        int angle = (int) renderData.angle;
        java.awt.image.BufferedImage weaponImg = this.cache.getImage(angle, weaponAssetId, weaponSize);
        
        if (weaponImg == null) {
            System.err.println("WARNING: Weapon image is NULL! Asset: " + weaponAssetId + 
                             ", Angle: " + angle + ", Size: " + weaponSize);
            return;
        }
        
        // Draw weapon centered on ship
        int wx = (int) (renderData.posX - (weaponImg.getWidth() * 0.5));
        int wy = (int) (renderData.posY - (weaponImg.getHeight() * 0.5));
        g.drawImage(weaponImg, wx, wy, null);
    }
    
    /**
     * Configure this as a player ship with weapon overlay
     */
    public void setAsPlayerShip(String weaponAssetId, int weaponSize) {
        this.isPlayerShip = true;
        this.weaponAssetId = weaponAssetId;
        this.weaponSize = weaponSize;
        System.out.println("AnimatedRenderable: Configured as player ship with weapon: " + 
                          weaponAssetId + ", size: " + weaponSize);
    }
    
    /**
     * Get the animation information
     */
    public AnimatedAssetInfoDTO getAnimationInfo() {
        return this.animationInfo;
    }
    
    /**
     * Reset the animation to the first frame and restart timing
     */
    public void resetAnimation() {
        this.animationStartTime = System.currentTimeMillis();
        this.lastFrameIndex = -1;
        this.currentFrameAssetId = animationInfo.getFrameAssetId(0);
    }
    
    /**
     * PRIVATE METHODS
     */
    
    /**
     * Update the current frame based on elapsed time
     */
    private void updateAnimationFrame() {
        long elapsedTime = System.currentTimeMillis() - animationStartTime;
        int frameIndex = animationInfo.getFrameIndexAtTime(elapsedTime);
        
        // Only update if frame changed
        if (frameIndex != lastFrameIndex) {
            lastFrameIndex = frameIndex;
            currentFrameAssetId = animationInfo.getFrameAssetId(frameIndex);
        }
    }
}
