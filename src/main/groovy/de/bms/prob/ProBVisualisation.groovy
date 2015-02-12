package de.bms.prob

import de.bms.BMotion
import de.bms.ImpossibleStepException
import de.bms.server.BMotionScriptEngineProvider
import de.prob.model.representation.AbstractModel
import de.prob.scripting.Api
import de.prob.statespace.*
import groovy.util.logging.Slf4j

@Slf4j
public abstract class ProBVisualisation extends BMotion implements IAnimationChangeListener, IModelChangedListener {

    public static final String TRIGGER_MODEL_CHANGED = "ModelChanged";
    public static final String TRIGGER_MODEL_INITIALISED = "ModelInitialised";
    def final AnimationSelector animations;
    def final Api api
    def Trace currentTrace;
    def UUID traceId;

    public ProBVisualisation(final UUID sessionId, final String templatePath,
                             final BMotionScriptEngineProvider scriptEngineProvider) {
        super(sessionId, templatePath, scriptEngineProvider)
        animations = de.prob.Main.getInjector().getInstance(AnimationSelector.class)
        api = de.prob.Main.getInjector().getInstance(Api.class)
        animations.registerAnimationChangeListener(this)
        animations.registerModelChangedListener(this)
    }

    public AbstractModel getModel() {
        return this.currentTrace?.getModel();
    }

    public void setTrace(Trace trace) {
        this.currentTrace = trace
        this.traceId = trace.getUUID()
    }

    public Trace getTrace() {
        return currentTrace;
    }

    public StateSpace getStateSpace() {
        return currentTrace != null ? currentTrace.getStateSpace() : null;
    }

    @Override
    public void traceChange(final Trace changeTrace, final boolean currentAnimationChanged) {

        if (changeTrace != null) {

            if (changeTrace.getUUID() == traceId) {

                // TODO: Is there a better way to check that the current transition is the initialise machine event?
                if (changeTrace.getCurrentTransition().toString().startsWith("\$initialise_machine")) {
                    checkObserver(TRIGGER_MODEL_INITIALISED)
                }
                //def modelFileName = changeTrace.getModel().getModelFile().getName()
                //if (getModel()?.getModelFile()?.getName()?.equals(modelFileName)) {
                if (changeTrace.getCurrentState().
                        isInitialised()) {
                    checkObserver(BMotion.TRIGGER_ANIMATION_CHANGED)
                }

                this.currentTrace = changeTrace

            }

        }

    }

    @Override
    public void modelChanged(StateSpace s) {
        checkObserver(TRIGGER_MODEL_CHANGED)
    }

    public String getCurrentState() {
        return currentTrace != null ? currentTrace.getCurrentState().getId() : null;
    }

    @Override
    public void loadModel(File modelFile, boolean force) {
        if (currentTrace != null) {
            if (force || !currentTrace.getModel().getModelFile().getCanonicalPath().
                    equals(modelFile.getCanonicalPath())) {
                // If a current trace is set and a load was forced, add a new trace
                // and remove the old one from the AnimationSelector
                def oldTrace = this.currentTrace
                this.currentTrace = createNewModelTrace(modelFile.getCanonicalPath())
                animations.addNewAnimation(this.currentTrace)
                animations.removeTrace(oldTrace)
            } else {
                animations.changeCurrentAnimation(currentTrace)
            }
        } else {
            // If no trace exists yet, check if the current trace in the AnimationSelector
            // corresponds to the model path, if not, load a new model and add it to the AnimationSelector
            def selectedTrace = animations.getCurrentTrace()
            if (selectedTrace?.getModel()?.getModelFile()?.getCanonicalPath()?.equals(modelFile.getCanonicalPath())) {
                this.currentTrace = selectedTrace
            } else {
                // Create a new trace for the model and add it to the AnimationSelector
                this.currentTrace = createNewModelTrace(modelFile.getCanonicalPath())
                this.traceId = this.currentTrace.getUUID()
                animations.addNewAnimation(this.currentTrace)
            }
        }
    }

    private Trace createNewModelTrace(String modelPath) {
        if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
            modelPath = modelPath.replace("\\", "\\\\")
        }
        def formalism = getFormalism(modelPath)
        def model = Eval.x(api, "x.${formalism}_load('$modelPath')")
        return new Trace(model);
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
    public Object executeEvent(final data) throws ImpossibleStepException {

        if (trace == null) {
            log.error "BMotion Studio: No currentTrace exists."
        }

        def Trace new_trace
        for (def alt : data.events) {
            new_trace = alt.predicate != null ? executeEventHelper(trace, alt.name, alt.predicate) :
                    executeEventHelper(trace, alt.name, [])
            if (new_trace != null)
                break;
        }
        if (new_trace != null) {
            animations.traceChange(new_trace)
            currentTrace = new_trace
        } else {
            log.error "BMotion Studio: Could not execute any event ..."
        }

        return trace.getCurrentState().getId();

    }

    private Trace executeEventHelper(t, name, pred) {
        try {
            t.execute(name, pred)
        } catch (Exception e) {
            null
        }
    }

    @Override
    public void refresh() {
        checkObserver(TRIGGER_MODEL_CHANGED);
        if (currentTrace != null && currentTrace.getCurrentState().isInitialised()) {
            checkObserver(TRIGGER_MODEL_INITIALISED);
            checkObserver(BMotion.TRIGGER_ANIMATION_CHANGED);
        }
    }

}
