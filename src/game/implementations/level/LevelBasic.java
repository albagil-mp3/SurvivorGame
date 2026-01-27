package game.implementations.level;

import java.util.ArrayList;

import controller.ports.WorldInitializer;
import game.core.AbstractLevelGenerator;
import world.ports.DefItem;
import world.ports.DefItemDTO;
import world.ports.WorldDefinition;

public class LevelBasic extends AbstractLevelGenerator {

    // *** CONSTRUCTORS ***

    public LevelBasic(WorldInitializer worldInitializer, WorldDefinition worldDef) {
        super(worldInitializer, worldDef);
    }

    // *** PROTECTED (alphabetic order) ***

    @Override
    protected void createSpaceDecorators() {
        ArrayList<DefItem> decorators = this.worldDefinition.spaceDecorators;

        for (DefItem def : decorators) {
            DefItemDTO deco = this.materialize(def);
            this.worldInitializer.addDecorator(deco.assetId, deco.size, deco.posX, deco.posY, deco.angle);
        }
    }

    @Override
    protected void createStaticBodies() {
        ArrayList<DefItem> bodyDefs = this.worldDefinition.gravityBodies;

        for (DefItem def : bodyDefs) {
            DefItemDTO body = this.materialize(def);
            this.worldInitializer.addStaticBody(body.assetId, body.size, body.posX, body.posY, body.angle);
        }
    }
}
