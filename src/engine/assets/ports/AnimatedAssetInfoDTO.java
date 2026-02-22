package engine.assets.ports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AnimatedAssetInfoDTO
 * 
 * Represents an animated asset composed of multiple frames.
 * Each frame is referenced by its assetId in the AssetCatalog.
 */
public class AnimatedAssetInfoDTO {
    public final String animationId;
    public final List<String> frameAssetIds;
    public final AssetType type;
    public final AssetIntensity intensity;
    public final int frameDurationMs;
    
    /**
     * @param animationId Unique identifier for this animation
     * @param frameAssetIds List of assetIds that compose the animation frames
     * @param type Type of asset
     * @param intensity Visual intensity level
     * @param frameDurationMs Duration of each frame in milliseconds
     */
    public AnimatedAssetInfoDTO(
            String animationId, 
            List<String> frameAssetIds,
            AssetType type,
            AssetIntensity intensity,
            int frameDurationMs) {
        
        if (animationId == null || animationId.isEmpty()) {
            throw new IllegalArgumentException("Animation ID cannot be null or empty");
        }
        if (frameAssetIds == null || frameAssetIds.isEmpty()) {
            throw new IllegalArgumentException("Frame asset IDs cannot be null or empty");
        }
        if (frameDurationMs <= 0) {
            throw new IllegalArgumentException("Frame duration must be positive");
        }
        
        this.animationId = animationId;
        this.frameAssetIds = Collections.unmodifiableList(new ArrayList<>(frameAssetIds));
        this.type = type;
        this.intensity = intensity;
        this.frameDurationMs = frameDurationMs;
    }
    
    /**
     * Get the asset ID for a specific frame index
     */
    public String getFrameAssetId(int frameIndex) {
        if (frameIndex < 0 || frameIndex >= frameAssetIds.size()) {
            throw new IndexOutOfBoundsException("Frame index out of bounds: " + frameIndex);
        }
        return frameAssetIds.get(frameIndex);
    }
    
    /**
     * Get total number of frames in this animation
     */
    public int getFrameCount() {
        return frameAssetIds.size();
    }
    
    /**
     * Calculate which frame should be displayed at a given time
     */
    public int getFrameIndexAtTime(long elapsedTimeMs) {
        int frameIndex = (int) ((elapsedTimeMs / frameDurationMs) % frameAssetIds.size());
        return frameIndex;
    }
    
    /**
     * Get the asset ID that should be displayed at a given time
     */
    public String getFrameAssetIdAtTime(long elapsedTimeMs) {
        int frameIndex = getFrameIndexAtTime(elapsedTimeMs);
        return getFrameAssetId(frameIndex);
    }
}
