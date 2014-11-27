package de.bms.prob

import com.google.common.base.Charsets
import com.google.common.io.Resources
import de.bms.BMotionGroovy
import de.bms.server.BMotionScriptEngineProvider
import de.prob.scripting.Api
import de.prob.scripting.Downloader
import de.prob.statespace.AnimationSelector

import javax.script.ScriptEngineManager

public class ProBScriptEngineProvider implements BMotionScriptEngineProvider {

    private Api api;
    private AnimationSelector animations;
    private ScriptEngineManager manager;
    private Downloader downloader;
    private static final String[] IMPORTS = ["import de.prob.statespace.*;",
                                             "import de.prob.model.representation.*;",
                                             "import de.prob.model.classicalb.*;",
                                             "import de.prob.model.eventb.*;",
                                             "import de.prob.animator.domainobjects.*;",
                                             "import de.prob.animator.command.*;",
                                             "import de.bms.prob.*",
                                             "import de.bms.prob.observer.*"];

    public ProBScriptEngineProvider() {
        this.api = de.prob.Main.getInjector().getInstance(Api.class)
        this.animations = de.prob.Main.getInjector().getInstance(AnimationSelector.class)
        this.downloader = de.prob.Main.getInjector().getInstance(Downloader.class)
        this.manager = new ScriptEngineManager(this.getClass().getClassLoader())
    }

    @Override
    public GroovyShell get() {
        def engine = new GroovyShell()
        engine.setVariable("api", api)
        engine.setVariable("animations", animations)
        engine.setVariable("downloader", downloader)
        engine.setVariable("engine", engine)
        URL url = Resources.getResource("probscript");
        String bmsscript = Resources.toString(url, Charsets.UTF_8);
        def aimports = IMPORTS
        aimports += BMotionGroovy.IMPORTS
        engine.evaluate(aimports.join("\n") + "\n" + bmsscript)
        return engine
    }

    @Override
    public String[] getImports() {
        return IMPORTS
    }
}
