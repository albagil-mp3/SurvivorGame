package world.ports;


public class WorldDefBackgroundDTO {

    public final String assetId;
    public final double scrollSpeedX;
    public final double scrollSpeedY;


    public WorldDefBackgroundDTO(String assetId, double scrollSpeedX, double scrollSpeedY) {

        this.assetId = assetId;
        this.scrollSpeedX = scrollSpeedX;
        this.scrollSpeedY = scrollSpeedY;
    }
}
