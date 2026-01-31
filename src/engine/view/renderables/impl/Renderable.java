package engine.view.renderables.impl;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import engine.utils.images.ImageCache;
import engine.view.renderables.ports.RenderDTO;

public class Renderable {

    private final String entityId;
    private final String assetId;
    private final ImageCache cache;

    private long lastFrameSeen;
    private RenderDTO renderableValues = null;
    private BufferedImage image = null;

    public Renderable(RenderDTO renderInfo, String assetId, ImageCache cache, long currentFrame) {
        if (assetId == null || assetId.isEmpty()) {
            throw new IllegalArgumentException("Asset ID not set");
        }
        if (cache == null) {
            throw new IllegalArgumentException("Image cache not set");
        }

        this.entityId = renderInfo.entityId;
        this.assetId = assetId;
        this.lastFrameSeen = currentFrame;
        this.renderableValues = renderInfo;
        this.cache = cache;
        this.updateImageFromCache(this.assetId, (int) renderInfo.size, renderInfo.angle);
    }

    public Renderable(String entityId, String assetId, ImageCache cache, long currentFrame) {
        if (entityId == null || entityId.isEmpty()) {
            throw new IllegalArgumentException("Entity ID not set");
        }
        if (assetId == null || assetId.isEmpty()) {
            throw new IllegalArgumentException("Asset ID not set");
        }
        if (cache == null) {
            throw new IllegalArgumentException("Image cache not set");
        }

        this.entityId = entityId;
        this.assetId = assetId;
        this.lastFrameSeen = currentFrame;
        this.cache = cache;
        this.image = null;
        this.renderableValues = null;
    }

    /**
     * PUBLICS
     */
    public long getLastFrameSeen() {
        return this.lastFrameSeen;
    }

    public String getAssetId() {
        return this.assetId;
    }

    public String getEntityId() {
        return this.entityId;
    }

    public RenderDTO getRenderableValues() {
        return this.renderableValues;
    }

    public BufferedImage getImage() {
        return this.image;
    }

    public void update(RenderDTO renderInfo, long currentFrame) {
        this.updateImageFromCache(this.assetId, (int) renderInfo.size, renderInfo.angle);
        this.lastFrameSeen = currentFrame;
        this.renderableValues = renderInfo;
    }

    public void paint(Graphics2D g, long currentFrame) {

        if (this.image == null) {
            return;
        }

        // Save the original (NOT rotated) transform
        AffineTransform old = g.getTransform();

        final double posX = this.renderableValues.posX;
        final double posY = this.renderableValues.posY;
        final double angleDeg = this.renderableValues.angle;

        // Using the REAL size of the sprite for the offset
        final double halfW = this.image.getWidth(null) * 0.5;
        final double halfH = this.image.getHeight(null) * 0.5;

        final int drawX = (int) (posX - halfW);
        final int drawY = (int) (posY - halfH);

        g.rotate(Math.toRadians(angleDeg), posX, posY);

        g.drawImage(this.image, drawX, drawY, null);

        // Restore original (NOT rotated) transform
        g.setTransform(old);
    }

    public void updateImageFromCache(RenderDTO entityInfo) {
        this.updateImageFromCache(this.assetId, (int) entityInfo.size, entityInfo.angle);
    }

    private boolean updateImageFromCache(String assetId, int size, double angle) {
        boolean imageNeedsUpdate = this.image == null
                || this.renderableValues == null
                || !this.assetId.equals(assetId)
                || this.renderableValues.size != size
                || (int) this.renderableValues.angle != (int) angle;

        if (imageNeedsUpdate) {
            int normalizedAngle = ((int) angle % 360 + 360) % 360;
            this.image = this.cache.getImage(normalizedAngle, assetId, size);

            return true; // ====
        }

        return false;
    }
}
