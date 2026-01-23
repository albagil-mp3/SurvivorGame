package generators.implementations;

import controller.ports.WorldInitializer;
import world.ports.WorldDefinition;

public class DefaultLevelGenerator extends generators.implementations.level.SimpleRandomLevelGenerator {

    // No additional functionality
    //
    // Just a default naming wrapper :-)
    //
    // Intended for easier swapping LEVEL generator implementations.
    //
    // NO MODIFICATIONS REQUIRED IN Main.java


    // *** CONSTRUCTOR *** //

    public DefaultLevelGenerator(WorldInitializer worldInitializer, WorldDefinition worldDef) {
        super(worldInitializer, worldDef);
    }

}
