package engine.world.core;

import engine.assets.core.AssetCatalog;
import engine.assets.ports.AnimatedAssetInfoDTO;
import engine.assets.ports.AssetInfoDTO;
import engine.assets.ports.AssetType;
import gameworld.ProjectAssets;

public final class WorldAssetsRegister {

    // endregion Fields
    private final ProjectAssets projectAssets;
    private final AssetCatalog gameAssets;
    // endregion

    // *** CONSTRUCTOR ***

    public WorldAssetsRegister(ProjectAssets projectAssets, AssetCatalog gameAssets) {
        if (projectAssets == null) {
            throw new IllegalArgumentException("ProjectAssets cannot be null.");
        }
        if (gameAssets == null) {
            throw new IllegalArgumentException("AssetCatalog (gameAssets) cannot be null.");
        }
        this.projectAssets = projectAssets;
        this.gameAssets = gameAssets;
    }

    // *** PUBLICS ***

    public String pickRandomAssetId(AssetType assetType) {
        if (assetType == null) {
            throw new IllegalArgumentException("AssetType cannot be null.");
        }

        String id = this.projectAssets.catalog.randomId(assetType);
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("ProjectAssets.catalog.randomId(" + assetType + ") returned null/blank.");
        }

        return id;
    }

    public String pickAndRegisterRandomAssetId(AssetType assetType) {
        if (assetType == null) {
            throw new IllegalArgumentException("AssetType cannot be null.");
        }

        String id = this.projectAssets.catalog.randomId(assetType);
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("ProjectAssets.catalog.randomId(" + assetType + ") returned null/blank.");
        }

        this.registerAssetId(id);
        return id;
    }

    public void registerAssetId(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("assetId cannot be null/blank.");
        }

        // Check if it's a regular asset first
        AssetInfoDTO info = this.projectAssets.catalog.get(assetId);
        if (info != null) {
            this.gameAssets.register(info);
            return;
        }
        
        // Check if it's an animation
        AnimatedAssetInfoDTO animationInfo = this.projectAssets.catalog.getAnimation(assetId);
        if (animationInfo != null) {
            // Register the animation in gameAssets
            this.gameAssets.registerAnimation(animationInfo);
            
            // Also register all frame assets that compose this animation
            for (String frameAssetId : animationInfo.frameAssetIds) {
                AssetInfoDTO frameInfo = this.projectAssets.catalog.get(frameAssetId);
                if (frameInfo != null) {
                    this.gameAssets.register(frameInfo);
                    // Silent: registered animation frame
                } else {
                    System.err.println("WorldAssetsRegister: frame '" + frameAssetId + "' NOT FOUND in projectAssets for animation '" + assetId + "'");
                }
            }
            return;
        }
        
        // Not found in either catalog
        throw new IllegalArgumentException("assetId not found in ProjectAssets catalog: " + assetId);
    }
}