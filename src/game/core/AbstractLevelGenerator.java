package game.core;

import java.util.Random;

import controller.ports.WorldInitializer;
import world.ports.DefItem;
import world.ports.DefItemDTO;
import world.ports.DefItemPrototypeDTO;
import world.ports.WorldDefinition;

public abstract class AbstractLevelGenerator {

    // region Fields
    protected final Random rnd = new Random();
    protected final WorldInitializer worldInitializer;
    protected final WorldDefinition worldDefinition;
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

    // *** PROTECTED ***

    /**
     * Standard world creation pipeline.
     * Subclasses decide which sections they want to create.
     */
    protected void createWorld() {
        this.worldInitializer.loadAssets(this.worldDefinition.gameAssets);

        this.createSpaceDecorators();
        this.createStaticBodies();
    }

    /**
     * Default: no-op. Override if generator needs it.
     */
    protected void createStaticBodies() {
        // no-op by default
    }

    /**
     * Default: no-op. Override if generator needs it.
     */
    protected void createSpaceDecorators() {
        // no-op by default
    }

    // *** PROTECTED ***

    protected final DefItemDTO materialize(DefItem defitem) {
        if (defitem == null) {
            throw new IllegalArgumentException("DefItem cannot be null.");
        }

        if (defitem instanceof DefItemDTO concreteItem) {
            return concreteItem;
        }

        if (defitem instanceof DefItemPrototypeDTO prototypeItem) {
            double size = this.randomDoubleBetween(prototypeItem.minSize, prototypeItem.maxSize);
            double angle = this.randomDoubleBetween(prototypeItem.minAngle, prototypeItem.maxAngle);
            double x = this.randomDoubleBetween(prototypeItem.posMinX, prototypeItem.posMaxX);
            double y = this.randomDoubleBetween(prototypeItem.posMinY, prototypeItem.posMaxY);

            return new DefItemDTO(
                    prototypeItem.assetId, size, angle, x, y, prototypeItem.density);
        }

        // sealed -> debería ser inalcanzable salvo que cambie permits
        throw new IllegalStateException(
            "Unsupported DefItem implementation: " + defitem.getClass().getName());
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
}
