package game.core;

import java.util.ArrayList;
import java.util.Random;

import controller.ports.WorldInitializer;
import world.ports.DefEmitterDTO;
import world.ports.DefItem;
import world.ports.DefItemDTO;
import world.ports.DefWeaponDTO;
import world.ports.WorldDefinition;

public abstract class AbstractLevelGenerator {

    // region Fields
    private final Random rnd = new Random();
    private final DefItemMaterializer defItemMaterializer = new DefItemMaterializer();
    private final WorldInitializer worldInitializer;
    private final WorldDefinition worldDefinition;
    // endregion

    // *** CONSTRUCTORS ***

    protected AbstractLevelGenerator(WorldInitializer worldInitializer, WorldDefinition worldDef) {
        if (worldInitializer == null) {
            throw new IllegalArgumentException("WorldInitializer cannot be null.");
        }
        if (worldDef == null) {
            throw new IllegalArgumentException("WorldDefinition cannot be null.");
        }

        this.worldInitializer = worldInitializer;
        this.worldDefinition = worldDef;

        this.createWorld();
    }

    // *** PROTECTED ABSTRACT ***

    protected abstract void createStatics();

    protected abstract void createDecorators();

    protected abstract void createPlayers();

    // *** PROTECTED ***

    protected void addDecoratorIntoTheGame(DefItemDTO deco) {
        this.worldInitializer.addDecorator(deco.assetId, deco.size, deco.posX, deco.posY, deco.angle);
    }

    protected String addLocalPlayerIntoTheGame(
            DefItemDTO bodyDef, ArrayList<DefWeaponDTO> weaponDefs,
            ArrayList<DefEmitterDTO> trailDefs) {

        String playerId = this.worldInitializer.addPlayer(
                bodyDef.assetId, bodyDef.size,
                bodyDef.posX, bodyDef.posY,
                bodyDef.speedX, bodyDef.speedY,
                0, 0,
                bodyDef.angle, bodyDef.angularSpeed,
                0,
                bodyDef.thrust);

        if (playerId == null) {
            throw new IllegalStateException("Failed to create local player.");
        }

        this.equipEmitters(playerId, trailDefs);
        this.equipWeapons(playerId, weaponDefs);

        this.worldInitializer.setLocalPlayer(playerId);
        return playerId;
    }

    protected void addStaticIntoTheGame(DefItemDTO bodyDef) {
        this.worldInitializer.addStaticBody(
                bodyDef.assetId, bodyDef.size,
                bodyDef.posX, bodyDef.posY,
                bodyDef.angle);
    }

    protected void equipEmitters(String entityId, ArrayList<DefEmitterDTO> emitterDefs) {
        for (DefEmitterDTO emitterDef : emitterDefs) {
            this.worldInitializer.equipTrail(
                    entityId, emitterDef);
        }
    }

    protected void equipWeapons(String entityId, ArrayList<DefWeaponDTO> weaponDefs) {
        for (DefWeaponDTO weaponDef : weaponDefs) {
            this.worldInitializer.equipWeapon(
                    entityId, weaponDef, 0);
        }
    }

    protected final DefItemDTO defItemToDTO(DefItem defitem) {
        return this.defItemMaterializer.defItemToDTO(defitem);
    }

    protected WorldDefinition getWorldDefinition() {
        return this.worldDefinition;
    }

    protected WorldInitializer getWorldInitializer() {
        return this.worldInitializer;
    }

    protected final double randomDoubleBetween(double minInclusive, double maxInclusive) {
        if (maxInclusive < minInclusive) {
            throw new IllegalArgumentException("maxInclusive must be >= minInclusive");
        }
        if (maxInclusive == minInclusive) {
            return minInclusive;
        }
        return minInclusive + (this.rnd.nextDouble() * (maxInclusive - minInclusive));
    }

    // *** PRIVATE ***

    // Standard world creation pipeline.
    private final void createWorld() {
        this.worldInitializer.loadAssets(this.worldDefinition.gameAssets);

        this.createDecorators();
        this.createStatics();
        this.createPlayers();
    }

}
