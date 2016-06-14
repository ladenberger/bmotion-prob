package de.bmotion.prob;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.bmotion.core.BMotion;
import de.bmotion.core.BMotionException;
import de.bmotion.core.BMotionServer;
import de.bmotion.core.InitSessionService;

public class InitSessionTest {

	private static BMotionServer server;
	private static String sessionId = UUID.randomUUID().toString();

	@BeforeClass
	public static void setUpBeforeClass() throws BMotionException {
		server = ProBServerFactory.getServer(new String[] {});
		server.start();
	}

	@AfterClass
	public static void tearDownAfterClass() throws BMotionException {
		if (server != null) {
			server.stop();
		}
	}

	private void initSessionTestHelper(String modelPath, Class<?> visualizationClass) throws BMotionException {

		URL modelResource = InitSessionTest.class.getResource(modelPath);
		BMotion bms = InitSessionService.initSession(server.getSocketServer(), sessionId, modelResource.getPath(),
				new HashMap<String, String>());

		// Correct visualization instance
		assertThat(bms, instanceOf(visualizationClass));
		// Client tool property is correct
		assertEquals(visualizationClass.getSimpleName(), bms.getSessionData().get("tool"));
		// Client model path is correct
		assertEquals(modelResource.getPath(), bms.getSessionData().get("modelPath"));
		// Session is saved
		assertEquals(bms, server.getSocketServer().getSessions().get(bms.getId()));

	}

	@Test
	public void initSessionWithNullOptions() throws BMotionException {
		URL modelResource = InitSessionTest.class.getResource("models/B/phonebook/phonebook.mch");
		BMotion bms = InitSessionService.initSession(server.getSocketServer(), sessionId, modelResource.getPath(),
				null);
		assertThat(bms, instanceOf(ClassicalBVisualization.class));
	}

	@Test
	public void initBSessionTest() throws BMotionException {
		initSessionTestHelper("models/B/phonebook/phonebook.mch", ClassicalBVisualization.class);
	}

	@Test
	public void initCSPSessionTest() throws BMotionException {
		initSessionTestHelper("models/CSP/bully/bully.csp", CSPVisualization.class);
	}

	@Test
	public void initEventBSessionTest() throws BMotionException {
		initSessionTestHelper("models/EventB/lift/m3.bum", EventBVisualization.class);
	}

}
