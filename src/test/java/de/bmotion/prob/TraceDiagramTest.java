package de.bmotion.prob;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.bmotion.core.BMotionException;
import de.bmotion.core.objects.FormulaListObject;
import de.bmotion.core.objects.FormulaObject;
import de.bmotion.prob.objects.GraphObject;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

public class TraceDiagramTest {

	private static EventBVisualization eventBVisualization;
	private static CSPVisualization cspVisualization;

	private static AnimationSelector animations;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		animations = de.prob.servlet.Main.getInjector().getInstance(AnimationSelector.class);

		eventBVisualization = new EventBVisualization();
		eventBVisualization.initModel(TraceDiagramTest.class.getResource("models/EventB/lift/m3.bum").getPath(),
				Collections.emptyMap());
		Trace trace = eventBVisualization.getTrace();
		Trace newTrace = trace.randomAnimation(2);
		animations.traceChange(newTrace);

		cspVisualization = new CSPVisualization();
		cspVisualization.initModel(TraceDiagramTest.class.getResource("models/CSP/bully/bully.csp").getPath(),
				Collections.emptyMap());
		cspVisualization.executeEvent("Network");

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {

		animations.removeTrace(eventBVisualization.getTrace());
		animations.removeTrace(cspVisualization.getTrace());
	}

	@Test
	public void createBTraceDiagramTest() throws BMotionException {

		eventBVisualization.executeEvent("close_door");
		eventBVisualization.executeEvent("switch_move_up");
		eventBVisualization.executeEvent("move_up");

		List<FormulaObject> list = new ArrayList<FormulaObject>();
		list.add(new FormulaObject("door"));
		list.add(new FormulaObject("floor"));
		FormulaListObject formulaListObject = new FormulaListObject(list);

		HashMap<String, FormulaListObject> oList = new HashMap<String, FormulaListObject>();
		oList.put("somebmsid", formulaListObject);

		GraphObject traceDiagram = eventBVisualization.createTraceDiagram(oList);

		assertEquals(6, traceDiagram.getNodes().size());
		assertEquals(5, traceDiagram.getEdges().size());

		eventBVisualization.executeEvent("move_up");

		GraphObject traceDiagram2 = eventBVisualization.createTraceDiagram(oList);

		assertEquals(7, traceDiagram2.getNodes().size());
		assertEquals(6, traceDiagram2.getEdges().size());

	}

	@Test
	public void createCSPTraceDiagramTest() throws BMotionException {

		cspVisualization.executeEvent("test.0.3");
		cspVisualization.executeEvent("fail.2");

		GraphObject traceDiagram = cspVisualization.createTraceDiagram();

		assertEquals(4, traceDiagram.getNodes().size());
		assertEquals(3, traceDiagram.getEdges().size());

		cspVisualization.executeEvent("fail.2");

		GraphObject traceDiagram2 = cspVisualization.createTraceDiagram();

		assertEquals(5, traceDiagram2.getNodes().size());
		assertEquals(4, traceDiagram2.getEdges().size());

	}

}
