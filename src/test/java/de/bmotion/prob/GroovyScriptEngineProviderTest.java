package de.bmotion.prob;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;

import de.bmotion.core.BMotion;
import de.bmotion.core.BMotionException;

public class GroovyScriptEngineProviderTest {

	private static ProBScriptEngineProvider proBScriptEngineProvider;

	private static BMotion vis;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		proBScriptEngineProvider = new ProBScriptEngineProvider();
		vis = new ClassicalBVisualization();

	}

	@Test(expected = BMotionException.class)
	public void ProBNotExistingFileShouldThrowError() throws BMotionException {
		proBScriptEngineProvider.load("notExistingGroovyPath", vis);
	}

	@Test(expected = BMotionException.class)
	public void ProBNullFileShouldThrowError() throws BMotionException {
		ProBScriptEngineProvider proBScriptEngineProvider = new ProBScriptEngineProvider();
		proBScriptEngineProvider.load(null, vis);
	}

	@Test
	public void ProBGroovyScriptEngineProviderSimpleTest() throws BMotionException {
		URL groovyResource = ClassicalBVisualizationTest.class.getResource("groovy/empty.groovy");
		proBScriptEngineProvider.load(groovyResource.getPath(), vis);
	}

	@Test
	public void ProBGroovyScriptEngineProviderRegisterMethodTest() throws BMotionException {

		URL groovyResource = ClassicalBVisualizationTest.class.getResource("groovy/registerMethod.groovy");
		proBScriptEngineProvider.load(groovyResource.getPath(), vis);

		// Two methods should be registered
		assertEquals(vis.getMethods().size(), 4);

		// Test method without parameters
		Object response1 = vis.callMethod("someMethodWithoutParameter");
		assertEquals(String.valueOf(response1), "Call without parameter");

		// Test method with one parameter
		Object response2 = vis.callMethod("someMethodWith1Parameter", "arg1");
		assertEquals(String.valueOf(response2), "Call with one parameter arg1");
		
		// Test method with two parameters
		Object response3 = vis.callMethod("someMethodWith2Parameter", "arg1", "arg2");
		assertEquals(String.valueOf(response3), "Call with two parameters arg1 arg2");
		
		// Test method that returns an integer
		Object response4 = vis.callMethod("returnInteger");
		assertEquals(response4, 0);
		
	}
	
	@Test(expected = BMotionException.class)
	public void ProBGroovyScriptEngineProviderCallMethodErrorTest() throws BMotionException {

		URL groovyResource = ClassicalBVisualizationTest.class.getResource("groovy/registerMethod.groovy");
		proBScriptEngineProvider.load(groovyResource.getPath(), vis);
		
		// Test someMethodWith2Parameter method with three parameters should throw exception
		vis.callMethod("someMethodWith2Parameter", "arg1", "arg2");
		// Test someMethodWith2Parameter method with one parameter should throw exception
		vis.callMethod("someMethodWith2Parameter", "arg1");
		
	}

}
