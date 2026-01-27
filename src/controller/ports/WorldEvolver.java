package controller.ports;

import java.awt.Dimension;

import world.ports.DefEmitterDTO;
import world.ports.DefWeaponDTO;

public interface WorldEvolver {

    public void addDynamicBody(String assetId, double size, double posX, double posY,
            double speedX, double speedY, double accX, double accY,
            double angle, double angularSpeed, double angularAcc, double thrust);

    public String addPlayer(String assetId, double size, double posX, double posY,
            double speedX, double speedY, double accX, double accY,
            double angle, double angularSpeed, double angularAcc, double thrust);

    public void equipWeapon(String playerId, DefWeaponDTO weaponDef, int shootingOffset);

    public void equipTrail(
            String playerId, DefEmitterDTO bodyEmitterDef);

    public Dimension getWorldDimension();

    public EngineState getEngineState();

    public void setLocalPlayer(String playerId);
}
