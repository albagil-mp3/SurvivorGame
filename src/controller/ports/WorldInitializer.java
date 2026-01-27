package controller.ports;

import utils.assets.core.AssetCatalog;
import world.ports.DefEmitterDTO;
import world.ports.DefWeaponDTO;

public interface WorldInitializer {

    public void addDynamicBody(String assetId, double size, double posX, double posY,
            double speedX, double speedY, double accX, double accY,
            double angle, double angularSpeed, double angularAcc, double thrust);

    public String addPlayer(String assetId, double size, double posX, double posY,
            double speedX, double speedY, double accX, double accY,
            double angle, double angularSpeed, double angularAcc, double thrust);

    public void equipWeapon(String playerId, DefWeaponDTO weaponDef, int shootingOffset);

    public void equipTrail(
            String playerId, DefEmitterDTO bodyEmitterDef);

    public EngineState getEngineState();

    public void setLocalPlayer(String playerId);

    public void loadAssets(AssetCatalog assets);

    public void addStaticBody(String assetId, double size, double posX, double posY, double angle);

    public void addDecorator(String assetId, double size, double posX, double posY, double angle);

}
