package model.trails.ports;

public class TrailDto {

    public final String trailAssetId;
    public final double trailSize;
    public final double shootingOffset;
    public final int emisionRate;
    public final double maxLifeTime;

    public TrailDto(
            String trailAssetId,
            double trailSize,
            double shootingOffset,
            int emisionRate,
            double maxLifeTime) {

        this.trailAssetId = trailAssetId;
        this.trailSize = trailSize;
        this.shootingOffset = shootingOffset;
        this.emisionRate = emisionRate;
        this.maxLifeTime = maxLifeTime;
    }

    // Clone constructor
    public TrailDto(TrailDto other) {
        this.trailAssetId = other.trailAssetId;
        this.trailSize = other.trailSize;
        this.shootingOffset = other.shootingOffset;
        this.emisionRate = other.emisionRate;
        this.maxLifeTime = other.maxLifeTime;
    }
}
