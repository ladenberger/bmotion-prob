package de.bmotion.prob;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Resources;

import de.bmotion.core.BMotion;
import de.bmotion.core.BMotionException;
import de.bmotion.core.IBMotionScriptEngineProvider;
import de.prob.scripting.Api;
import de.prob.statespace.AnimationSelector;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;

public class ProBScriptEngineProvider implements IBMotionScriptEngineProvider {

	private Api api;

	private AnimationSelector animations;

	private static final String[] IMPORTS = new String[] { "import de.prob.statespace.*;",
			"import de.prob.currentModel.representation.*;", "import de.prob.currentModel.classicalb.*;",
			"import de.prob.currentModel.eventb.*;", "import de.prob.animator.domainobjects.*;",
			"import de.prob.animator.command.*;" };

	public ProBScriptEngineProvider() {
		this.api = de.prob.Main.getInjector().getInstance(Api.class);
		this.animations = de.prob.Main.getInjector().getInstance(AnimationSelector.class);
	}

	@Override
	public GroovyShell load(String groovyPath, BMotion session) throws BMotionException {

		if (groovyPath == null) {
			throw new BMotionException("Groovy path must not be null.");
		}

		File groovyFile = new File(groovyPath);
		if (!groovyFile.exists()) {
			throw new BMotionException("No groovy script file found at path " + groovyPath + ".");
		}

		GroovyShell engine = new GroovyShell();
		engine.setVariable("api", api);
		engine.setVariable("animations", animations);
		engine.setVariable("engine", engine);
		engine.setVariable("bms", session);

		try {
			URI uri = groovyFile.toURI();
			String scriptContents = Resources.toString(uri.toURL(), Charsets.UTF_8);
			engine.evaluate(Joiner.on("\n").join(IMPORTS) + "\n" + scriptContents);
		} catch (IOException e) {
			throw new BMotionException("An exception occurred while loading groovy script: " + e.getMessage() + ".");
		} catch (GroovyRuntimeException e) {
			throw new BMotionException("An exception occurred while loading groovy script: " + e.getMessage() + ".");
		}

		return engine;

	}

	@Override
	public String[] getImports() {
		return IMPORTS;
	}

}
