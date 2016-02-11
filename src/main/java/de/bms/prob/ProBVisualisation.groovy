package de.bms.prob

import de.bms.BMotion
import de.bms.BMotionException
import de.bms.BMotionScriptEngineProvider
import de.prob.model.classicalb.Operation
import de.prob.model.eventb.Event
import de.prob.model.eventb.EventBModel
import de.prob.model.representation.AbstractElement
import de.prob.model.representation.AbstractModel
import de.prob.model.representation.BEvent
import de.prob.model.representation.Machine
import de.prob.scripting.Api
import de.prob.statespace.*
import groovy.util.logging.Slf4j

@Slf4j
public abstract class ProBVisualisation extends BMotion implements IAnimationChangeListener, IModelChangedListener {

    def static final String TRIGGER_MODEL_CHANGED = "ModelChanged"
    def static final String TRIGGER_MODEL_INITIALISED = "ModelInitialised"
    def static final String TRIGGER_MODEL_SETUP_CONSTANTS = "ModelSetupConstants"
    def final AnimationSelector animations
    def final Api api
    def Trace trace
    def UUID traceId

    public ProBVisualisation(final UUID id, final BMotionScriptEngineProvider scriptEngineProvider) {
        super(id, scriptEngineProvider)
        animations = de.prob.servlet.Main.getInjector().getInstance(AnimationSelector.class)
        api = de.prob.Main.getInjector().getInstance(Api.class)
        animations.registerAnimationChangeListener(this)
        animations.registerModelChangedListener(this)
    }

    public AbstractModel getModel() {
        return trace?.getModel()
    }

    public void setTrace(Trace t) {
        trace = t
        traceId = trace.getUUID()
    }

    public StateSpace getStateSpace() {
        return trace?.getStateSpace()
    }

    public String getCurrentState() {
        return trace?.getCurrentState()
    }

    @Override
    public void disconnect() {
        log.info("ProB Session " + id + " disconnected!")
        animations.deregisterAnimationChangeListener(this)
        animations.deregisterModelChangedListeners(this)
    }

    @Override
    public void traceChange(final Trace ct, final boolean currentAnimationChanged) {

        def changeTrace = animations.getTrace(traceId)

        // Only execute if trace id is current trace id (same animation)
        if (changeTrace != null && changeTrace.getUUID() == traceId) {

            def currentTransition = changeTrace.getCurrentTransition()
            def currentState = changeTrace.getCurrentState()
            clientData["stateId"] = currentState.getId()
            clientData["lastOperation"] = currentTransition.toString()
            def cData = [stateId: currentState.getId(), traceId: traceId]
            // TODO: Is there a better way to check that the current transition is the initialise machine event?
            if (currentTransition.toString().startsWith("\$initialise_machine")) {
                checkObserver(TRIGGER_MODEL_INITIALISED, cData)
            } else if (currentTransition.toString().startsWith("\$setup_constants")) {
                checkObserver(TRIGGER_MODEL_SETUP_CONSTANTS, cData)
            }
            if (currentState.isInitialised()) {
                checkObserver(TRIGGER_ANIMATION_CHANGED, cData)
                clientData["initialised"] = true
            }

            trace = changeTrace

        }

    }

    @Override
    public void modelChanged(StateSpace s) {
        // TODO: fix this!
        def clientData = [stateId: null, traceId: null]
        checkObserver(TRIGGER_MODEL_CHANGED, clientData)
    }

    private updateModelData(Trace t) {

        // Update model events
        clientData["model"]["events"] = []
        AbstractElement mainComponent = t.getStateSpace().getMainComponent()
        if (mainComponent instanceof Machine) {

            // Collect sets
            clientData["model"]["sets"] = mainComponent.getChildrenOfType(de.prob.model.representation.Set.class).collect {
                return it.getName()
            }

            // Collect events/operations
            def events = mainComponent.getChildrenOfType(BEvent.class)
            for (BEvent e in events) {
                if (e instanceof Event) {
                    clientData["model"]["events"] << [
                            name     : e.getName(),
                            parameter: e.getParameters().collect {
                                return [name: it.getName(), comment: it.getComment()]
                            },
                            guards   : e.getGuards().collect {
                                return [name: it.getName(), comment: it.getComment(), isTheorem: it.isTheorem()]
                            }
                    ]
                } else if (e instanceof Operation) {
                    clientData["model"]["events"] << [
                            name     : e.getName(),
                            parameter: e.getParameters().collect {
                                return [name: it]
                            }
                    ]
                }
            }

        }

        // Update model refinements
        if (t.getModel() instanceof EventBModel) {
            clientData["model"]["refinements"] = ((EventBModel) t.getModel()).getMachines().collect {
                return it.name
            }
        }

    }

    @Override
    protected void initSession(options) throws BMotionException {
        Trace t = getTrace()
        getClientData().put("stateId", t.getCurrentState().getId())
        getClientData().put("traceId", t.getUUID())
        getClientData().put("initialised", t.getCurrentState().isInitialised())
    }

    @Override
    protected void initModel(String modelPath, options) throws BMotionException {

        File modelFile = new File(modelPath)
        if (modelFile.exists()) {

            log.info "BMotion Studio: Loading model " + modelPath

            clientData["model"] = [:]

            // Get or create trace
            if (trace != null) {
                if (!trace.getModel().getModelFile().getCanonicalPath().
                        equals(modelFile.getCanonicalPath())) {
                    // If a current trace is set and a load was forced, add a new trace
                    // and remove the old one from the AnimationSelector
                    def oldTrace = trace
                    trace = createNewModelTrace(modelFile.getCanonicalPath(), options.preferences)
                    traceId = trace.getUUID()
                    animations.addNewAnimation(trace)
                    animations.removeTrace(oldTrace)
                } else {
                    animations.changeCurrentAnimation(trace)
                }
            } else {
                trace = animations.getTraces().find() { Trace t ->
                    t.getModel()?.getModelFile()?.getCanonicalPath()?.equals(modelFile.getCanonicalPath())
                }
                if (trace == null) {
                    // Create a new trace for the model and add it to the AnimationSelector
                    trace = createNewModelTrace(modelFile.getCanonicalPath(), options.preferences)
                    traceId = trace.getUUID()
                    animations.addNewAnimation(trace)
                }
            }

            updateModelData(trace);
            clientData["traceId"] = traceId;

        } else {
            throw new BMotionException("Model " + modelPath + " does not exist")
        }

    }

    private Trace createNewModelTrace(String modelPath, preferences) {
        if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
            modelPath = modelPath.replace("\\", "\\\\")
        }
        def StateSpace s = api."${getFormalism(modelPath)}_load"(modelPath, preferences)
        return new Trace(s)
    }

    protected static String getFormalism(final String modelPath) {
        switch (modelPath[-3..-1]) {
            case "csp":
                return "csp";
                break
            case { it == "buc" || it == "bcc" || it == "bum" || it == "bcm" }:
                return "eventb";
                break
            case "mch":
                return "b";
                break
            case "tla":
                return "tla";
                break
            default:
                return null;
                break;
        }
    }

    @Override
    public Object executeEvent(final data) {

        def returnValue = [:]
        def errors = []

        if (trace == null) {
            errors << "No trace exists."
        } else {
            def String eventId = data['event']['id']
            def String eventName = data['event']['name']
            def String eventPredicate = data['event']['predicate']
            returnValue['event'] = data['event']
            try {
                def Trace newTrace = eventId != null ? trace.add(eventId) : getNewTrace(trace, eventName, eventPredicate)
                animations.traceChange(newTrace)
                trace = newTrace
                returnValue['stateId'] = trace.getCurrentState().getId()
                returnValue['returnValues'] = newTrace.getCurrentTransition().getReturnValues()
            } catch (IllegalArgumentException e) {
                errors << 'Could not execute any event with name ' + eventName + ' and predicate ' + eventPredicate + ', Message: ' + e.getMessage()
            }
        }

        if (!errors.isEmpty()) returnValue['errors'] = errors

        return returnValue

    }

    protected
    abstract Trace getNewTrace(Trace trace, String transitionName, String transitionPredicate) throws IllegalArgumentException

}
