package de.bms.prob

import de.bms.BMotion
import de.bms.BMotionScriptEngineProvider
import de.prob.model.representation.AbstractModel
import de.prob.scripting.Api
import de.prob.statespace.*
import groovy.util.logging.Slf4j

@Slf4j
public abstract class ProBVisualisation extends BMotion implements IAnimationChangeListener, IModelChangedListener {

    public static final String TRIGGER_MODEL_CHANGED = "ModelChanged";
    public static final String TRIGGER_MODEL_INITIALISED = "ModelInitialised";
    public static final String TRIGGER_MODEL_SETUP_CONSTANTS = "ModelSetupConstants";
    def final AnimationSelector animations;
    def final Api api
    def Trace currentTrace;
    def UUID traceId;

    public ProBVisualisation(final UUID id, final BMotionScriptEngineProvider scriptEngineProvider) {
        super(id, scriptEngineProvider)
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
    public void disconnect() {
        animations.deregisterAnimationChangeListener(this)
        animations.deregisterModelChangedListeners(this)
    }

    @Override
    public void traceChange(final Trace ct, final boolean currentAnimationChanged) {

        def changeTrace = animations.getTrace(traceId)

        if (changeTrace != null) {

            if (changeTrace.getUUID() == traceId) {

                def currentTransition = changeTrace.getCurrentTransition()
                def currentState = changeTrace.getCurrentState()
                def clientData = [stateId: currentState.getId(), traceId: traceId]

                // TODO: Is there a better way to check that the current transition is the initialise machine event?
                if (currentTransition.toString().startsWith("\$initialise_machine")) {
                    checkObserver(TRIGGER_MODEL_INITIALISED, clientData)
                } else if (currentTransition.toString().startsWith("\$setup_constants")) {
                    checkObserver(TRIGGER_MODEL_SETUP_CONSTANTS, clientData)
                }
                if (currentState.isInitialised()) {
                    checkObserver(BMotion.TRIGGER_ANIMATION_CHANGED, clientData)
                }

                this.currentTrace = changeTrace

            }

        }

    }

    @Override
    public void modelChanged(StateSpace s) {
        // TODO: fix this!
        def clientData = [stateId: null, traceId: null]
        checkObserver(TRIGGER_MODEL_CHANGED, clientData)
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
                this.traceId = this.currentTrace.getUUID()
                animations.addNewAnimation(this.currentTrace)
                animations.removeTrace(oldTrace)
            } else {
                animations.changeCurrentAnimation(currentTrace)
            }
        } else {
            def found = false;
            //if (mode == BMotionServer.MODE_INTEGRATED || mode == BMotionServer.MODE_ONLINE) {
            for (Trace t : animations.getTraces()) {
                if (t.getModel().getModelFile().getCanonicalPath().equals(modelFile.getCanonicalPath())) {
                    this.currentTrace = t
                    this.traceId = t.getUUID()
                    found = true;
                    break;
                }
            }
            //}
            if (!found) {
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
    public Object executeEvent(final data) {

        if (trace == null) {
            log.error "BMotion Studio: No currentTrace exists."
        }

        def Trace newTrace
        for (def transition : data.events) {
            if (transition.id != null) {
                try {
                    newTrace = trace.add(transition.id);
                } catch (IllegalArgumentException e) {
                }
            } else {
                // Delegate to formalism
                newTrace = getNewTrace(trace, transition);
            }
            if (newTrace != null) break;
        }

        if (newTrace != null) {
            animations.traceChange(newTrace)
            currentTrace = newTrace
        } else {
            log.error "BMotion Studio: Could not execute any event ..."
        }

        return trace.getCurrentState().getId();

    }

    protected abstract Trace getNewTrace(Trace trace, transition);

}
