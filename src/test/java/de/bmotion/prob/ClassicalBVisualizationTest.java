package de.bmotion.prob;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.bmotion.core.BMotionException;
import de.bmotion.prob.model.ModelObject;
import de.bmotion.prob.objects.BEventReturnObject;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

public class ClassicalBVisualizationTest {

	private static ClassicalBVisualization vis;
	private static AnimationSelector animations;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		animations = de.prob.servlet.Main.getInjector().getInstance(AnimationSelector.class);

		URL modelResource = ClassicalBVisualizationTest.class.getResource("models/B/phonebook/phonebook.mch");
		vis = new ClassicalBVisualization();
		vis.initModel(modelResource.getPath(), new HashMap<String, String>());
		Trace trace = vis.getTrace();
		Trace newTrace = trace.randomAnimation(2);
		animations.traceChange(newTrace);

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {

		animations.removeTrace(vis.getTrace());

	}

	@Test
	public void classicalBModelInitializedTest() {

		Map<String, Object> tooltData = vis.getToolData();

		// Initializing model should create a trace
		assertNotNull(vis.getTrace());

		// Client data is set
		assertEquals(tooltData.get("stateId"), vis.getTrace().getCurrentState().getId());
		assertEquals(tooltData.get("traceId"), vis.getTrace().getUUID().toString());
		assertEquals(true, tooltData.get("initialized"));

	}

	@Test
	public void classicalBModelDataTest() {

		Map<String, Object> clientData = vis.getToolData();

		// Client model object created
		ModelObject modelObject = (ModelObject) clientData.get("model");
		assertEquals(2, modelObject.getSets().size());
		assertEquals(3, modelObject.getVariables().size());
		assertEquals(2, modelObject.getConstants().size());

		assertEquals(9, modelObject.getTransitions().size());
		// TransitionObject transitionObject =
		// modelObject.getTransitions().get(0);
		// assertEquals(1, transitionObject.getParameters().size());

	}

	@Test
	public void classicalBExecuteEventAndGetOpStringTest() throws BMotionException {

		Map<String, String> options = new HashMap<String, String>();
		vis.executeEvent("add", options);

		options.put("predicate", "name=\"PERSON1\" & nr=3");
		vis.executeEvent("add", options);

		assertEquals("add(\"PERSON1\",3)", vis.getOpString(vis.getTrace().getCurrentTransition()));

		options.clear();
		BEventReturnObject returnObject = (BEventReturnObject) vis.executeEvent("lookup", options);

		assertEquals("3 <-- lookup(\"PERSON1\")", vis.getOpString(vis.getTrace().getCurrentTransition()));
		assertEquals(1, returnObject.getReturnValues().size());
		assertEquals(vis.getTrace().getCurrentState().getId(), returnObject.getStateId());

	}

	@Test
	public void classicalBEvalTest() throws BMotionException {
		assertEquals("FALSE", vis.eval("lock"));
		assertEquals("{}", vis.eval("active"));
	}

	@Test
	public void classicalBTranslateTest() throws BMotionException {

		assertEquals(false, vis.translate("FALSE"));
		assertEquals(Collections.emptyList(), vis.translate("{}"));
		assertEquals("String", vis.translate("\"String\""));

		vis.translate(vis.eval("db").toString());
		vis.translate(vis.eval("active").toString());

		assertThat(vis.translate(vis.eval("lock").toString()), instanceOf(Boolean.class));

	}

}
