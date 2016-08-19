package de.bmotion.prob;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.bmotion.core.BMotionException;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

public class TranslateTest {

	private static ClassicalBVisualization vis;
	private static AnimationSelector animations;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		animations = de.prob.servlet.Main.getInjector().getInstance(AnimationSelector.class);

		URL modelResource = TranslateTest.class.getResource("models/B/other/sets.mch");
		vis = new ClassicalBVisualization();
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("MAXINT", "300");
		options.put("MININT", "-300");
		options.put("DEFAULT_SETSIZE", "300");
		vis.initModel(modelResource.getPath(), options);
		Trace trace = vis.getTrace();
		Trace newTrace = trace.randomAnimation(2);
		animations.traceChange(newTrace);

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		animations.removeTrace(vis.getTrace());
	}

	@Test
	public void largeSetTranslateTest() throws BMotionException {
		Object translated = vis.translate(vis.eval("largeSet").toString());
		List<?> castedTranslated = (List<?>) translated;
		assertEquals(castedTranslated.size(), 300);
		assertThat(castedTranslated, instanceOf(List.class));
	}

	@Test
	public void setOfSetsTranslateTest() throws BMotionException {
		Object translated = vis.translate(vis.eval("setOfSets").toString());
		List<?> castedTranslated = (List<?>) translated;
		assertEquals(castedTranslated.size(), 10);
		assertThat(castedTranslated.get(0), instanceOf(List.class));
	}

	@Test
	public void simpleRelationTranslateTest() throws BMotionException {
		Object translated = vis.translate(vis.eval("simpleRelation").toString());
		List<?> castedTranslated = (List<?>) translated;
		assertEquals(castedTranslated.size(), 3);
		Object actual = castedTranslated.get(2);
		assertThat(actual, instanceOf(List.class));
	}

	@Test
	public void nestedRelationTranslateTest() throws BMotionException {
		Object translated = vis.translate(vis.eval("nestedRelation").toString());
		List<?> castedTranslated = (List<?>) translated;
		assertEquals(castedTranslated.size(), 4);
		Object actual = castedTranslated.get(2);
		assertThat(actual, instanceOf(List.class));
	}
	

	@Test
	public void simpleFunctionTranslateTest() throws BMotionException {
		Object translated = vis.translate(vis.eval("simpleFunction").toString());
		List<?> castedTranslated = (List<?>) translated;
		assertEquals(castedTranslated.size(), 5);
		Object actual = castedTranslated.get(2);
		assertThat(actual, instanceOf(List.class));
	}	
	
}
