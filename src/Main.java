
import engine.controller.impl.Controller;
import engine.controller.ports.ActionsGenerator;
import engine.model.impl.Model;
import engine.utils.helpers.DoubleVector;
import engine.view.core.View;
import engine.world.ports.WorldDefinition;
import engine.world.ports.WorldDefinitionProvider;
import gameworld.ProjectAssets;

public class Main {

	public static void main(String[] args) {

		// region Graphics configuration
		System.setProperty("sun.java2d.uiScale", "1.0");
		System.setProperty("sun.java2d.opengl", "true");
		System.setProperty("sun.java2d.d3d", "false"); // OpenGL
		// endregion

		DoubleVector worldDimension = new DoubleVector(40000, 40000);
		DoubleVector viewDimension = new DoubleVector(2400, 1500);
		int maxBodies = 2000;
		int maxAsteroidCreationDelay = 3;

		ProjectAssets projectAssets = new ProjectAssets();

		// ActionsGenerator gameRules = new gamerules.LimitRebound();
		// ActionsGenerator gameRules = new gamerules.ReboundAndCollision();
		ActionsGenerator gameRules = new gamerules.InLimitsGoToCenter();

		// *** WORLD DEFINITION PROVIDER ***
		WorldDefinitionProvider worldProv = new gameworld.RandomWorldDefinitionProvider(
				worldDimension, projectAssets);

		// *** CORE ENGINE ***

		// region Controller
		Controller controller = new Controller(
				worldDimension, viewDimension, maxBodies,
				new View(), new Model(worldDimension, maxBodies),
				gameRules);

		controller.activate();
		// endregion

		// *** SCENE ***

		// region World definition
		WorldDefinition worldDef = worldProv.provide();
		// endregion

		// region Level generator (Level***)
		new gamelevel.LevelBasic(controller, worldDef);
		// endregion

		// region AI generator (AI***)
		new gameai.AIBasicSpawner(controller, worldDef, maxAsteroidCreationDelay).activate();
		// endregion
	}
}
