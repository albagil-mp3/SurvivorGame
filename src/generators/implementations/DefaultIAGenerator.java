package generators.implementations;

import controller.ports.WorldEvolver;
import generators.ports.IAConfigDTO;
import world.ports.WorldDefinition;

public class DefaultIAGenerator
        extends generators.implementations.ia.AsteroidSpawnIAGenerator {

    // No additional functionality
    //
    // Just a default naming wrapper :-)
    //
    // Intended for easier swapping IA generator implementations.
    //
    // NO MODIFICATIONS REQUIRED IN in Main.java

    // *** CONSTRUCTOR *** //

    public DefaultIAGenerator(WorldEvolver controller,
            WorldDefinition worldDefinition, IAConfigDTO lifeConfig) {
        super(controller, worldDefinition, lifeConfig);
    }
}
