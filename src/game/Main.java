package game;

import controller.implementations.Controller;
import game.implementations.actions.*;
import game.implementations.ai.*;
import game.implementations.level.*;
import model.implementations.Model;
import utils.assets.implementations.ProjectAssets;
import view.core.View;
import world.implementations.*;
import world.ports.WorldDefinition;
import world.ports.WorldDefinitionProvider;

public class Main {

	public static void main(String[] args) {

		System.setProperty("sun.java2d.uiScale", "1.0");

		int worldWidth = 2450;
		int worldHeight = 1450;
		int maxDynamicBodies = 2000;
		int maxAsteroidCreationDelay = 500;

		// *** CORE ENGINE => MVC + controller + default actions generator ***

		Controller controller = new Controller(
				worldWidth, worldHeight,
				new View(), new Model(worldWidth, worldHeight, maxDynamicBodies),
				new ActionsReboundCollisionPlayerImmunity());

		controller.activate();

		// *** SCENE SETUP => world definition+ level generator + IA generator ***

		// 1) World definition
		ProjectAssets projectAssets = new ProjectAssets();
		WorldDefinitionProvider world = new RandomWorldDefinitionProvider(
				worldWidth, worldHeight, projectAssets);
		WorldDefinition worldDef = world.provide();

		// 2) Level generator (Level***)
		new LevelBasic(controller, worldDef);

		// 3) AI generator (AI***)
		new AIBasicSpawner(controller, worldDef, maxAsteroidCreationDelay).activate();

	}
}
