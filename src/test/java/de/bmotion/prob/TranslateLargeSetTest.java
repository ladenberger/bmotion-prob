package de.bmotion.prob;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.bmotion.core.BMotionException;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

public class TranslateLargeSetTest {

	private static ClassicalBVisualization vis;
	private static AnimationSelector animations;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		animations = de.prob.servlet.Main.getInjector().getInstance(AnimationSelector.class);

		URL modelResource = TranslateLargeSetTest.class.getResource("models/B/largeset/largeset.mch");
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
		assertThat(vis.translate(vis.eval("iv").toString()), instanceOf(List.class));
	}

}
