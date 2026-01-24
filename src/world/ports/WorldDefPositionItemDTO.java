package world.ports;

public class WorldDefPositionItemDTO extends WorldDefItemDTO {

    // region Fields
    public final double posX;
    public final double posY;
    public final double size;
    public final double angle;
    public final double mass;
    // endregion

    // *** CONSTRUCTOR ***

    public WorldDefPositionItemDTO(
            String assetId, double size, double angle,
            double posX, double posY, double mass) {

        super(assetId);

        this.posX = posX;
        this.posY = posY;
        this.size = size;
        this.angle = angle;
        this.mass = mass;
    }
}
