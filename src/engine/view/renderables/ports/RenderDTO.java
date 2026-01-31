package engine.view.renderables.ports;

public class RenderDTO {

    public final String entityId;
    public final double posX;
    public final double posY;
    public final double angle;
    public final double size;
    public long timestamp;

    public RenderDTO(
            String entityId, double posX, double posY, double angle, double size, long timestamp) {

        this.entityId = entityId;
        this.posX = posX;
        this.posY = posY;
        this.angle = angle;
        this.size = size;
        this.timestamp = timestamp;
    }
}
