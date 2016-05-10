package de.bmotion.prob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.bmotion.core.BMotionException;
import de.prob.statespace.AnimationSelector;

public class CSPVisualizationTest {

	private static CSPVisualization vis;
	private static AnimationSelector animations;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		animations = de.prob.servlet.Main.getInjector().getInstance(AnimationSelector.class);

		URL modelResource = CSPVisualizationTest.class.getResource("models/CSP/bully/bully.csp");
		vis = new CSPVisualization();
		vis.initModel(modelResource.getPath(), new HashMap<String, String>());

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {

		animations.removeTrace(vis.getTrace());

	}

	@Test
	public void cspModelInitializedTest() {

		Map<String, Object> tooltData = vis.getToolData();

		// Initializing model should create a trace
		assertNotNull(vis.getTrace());

		// Client data is set
		assertEquals(tooltData.get("stateId"), vis.getTrace().getCurrentState().getId());
		assertEquals(tooltData.get("traceId"), vis.getTrace().getUUID().toString());
		assertEquals(true, tooltData.get("initialized"));

	}

	@Test
	public void cspExecuteEventAndGetOpStringTest() throws BMotionException {

		vis.executeEvent("Network");
		assertEquals("Network", vis.getOpString(vis.getTrace().getCurrentTransition()));

		vis.executeEvent("test.0.3");
		assertEquals("test.0.3", vis.getOpString(vis.getTrace().getCurrentTransition()));

		vis.executeEvent("fail.2");
		assertEquals("fail.2", vis.getOpString(vis.getTrace().getCurrentTransition()));

	}

	@Test
	public void cspEvalTest() throws BMotionException {
		assertEquals("4", vis.eval("N"));
		assertEquals("{fail.0,fail.1}", vis.eval("{fail.x | x <- {0..1}}"));
	}

}
