package de.bmotion.prob;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.bmotion.core.BMotion;
import de.bmotion.core.BMotionException;
import de.bmotion.core.BMotionServer;
import de.bmotion.core.objects.FormulaListObject;
import de.bmotion.prob.model.TransitionObject;
import de.bmotion.prob.objects.GraphNodeEdgeObject;
import de.bmotion.prob.objects.GraphObject;
import de.prob.model.representation.AbstractModel;
import de.prob.scripting.Api;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.IModelChangedListener;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.TraceElement;
import de.prob.statespace.Transition;

public abstract class ProBVisualization extends BMotion implements IAnimationChangeListener, IModelChangedListener {

	private final Logger log = LoggerFactory.getLogger(ProBVisualization.class);

	public static final String TRIGGER_MODEL_CHANGED = "ModelChanged";
	public static final String TRIGGER_MODEL_INITIALISED = "ModelInitialised";
	public static final String TRIGGER_MODEL_SETUP_CONSTANTS = "ModelSetupConstants";
	protected final AnimationSelector animations;
	protected final Api api;
	protected Trace trace;
	protected Map<Integer, String> transitionExecutors = new HashMap<Integer, String>();

	public ProBVisualization(String sessionId) {
		super(sessionId, new ProBScriptEngineProvider());
		animations = de.prob.servlet.Main.getInjector().getInstance(AnimationSelector.class);
		api = de.prob.Main.getInjector().getInstance(Api.class);
		animations.registerAnimationChangeListener(this);
		animations.registerModelChangedListener(this);
	}

	public ProBVisualization() {
		this(UUID.randomUUID().toString());
	}

	public AbstractModel getModel() {
		return trace.getModel();
	}

	public void setTrace(Trace t) {
		trace = t;
	}

	public Trace getTrace() {
		return trace;
	}

	public AnimationSelector getAnimationSelector() {
		return animations;
	}

	@Override
	public void disconnect() {
		log.info("ProB Session " + id + " disconnected!");
		animations.deregisterAnimationChangeListener(this);
		animations.deregisterModelChangedListeners(this);
		animations.removeTrace(trace);
	}

	public abstract String getOpString(Transition transition);

	public List<TransitionObject> getHistory(TraceElement currentTraceElement) {

		List<Transition> transitionList = trace.getTransitionList(true);
		List<TransitionObject> transitionObjectList = new ArrayList<TransitionObject>();
		IntStream.range(0, transitionList.size()).forEach(i -> {

			Transition transition = transitionList.get(i);
			String group = "past";
			if (currentTraceElement.getIndex() < i) {
				group = "future";
			} else if (currentTraceElement.getIndex() == i) {
				group = "current";
			}

			TransitionObject transitionObject = new TransitionObject(transition.getName());
			transitionObject.setId(transition.getId());
			transitionObject.setGroup(group);
			transitionObject.setOpString(getOpString(transition));
			transitionObject.setExecutor(transitionExecutors.get(i));
			transitionObject.setIndex(i);
			transitionObject.getParameters().addAll(transition.getParams());
			transitionObject.getReturnValues().addAll(transition.getReturnValues());
			transitionObjectList.add(transitionObject);

		});

		return transitionObjectList;

	}

	@Override
	public void traceChange(final Trace ct, final boolean currentAnimationChanged) {

		if (trace != null) {

			Trace changeTrace = animations.getTrace(trace.getUUID());

			// Only execute if trace id is current trace id (same animation)
			// if (ct != null && ct.getUUID() == trace.getUUID()) {
			// if (changeTrace != null && changeTrace.getUUID() ==
			// trace.getUUID()) {
			if (changeTrace != null && !changeTrace.equals(trace)) {

				Transition currentTransition = changeTrace.getCurrentTransition();
				State currentState = changeTrace.getCurrentState();
				toolData.put("stateId", currentState.getId());
				toolData.put("lastOperation", currentTransition.toString());

				// TODO: Is there a better way to check that the current
				// transition
				// is the initialise machine event?
				if (currentTransition.toString().startsWith("$initialise_machine")) {
					checkObserver(TRIGGER_MODEL_INITIALISED);
				} else if (currentTransition.toString().startsWith("$setup_constants")) {
					checkObserver(TRIGGER_MODEL_SETUP_CONSTANTS);
				}
				toolData.put("initialized", currentState.isInitialised());
				checkObserver(TRIGGER_ANIMATION_CHANGED);

				trace = changeTrace;

				clients.forEach(client -> client.sendEvent("observeHistory", getHistory(trace.getCurrent())));

			}

		}

	}

	@Override
	public void modelChanged(StateSpace s) {
		// TODO: Implement me!
	}

	protected abstract void updateModelData(Trace t);

	@Override
	public void initModel(String model, Map<String, String> options, String mode) throws BMotionException {

		if (mode == BMotionServer.MODE_INTEGRATED) {
			trace = animations.getCurrentTrace();
		} else {

			File modelFile = new File(model);
			if (modelFile.exists()) {

				log.info("BMotionWeb: Loading model " + model);

				try {

					trace = createNewModelTrace(modelFile.getCanonicalPath(), options);
					animations.addNewAnimation(trace);

				} catch (IOException e) {
					throw new BMotionException("An error occured while loading model " + model + ": " + e.getMessage());
				}

			} else {
				throw new BMotionException("No file exists at path " + model);
			}

		}

		if (trace == null) {
			throw new BMotionException("Could not create or get trace. Please animate a model.");
		}

		toolData.put("stateId", trace.getCurrentState().getId());
		toolData.put("traceId", trace.getUUID().toString());
		toolData.put("initialized", trace.getCurrentState().isInitialised());
		toolData.put("lastOperation", trace.getCurrentState().toString());

		updateModelData(trace);

	}

	private Trace createNewModelTrace(String modelPath, Map<String, String> options) throws BMotionException {

		StateSpace s;
		try {
			String fileExtension = modelPath.substring(modelPath.length() - 3);
			switch (fileExtension) {
			case "csp":
				s = api.csp_load(modelPath, options);
				break;
			case "buc":
			case "bcc":
			case "bum":
			case "bcm":
				s = api.eventb_load(modelPath, options);
				break;
			case "mch":
				s = api.b_load(modelPath, options);
				break;
			case "tla":
				s = api.tla_load(modelPath, options);
				break;
			default:
				throw new BMotionException("Unknown model " + modelPath);
			}
		} catch (Exception e) {
			throw new BMotionException("An error occured while loading model " + modelPath + ": " + e.getMessage());
		}

		return new Trace(s);

	}

	public GraphObject createTraceDiagram() {
		return createTraceDiagram(Collections.emptyMap());
	}

	public GraphObject createTraceDiagram(Map<String, FormulaListObject> formulas) {

		List<GraphNodeEdgeObject> nodes = new ArrayList<GraphNodeEdgeObject>();
		List<GraphNodeEdgeObject> edges = new ArrayList<GraphNodeEdgeObject>();

		GraphNodeEdgeObject rootObj = new GraphNodeEdgeObject("nodes");
		rootObj.getData().put("id", "root");
		rootObj.getData().put("label", "root");

		nodes.add(rootObj);

		IntStream.range(0, trace.getTransitionList().size()).forEach(i -> {

			Transition transition = trace.getTransitionList().get(i);
			String sId = transition.getSource().getId();
			String dId = transition.getDestination().getId();

			GraphNodeEdgeObject nodeObj = new GraphNodeEdgeObject("nodes");
			nodeObj.getData().put("id", dId);
			nodeObj.getData().put("label", dId);
			nodeObj.getData().put("index", i);

			if (!formulas.isEmpty()) {

				formulas.values().forEach(fList -> {
					fList.getFormulas().forEach(fObj -> fObj.getOptions().put("stateId", dId));
				});

				try {
					Map<String, Map<String, Object>> evaluateFormulas = evaluateFormulas(formulas);
					nodeObj.setResults(evaluateFormulas);
				} catch (BMotionException e) {
					nodeObj.getErrors().add(e.getMessage());
				}

			}

			nodes.add(nodeObj);

			GraphNodeEdgeObject edgeObj = new GraphNodeEdgeObject("edges");
			edgeObj.getData().put("id", "e" + sId + "" + dId);
			edgeObj.getData().put("label", getOpString(transition));
			edgeObj.getData().put("source", sId);
			edgeObj.getData().put("target", dId);
			edges.add(edgeObj);

		});

		return new GraphObject(nodes, edges);

	}

	public void gotoTraceIndex(int index) throws BMotionException {

		Trace newTrace = trace.gotoPosition(Integer.valueOf(index));

		if (newTrace == null) {
			throw new BMotionException("Could not got to trace index " + index);
		}

		animations.traceChange(newTrace);
		trace = newTrace;

	}

	@Override
	public void animatorStatus(final boolean busy) {
	}

	@Override
	public void sessionLoaded() {
		toolData.put("stateId", trace.getCurrentState().getId());
		toolData.put("traceId", trace.getUUID().toString());
		toolData.put("initialized", trace.getCurrentState().isInitialised());
		toolData.put("lastOperation", trace.getCurrentState().toString());
	}

}
