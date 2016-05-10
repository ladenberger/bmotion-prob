package de.bmotion.prob;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.bmotion.core.BMotionException;
import de.bmotion.prob.model.TransitionObject;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

public class ProBVisualizationTest {

	private static AnimationSelector animations;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		animations = de.prob.servlet.Main.getInjector().getInstance(AnimationSelector.class);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	@Test
	public void gotoTraceIndexTest() throws BMotionException {
		
		URL modelResource = ProBVisualizationTest.class.getResource("models/EventB/lift/m3.bum");
		EventBVisualization vis = new EventBVisualization();
		vis.initModel(modelResource.getPath(), new HashMap<String, String>());
		Trace oldTrace = vis.getTrace();
		Trace trace = oldTrace.randomAnimation(2);
		animations.traceChange(trace);

		vis.executeEvent("close_door");
		vis.executeEvent("switch_move_up");
		vis.executeEvent("move_up");
		
		vis.gotoTraceIndex(3);
		
		assertEquals("switch_move_up()", vis.getTrace().getCurrentTransition().getRep());
		
	}

	@Test
	public void getHistoryEventBTest() throws BMotionException {

		URL modelResource = ProBVisualizationTest.class.getResource("models/EventB/lift/m3.bum");
		EventBVisualization vis = new EventBVisualization();
		vis.initModel(modelResource.getPath(), new HashMap<String, String>());
		Trace oldTrace = vis.getTrace();
		Trace trace = oldTrace.randomAnimation(2);
		animations.traceChange(trace);

		vis.executeEvent("close_door");
		vis.executeEvent("switch_move_up");
		vis.executeEvent("move_up");

		HashMap<String, String> options = new HashMap<String, String>();
		options.put("predicate", "f=1");
		vis.executeEvent("send_request", options);

		Trace trace5 = vis.getTrace().back();
		animations.traceChange(trace5);

		assertEquals(6, trace5.getTransitionList(true).size());

		List<TransitionObject> history = vis.getHistory(trace5.getCurrent());

		assertEquals(6, history.size());

		TransitionObject transitionObject = history.get(5);

		assertEquals("past", history.get(3).getGroup()); // switch_move_up
		assertEquals("current", history.get(4).getGroup()); // move_up
		assertEquals("future", transitionObject.getGroup()); // send_request

		assertEquals("close_door()", history.get(2).getOpString());
		assertEquals("send_request(1)", transitionObject.getOpString());

		animations.removeTrace(vis.getTrace());

	}

	@Test
	public void getHistoryCSPTest() throws BMotionException {

		URL modelResource = ProBVisualizationTest.class.getResource("models/CSP/bully/bully.csp");
		CSPVisualization vis = new CSPVisualization();
		vis.initModel(modelResource.getPath(), new HashMap<String, String>());

		vis.executeEvent("Network");
		vis.executeEvent("test.0.3");
		vis.executeEvent("fail.2");
		vis.executeEvent("ok.3.0");

		Trace back = vis.getTrace().back();
		animations.traceChange(back);

		assertEquals(back, vis.getTrace());
		assertEquals(4, vis.getTrace().getTransitionList(true).size());

		List<TransitionObject> history = vis.getHistory(vis.getTrace().getCurrent());
		assertEquals(4, history.size());

		assertEquals("past", history.get(1).getGroup()); // test.0.3
		assertEquals("current", history.get(2).getGroup()); // fail.2
		assertEquals("future", history.get(3).getGroup()); // ok.3.0

		assertEquals("Network", history.get(0).getOpString());
		assertEquals("test.0.3", history.get(1).getOpString());
		assertEquals("fail.2", history.get(2).getOpString());

		animations.removeTrace(back);

	}

}
