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
            // Update image cache with current animation frame
            this.updateImageFromCache(this.currentFrameAssetId, (int) current.size, current.angle);
            this.lastFrameSeen = currentFrame;
            this.renderData = current;
            return;
        }
        
        // First time: renderInfo is from pool, keep it and update image
        this.updateImageFromCache(this.currentFrameAssetId, (int) renderInfo.size, renderInfo.angle);
        this.lastFrameSeen = currentFrame;
        this.renderData = renderInfo;
    }
    
    /**
     * Paint the current animation frame
     */
    @Override
    public void paint(Graphics2D g, long currentFrame) {
        updateAnimationFrame();
        super.paint(g, currentFrame);
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
