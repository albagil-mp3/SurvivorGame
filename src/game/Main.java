package game;

import java.awt.Dimension;

import controller.impl.Controller;
import controller.ports.ActionsGenerator;
import game.actionsgen.*;
import game.aigen.*;
import game.levelgen.*;
import game.worlddef.RandomWorldDefinitionProvider;
import model.impl.Model;
import utils.assets.impl.ProjectAssets;
import utils.helpers.DoubleVector;
import view.core.View;
import world.ports.WorldDefinition;
import world.ports.WorldDefinitionProvider;

public class Main {

	public static void main(String[] args) {

		System.setProperty("sun.java2d.uiScale", "1.0");

		DoubleVector worldDimension = new DoubleVector(9000, 6000);
		DoubleVector viewDimension = new DoubleVector(1800, 1800);
		int maxDynamicBodies = 2000;
		int maxAsteroidCreationDelay = 500;

		ProjectAssets projectAssets = new ProjectAssets();
		ActionsGenerator actionsGenerator = new ActionsReboundCollisionPlayerImmunity();
		WorldDefinitionProvider world = new RandomWorldDefinitionProvider(
				worldDimension, projectAssets);

		// *** CORE ENGINE ***

		// region Controller
		Controller controller = new Controller(
				worldDimension,
				viewDimension,
				maxDynamicBodies,
				new View(),
				new Model(),
				actionsGenerator);

		controller.activate();
		// endregion

		// *** SCENE ***

		// region World definition
		WorldDefinition worldDef = world.provide();
		// endregion

		// region Level generator (Level***)
		new LevelBasic(controller, worldDef);
		// endregion

		// region AI generator (AI***)
		new AIBasicSpawner(controller, worldDef, maxAsteroidCreationDelay).activate();
		// endregion

	}
}
