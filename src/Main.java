

import ai.*;
import engine.controller.impl.Controller;
import engine.controller.ports.ActionsGenerator;
import engine.model.impl.Model;
import engine.utils.helpers.DoubleVector;
import engine.view.core.View;
import engine.worlddef.ports.WorldDefinition;
import engine.worlddef.ports.WorldDefinitionProvider;
import level.*;
import world.ProjectAssets;

public class Main {

	public static void main(String[] args) {

		System.setProperty("sun.java2d.uiScale", "1.0");

		DoubleVector worldDimension = new DoubleVector(40000, 40000);
		DoubleVector viewDimension = new DoubleVector(2400, 1500);
		int maxBodies = 800;
		int maxAsteroidCreationDelay = 100;

		ProjectAssets projectAssets = new ProjectAssets();
		ActionsGenerator gameRules = new rules.DeadInLimitsPlayerImmunity();
		WorldDefinitionProvider worldProv = new world.RandomWorldDefinitionProvider(
				worldDimension, projectAssets);

		// *** CORE ENGINE ***

		// region Controller
		Controller controller = new Controller(
				worldDimension,
				viewDimension,
				maxBodies,
				new View(),
				new Model(),
				gameRules);

		controller.activate();
		// endregion

		// *** SCENE ***

		// region World definition
		WorldDefinition worldDef = worldProv.provide();
		// endregion

		// region Level generator (Level***)
		new level.LevelBasic(controller, worldDef);
		// endregion

		// region AI generator (AI***)
		new ai.AIBasicSpawner(controller, worldDef, maxAsteroidCreationDelay).activate();
		// endregion

	}
}
