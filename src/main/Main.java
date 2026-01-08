package main;

import assets.implementations.ProjectAssets;
import controller.implementations.Controller;
import generators.implementations.LifeGenerator;
import generators.implementations.SceneGenerator;
import generators.ports.LifeConfigDTO;
import model.implementations.Model;
import view.core.View;
import world.implementations.RandomWorldDefinitionProvider;
import world.ports.WorldDefinition;
import world.ports.WorldDefinitionProvider;

public class Main {

        public static void main(String[] args) {

                System.setProperty("sun.java2d.uiScale", "1.0");
                int worldWidth = 2450;
                int worldHeight = 1450;

                ProjectAssets projectAssets = new ProjectAssets();

                WorldDefinitionProvider world = new RandomWorldDefinitionProvider(worldWidth, worldHeight,
                                projectAssets);

                WorldDefinition worldDef = world.provide();

                Controller controller = new Controller(
                                worldWidth, worldHeight, // World dimensions
                                3500, // Max dynamic bodies
                                new View(), new Model(),
                                worldDef.gameAssets);

                controller.activate();

                SceneGenerator worldGenerator = new SceneGenerator(controller, worldDef);

                LifeConfigDTO lifeConfig = new LifeConfigDTO(
                                10000, // maxCreationDelay
                                54, 54, // maxSize, minSize
                                1000, 10, // maxMass, minMass
                                175, // maxSpeedModule
                                0); // maxAccModule

                LifeGenerator lifeGenerator = new LifeGenerator(controller, worldDef, lifeConfig);

                lifeGenerator.activate();
        }
}
