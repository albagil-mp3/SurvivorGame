package game.implementations.ai;

import java.util.ArrayList;

import controller.ports.WorldEvolver;
import game.core.AbstractIAGenerator;
// import generators.ports.AIConfigDTO;
import world.ports.DefItem;
import world.ports.DefItemDTO;
import world.ports.WorldDefinition;

public class AIBasicSpawner extends AbstractIAGenerator {

    // region Fields
    private final ArrayList<DefItem> asteroidDefs;
    // endregion

    // *** CONSTRUCTORS ***

    public AIBasicSpawner(
            WorldEvolver worldEvolver, WorldDefinition worldDefinition,
            int maxCreationDelay) {

        super(worldEvolver, worldDefinition, maxCreationDelay);

        this.asteroidDefs = this.worldDefinition.asteroids;
    }

    // *** PROTECTED (alphabetical order) ***

    @Override
    protected String getThreadName() {
        return "Asteroid IA generator (AIBasicSpawner)";
    }

    @Override
    protected void onActivate() {
        this.createPlayers();
    }

    @Override
    protected void tickAlive() {
        // Select a random asteroid definition
        DefItem defItem = this.asteroidDefs.get(
                this.rnd.nextInt(this.asteroidDefs.size()));

        this.addDynamic(defItem);
    }

    // *** PRIVATE (alphabetic order) ***

    private void addDynamic(DefItem defItem) {
        // If defItem is a prototype, we need to convert it to a DTO
        // to resolve range-based properties ...
        DefItemDTO bodyDef = this.toDTO(defItem);

        // At this place you can modify position, 
        // speed, thrust, etc. as needed
        // or you can accept definition values 
        // as they came form world definition.
        // ... or do nothing.

        // Injecting dynamic body into the game
        this.addDynamicIntoTheGame(bodyDef);
    }

    private void createPlayers() {
        ArrayList<DefItem> shipDefs = this.worldDefinition.spaceships;
        String playerId = null;

        for (DefItem def : shipDefs) {
            DefItemDTO body = this.toDTO(def);

            playerId = this.worldEvolver.addPlayer(
                    body.assetId, body.size,
                    500, 200,
                    0, 0,
                    0, 0,
                    0,
                    this.randomAngularSpeed(270),
                    0, 0);

            System.out.println("Created player with ID: " + playerId);

            this.worldEvolver.addWeaponToPlayer(
                    playerId, this.worldDefinition.bulletWeapons.get(0), 0);
            this.worldEvolver.addWeaponToPlayer(
                    playerId, this.worldDefinition.burstWeapons.get(0), 0);
            this.worldEvolver.addWeaponToPlayer(
                    playerId, this.worldDefinition.missileLaunchers.get(0), -15);
            this.worldEvolver.addWeaponToPlayer(
                    playerId, this.worldDefinition.mineLaunchers.get(0), 15);

            this.worldEvolver.bodyEquipTrail(
                    playerId, this.worldDefinition.trailEmitters.get(0));
        }

        if (playerId == null) {
            System.out.println("AIBasicSpawner: No player created!");
            return;
        }

        this.worldEvolver.setLocalPlayer(playerId);
    }

}
