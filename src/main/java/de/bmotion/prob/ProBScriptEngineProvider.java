package de.bmotion.prob;


import java.io.IOException;
import java.net.URL;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Resources;

import de.bmotion.core.IBMotionScriptEngineProvider;
import de.prob.scripting.Api;
import de.prob.statespace.AnimationSelector;
import groovy.lang.GroovyShell;

public class ProBScriptEngineProvider implements IBMotionScriptEngineProvider {

	private Api api;
	private AnimationSelector animations;
	// private ScriptEngineManager manager;
	private static final String[] IMPORTS = new String[] { "import de.prob.statespace.*;",
			"import de.prob.currentModel.representation.*;", "import de.prob.currentModel.classicalb.*;",
			"import de.prob.currentModel.eventb.*;", "import de.prob.animator.domainobjects.*;",
			"import de.prob.animator.command.*;", "import de.bms.prob.*;", "import de.bms.prob.observer.*;" };

	public ProBScriptEngineProvider() {
		this.api = de.prob.Main.getInjector().getInstance(Api.class);
		this.animations = de.prob.Main.getInjector().getInstance(AnimationSelector.class);
		// this.manager = new
		// ScriptEngineManager(this.getClass().getClassLoader());
	}

	@Override
	public GroovyShell get() {
		GroovyShell engine = new GroovyShell();
		engine.setVariable("api", api);
		engine.setVariable("animations", animations);
		engine.setVariable("engine", engine);
		try {
			URL url = Resources.getResource("probscript");
			String bmsscript;
			bmsscript = Resources.toString(url, Charsets.UTF_8);
			engine.evaluate(Joiner.on("\n").join(IMPORTS) + "\n" + bmsscript);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return engine;
	}

	@Override
	public String[] getImports() {
		return IMPORTS;
	}

}
