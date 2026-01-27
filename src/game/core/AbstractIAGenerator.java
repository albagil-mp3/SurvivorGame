package game.core;

import java.util.Random;

import controller.ports.EngineState;
import controller.ports.WorldEvolver;
import utils.helpers.DoubleVector;
import world.ports.DefItem;
import world.ports.DefItemDTO;
import world.ports.DefItemPrototypeDTO;
import world.ports.WorldDefinition;

public abstract class AbstractIAGenerator implements Runnable {

    // region Fields
    protected final Random rnd = new Random();
    protected final WorldEvolver worldEvolver;
    protected final WorldDefinition worldDefinition;
    protected final int maxCreationDelay;
    private Thread thread;
    // endregion

    // *** CONSTRUCTORS ***

    protected AbstractIAGenerator(
            WorldEvolver worldEvolver,
            WorldDefinition worldDefinition,
            int maxCreationDelay) {

        if (worldEvolver == null) {
            throw new IllegalArgumentException("WorldEvolver cannot be null.");
        }
        if (worldDefinition == null) {
            throw new IllegalArgumentException("WorldDefinition cannot be null.");
        }

        this.worldEvolver = worldEvolver;
        this.worldDefinition = worldDefinition;
        this.maxCreationDelay = maxCreationDelay;
    }

    // *** PUBLIC ***

    public void activate() {
        this.thread = new Thread(this);
        this.thread.setName(this.getThreadName());
        this.thread.setPriority(Thread.MIN_PRIORITY);
        this.thread.start();

        this.onActivate();

        System.out.println(this.getThreadName() + " activated!");
    }

    // *** PROTECTED (alphabetical order) ***

    protected void addDynamicIntoTheGame(DefItemDTO bodyDef) {
        this.worldEvolver.addDynamicBody(
                bodyDef.assetId, bodyDef.size,
                bodyDef.posX, bodyDef.posY,
                bodyDef.speedX, bodyDef.speedY,
                0, 0,
                bodyDef.angle,
                bodyDef.angularSpeed,
                0d,
                bodyDef.thrust);
    }

    protected String getThreadName() {
        return this.getClass().getSimpleName();
    }

    // Optional hook for subclasses (e.g., create players).
    protected void onActivate() {
        // no-op by default
    }

    // Override to implement logic for ALIVE tick
    protected abstract void tickAlive();

    protected final DoubleVector centerPosition() {
        double x = this.worldEvolver.getWorldDimension().width / 2.0;
        double y = this.worldEvolver.getWorldDimension().height / 2.0;
        return new DoubleVector(x, y);
    }

    // Converts a DefItem (prototype or DTO) into a concrete DefItemDTO
    protected final DefItemDTO toDTO(DefItem defItem) {
        switch (defItem) {
            case DefItemDTO dto -> {
                return dto; // ========== already a DTO ==========>>
            }
            case DefItemPrototypeDTO proto -> {
                return this.prototypeToDTO(proto); // ====== convert prototype ======>
            }
            default -> throw new IllegalStateException(
                    "Unsupported DefItem implementation: " + defItem.getClass().getName());
        }

    }

    // region Random helpers (random***)
    // protected DoubleVector randomAcceleration() {
    // return new DoubleVector(
    // this.rnd.nextGaussian(),
    // this.rnd.nextGaussian(),
    // this.rnd.nextFloat() * this.AIConfig.maxAccModule);
    // }

    protected double randomAngularSpeed(double maxAngularSpeed) {
        return this.rnd.nextFloat() * maxAngularSpeed - maxAngularSpeed / 2;
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

    // protected DoubleVector randomSpeed() {
    // return new DoubleVector(
    // this.rnd.nextGaussian(),
    // this.rnd.nextGaussian(),
    // this.rnd.nextFloat() * this.AIConfig.maxSpeedModule);
    // }
    // endregion

    // *** PRIVATE ***

    private DefItemDTO prototypeToDTO(DefItemPrototypeDTO proto) {
        double size = this.randomDoubleBetween(proto.minSize, proto.maxSize);
        double angle = this.randomDoubleBetween(proto.minAngle, proto.maxAngle);
        double x = this.randomDoubleBetween(proto.posMinX, proto.posMaxX);
        double y = this.randomDoubleBetween(proto.posMinY, proto.posMaxY);
        double angularSpeed = this.randomDoubleBetween(proto.angularSpeedMin, proto.angularSpeedMax);
        double thrust = this.randomDoubleBetween(proto.thrustMin, proto.thrustMax);

        // Velocity components based on angle

        double angleRad = Math.toRadians(angle);
        double speed = this.randomDoubleBetween(proto.speedMin, proto.speedMax);
        double speedX = 0.0d;
        double speedY = 0.0d;
        if (speed != 0.0d) {
            speedX = Math.cos(angleRad) * speed;
            speedY = Math.sin(angleRad) * speed;
        }

        DefItemDTO dto = new DefItemDTO(
                proto.assetId, size, angle, x, y, proto.density,
                speedX, speedY, angularSpeed, thrust);

        return dto;
    }

    // *** INTERFACE IMPLEMENTATION ***

    // region Runnable
    @Override
    public final void run() {
        while (this.worldEvolver.getEngineState() != EngineState.STOPPED) {

            if (this.worldEvolver.getEngineState() == EngineState.ALIVE) {
                this.tickAlive();
            }

            try {
                Thread.sleep(this.rnd.nextInt(this.maxCreationDelay));
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    // endregion

    // private DoubleVector radialSpeedFromCenter() {
    //     double angle = this.rnd.nextDouble() * Math.PI * 2.0;

    //     double module = this.AIConfig.fixedSpeed
    //             ? Math.sqrt(this.AIConfig.speedX * this.AIConfig.speedX
    //                     + this.AIConfig.speedY * this.AIConfig.speedY)
    //             : this.AIConfig.maxSpeedModule * this.rnd.nextDouble();

    //     return new DoubleVector(
    //             Math.cos(angle), Math.sin(angle), module);
    // }

}
