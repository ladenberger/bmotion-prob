package de.bmotion.prob;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.bmotion.core.BMotionException;
import de.bmotion.core.objects.FormulaListObject;
import de.bmotion.core.objects.FormulaObject;
import de.bmotion.core.objects.FormulaReturnObject;
import de.bmotion.prob.model.ModelObject;
import de.bmotion.prob.objects.BEventReturnObject;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.translator.types.BigInteger;

public class EventBVisualizationTest {

	private static EventBVisualization vis;
	private static AnimationSelector animations;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		animations = de.prob.servlet.Main.getInjector().getInstance(AnimationSelector.class);

		URL modelResource = EventBVisualizationTest.class.getResource("models/EventB/lift/m3.bum");
		vis = new EventBVisualization();
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
	public void eventBModelInitializedTest() {

		Map<String, Object> tooltData = vis.getToolData();

		// Initializing model should create a trace
		assertNotNull(vis.getTrace());

		// Client data is set
		assertEquals(vis.getTrace().getCurrentState().getId(), tooltData.get("stateId"));
		assertEquals(vis.getTrace().getUUID().toString(), tooltData.get("traceId"));
		assertEquals(tooltData.get("initialized"), true);

	}

	@Test
	public void eventBModelDataTest() {

		Map<String, Object> clientData = vis.getToolData();

		// Client model object created
		ModelObject modelObject = (ModelObject) clientData.get("model");
		assertEquals(2, modelObject.getSets().size());
		assertEquals(5, modelObject.getVariables().size());
		assertEquals(9, modelObject.getConstants().size());

		assertEquals(11, modelObject.getTransitions().size());

	}

	@Test
	public void eventBExecuteEventAndGetOpStringTest() throws BMotionException {

		Map<String, String> options = new HashMap<String, String>();
		vis.executeEvent("close_door", options);

		options.put("predicate", "f=0");
		BEventReturnObject returnObject = (BEventReturnObject) vis.executeEvent("send_request", options);
		assertEquals("send_request(0)", vis.getOpString(vis.getTrace().getCurrentTransition()));

		assertEquals(0, returnObject.getReturnValues().size());
		assertEquals(vis.getTrace().getCurrentState().getId(), returnObject.getStateId());

	}

	@Test
	public void eventBExecuteEventEmptyPredicateTest() throws BMotionException {

		Map<String, String> options = new HashMap<String, String>();
		options.put("predicate", "");
		BEventReturnObject returnObject = (BEventReturnObject) vis.executeEvent("send_request", options);

		assertEquals(0, returnObject.getReturnValues().size());
		assertEquals(vis.getTrace().getCurrentState().getId(), returnObject.getStateId());

	}

	@Test
	public void eventBExecuteEventNullPredicateTest() throws BMotionException {

		Map<String, String> options = new HashMap<String, String>();
		options.put("predicate", null);
		BEventReturnObject returnObject = (BEventReturnObject) vis.executeEvent("send_request", options);

		assertEquals(0, returnObject.getReturnValues().size());
		assertEquals(vis.getTrace().getCurrentState().getId(), returnObject.getStateId());

	}

	@Test
	public void eventBEvalTest() throws BMotionException {
		assertEquals("idle", vis.eval("move"));
		assertEquals("{}", vis.eval("service"));
	}

	@Test
	public void eventBEvaluateFormulasTest() throws BMotionException {

		List<FormulaObject> list = new ArrayList<FormulaObject>();
		list.add(new FormulaObject("move", new HashMap<String, Object>()));
		list.add(new FormulaObject("service", new HashMap<String, Object>()));
		FormulaListObject formulaListObject = new FormulaListObject(list);

		HashMap<String, FormulaListObject> oList = new HashMap<String, FormulaListObject>();
		oList.put("somebmsid", formulaListObject);

		Map<String, Map<String, Object>> result = vis.evaluateFormulas(oList);
		Map<String, Object> resultFormulas = result.get("somebmsid");
		assertEquals(2, resultFormulas.size());
		assertEquals(((FormulaReturnObject) resultFormulas.get("move")).getResult(), "idle");
		assertEquals(((FormulaReturnObject) resultFormulas.get("service")).getResult(), "{}");

	}

	@Test
	public void eventBEvaluateFormulasTranslateTest() throws BMotionException {

		HashMap<String, Object> options = new HashMap<String, Object>();
		options.put("translate", true);

		List<FormulaObject> list = new ArrayList<FormulaObject>();
		list.add(new FormulaObject("floor", options));
		list.add(new FormulaObject("service", options));
		FormulaListObject formulaListObject = new FormulaListObject(list);

		HashMap<String, FormulaListObject> oList = new HashMap<String, FormulaListObject>();
		oList.put("somebmsid", formulaListObject);

		Map<String, Map<String, Object>> result = vis.evaluateFormulas(oList);
		Map<String, Object> resultFormulas = result.get("somebmsid");
		assertEquals(2, resultFormulas.size());
		assertThat(((FormulaReturnObject) resultFormulas.get("floor")).getResult(), instanceOf(BigInteger.class));
		assertEquals(((FormulaReturnObject) resultFormulas.get("service")).getResult(), Collections.emptyList());

	}

	@Test
	public void eventBTranslateTest() throws BMotionException {

		assertEquals(false, vis.translate("FALSE"));
		assertEquals(Collections.emptyList(), vis.translate("{}"));
		assertEquals("String", vis.translate("\"String\""));
		vis.translate(vis.eval("service").toString());
		assertThat(vis.translate(vis.eval("floor").toString()), instanceOf(BigInteger.class));

	}

}
