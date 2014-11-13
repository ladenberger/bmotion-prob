package de.bms.prob

import de.bms.BMotion
import de.bms.itool.ITool
import de.bms.itool.ToolRegistry
import de.prob.model.representation.AbstractModel
import de.prob.scripting.Api
import de.prob.statespace.*
import groovy.util.logging.Slf4j

@Slf4j
public abstract class ProBAnimation implements ITool, IAnimationChangeListener, IModelChangedListener {

    public static final String TRIGGER_MODEL_CHANGED = "ModelChanged";
    def final ToolRegistry toolRegistry;
    def final AnimationSelector animations;
    def final Api api
    def final String toolId;
    def Trace trace;
    def AbstractModel model;

    public ProBAnimation(final String toolId, final ToolRegistry toolRegistry) {
        this.toolId = toolId
        this.toolRegistry = toolRegistry
        animations = de.prob.Main.getInjector().getInstance(AnimationSelector.class)
        api = de.prob.Main.getInjector().getInstance(Api.class)
        animations.registerAnimationChangeListener(this);
    }

    public AbstractModel getModel() {
        return model;
    }

    public void setModel(final AbstractModel model) {
        this.model = model;
        trace = new Trace(model);
        animations.addNewAnimation(trace)
    }

    public Trace getTrace() {
        return trace;
    }

    public StateSpace getStateSpace() {
        return trace != null ? trace.getStateSpace() : null;
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    @Override
    public void traceChange(final Trace currentTrace, final boolean currentAnimationChanged) {
        Trace oldtrace = trace;
        trace = currentTrace;
        if (oldtrace != null && !currentTrace.equals(oldtrace) && currentTrace.getCurrentState().isInitialised()) {
            toolRegistry.notifyToolChange(BMotion.TRIGGER_ANIMATION_CHANGED, this);
        }
    }

    @Override
    public void modelChanged(StateSpace s) {
        toolRegistry.notifyToolChange(de.prob.bmotion.ProBAnimation.TRIGGER_MODEL_CHANGED, this);
    }

    @Override
    public String getCurrentState() {
        return trace != null ? trace.getCurrentState().getId() : null;
    }

    @Override
    public String getName() {
        return toolId;
    }

    @Override
    public boolean canBacktrack() {
        return true;
    }

    @Override
    void initModel(String modelPath) {
        log.info "Loading model " + modelPath
        def formalism = getFormalism(modelPath)
        def model = Eval.x(api, "x.${formalism}_load('$modelPath')")
        setModel(model)
    }

    protected String getFormalism(final String modelPath) {
        String lang = null;
        if (modelPath.endsWith(".csp")) {
            return "csp";
        } else if (modelPath.endsWith(".buc") || modelPath.endsWith(".bcc") || modelPath.
                endsWith(".bum") || modelPath.endsWith(".bcm")) {
            return "eventb";
        } else if (modelPath.endsWith(".mch")) {
            return "b";
        } else if (modelPath.endsWith(".tla")) {
            return "tla";
        }
        return lang;
    }

    @Override
    public void refresh() {
        if (trace.getCurrentState().isInitialised()) {
            toolRegistry.notifyToolChange(BMotion.TRIGGER_ANIMATION_CHANGED, this);
        }
    }

}
