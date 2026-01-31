

import ai.*;
import assets.impl.ProjectAssets;
import engine.controller.impl.Controller;
import engine.controller.ports.ActionsGenerator;
import engine.model.impl.Model;
import engine.utils.helpers.DoubleVector;
import engine.view.core.View;
import engine.world.ports.WorldDefinition;
import engine.world.ports.WorldDefinitionProvider;
import level.*;
import rules.*;
import worlddef.RandomWorldDefinitionProvider;

public class Main {

	public static void main(String[] args) {

		System.setProperty("sun.java2d.uiScale", "1.0");

		DoubleVector worldDimension = new DoubleVector(40000, 40000);
		DoubleVector viewDimension = new DoubleVector(2400, 1500);
		int maxBodies = 800;
		int maxAsteroidCreationDelay = 100;

		ProjectAssets projectAssets = new ProjectAssets();
		ActionsGenerator actionsGenerator = new DeadInLimitsPlayerImmunity();
		WorldDefinitionProvider world = new RandomWorldDefinitionProvider(
				worldDimension, projectAssets);

		// *** CORE ENGINE ***

		// region Controller
		Controller controller = new Controller(
				worldDimension,
				viewDimension,
				maxBodies,
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
