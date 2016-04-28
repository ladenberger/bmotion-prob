package de.bmotion.prob;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.bmotion.core.objects.FormulaListObject;
import de.bmotion.core.objects.FormulaObject;
import de.bmotion.prob.objects.GraphObject;
import de.prob.check.ConsistencyChecker;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelChecker;
import de.prob.check.ModelCheckingOptions;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.AnimationSelector;

public class ProjectionDiagramTest {

	private static EventBVisualization vis;
	private static IModelCheckListener listener;

	private static AnimationSelector animations;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		animations = de.prob.servlet.Main.getInjector().getInstance(AnimationSelector.class);

		URL modelResource = InitSessionTest.class.getResource("models/EventB/lift/m3.bum");
		vis = new EventBVisualization();
		vis.initModel(modelResource.getPath(), new HashMap<String, String>());

		listener = new IModelCheckListener() {

			@Override
			public void updateStats(String jobId, long timeElapsed, IModelCheckingResult result,
					StateSpaceStats stats) {
			}

			@Override
			public void isFinished(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {

				synchronized (this) {
					notifyAll();
				}

			}

		};

		ModelChecker checker = new ModelChecker(new ConsistencyChecker(vis.getTrace().getStateSpace(),
				new ModelCheckingOptions().checkInvariantViolations(false), null, listener));
		checker.start();

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {

		animations.removeTrace(vis.getTrace());
	}

	@Test
	public void convertFormulasToExpressionsTest() {

		List<FormulaObject> formulas = new ArrayList<FormulaObject>();
		formulas.add(new FormulaObject("door"));
		// Duplicate formulas should be filtered
		formulas.add(new FormulaObject("door"));
		formulas.add(new FormulaObject("door = closed"));

		Map<String, String> expressions = vis.convertFormulasToExpressions(formulas);

		assertEquals(2, expressions.size());
		assertEquals(expressions.get("door"), "door");
		// predicate should be converted to expression
		assertEquals(expressions.get("door = closed"), "bool(door = closed)");

	}

	@Test
	public void createProjectionDiagramTest1() throws InterruptedException {

		synchronized (listener) {
			listener.wait(2000);
		}

		List<FormulaObject> list = new ArrayList<FormulaObject>();
		list.add(new FormulaObject("door"));
		FormulaListObject formulaListObject = new FormulaListObject(list);

		HashMap<String, FormulaListObject> oList = new HashMap<String, FormulaListObject>();
		oList.put("somebmsid", formulaListObject);

		GraphObject graph = vis.createProjectionDiagram(oList);

		assertEquals(4, graph.getNodes().size());
		assertEquals(4, graph.getEdges().size());

	}

	@Test
	public void createProjectionDiagramTest2() throws InterruptedException {

		synchronized (listener) {
			listener.wait(2000);
		}

		List<FormulaObject> list = new ArrayList<FormulaObject>();
		list.add(new FormulaObject("door"));
		list.add(new FormulaObject("floor"));
		FormulaListObject formulaListObject = new FormulaListObject(list);

		HashMap<String, FormulaListObject> oList = new HashMap<String, FormulaListObject>();
		oList.put("somebmsid", formulaListObject);

		GraphObject graph = vis.createProjectionDiagram(oList);

		assertEquals(8, graph.getNodes().size());
		assertEquals(12, graph.getEdges().size());

	}

	@Test
	public void reTranslateTest() {

		assertEquals(vis.reTranslate(true), "TRUE");
		assertEquals(vis.reTranslate(false), "FALSE");
		assertEquals(vis.reTranslate("SomeString"), "SomeString");

		List<String> list = new ArrayList<String>();
		list.add("ele1");
		list.add("ele2");

		assertEquals(vis.reTranslate(list), "[ele1, ele2]");

	}

}
