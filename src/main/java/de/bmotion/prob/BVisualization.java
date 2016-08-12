package de.bmotion.prob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.bmotion.core.BMotionEvalException;
import de.bmotion.core.BMotionException;
import de.bmotion.core.objects.FormulaListObject;
import de.bmotion.core.objects.FormulaObject;
import de.bmotion.core.objects.FormulaReturnObject;
import de.bmotion.prob.model.ModelObject;
import de.bmotion.prob.objects.BEventReturnObject;
import de.bmotion.prob.objects.GraphNodeEdgeObject;
import de.bmotion.prob.objects.GraphObject;
import de.prob.animator.command.GetTransitionDiagramCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.DotEdge;
import de.prob.animator.domainobjects.DotNode;
import de.prob.animator.domainobjects.EvalElementType;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob.translator.Translator;
import de.prob.translator.types.Atom;
import de.prob.translator.types.BObject;

public abstract class BVisualization extends ProBVisualization {

	private final Map<String, IEvalElement> formulas = new HashMap<String, IEvalElement>();

	public BVisualization(String sessionId) {
		super(sessionId);
	}

	public BVisualization() {
		super();
	}

	@Override
	protected void updateModelData(Trace t) {
		ModelObject modelObject = new ModelObject();
		toolData.put("model", modelObject);
		updateBModelData(t);
	}

	protected abstract void updateBModelData(Trace t);

	@Override
	public Object eval(String formula, Map<String, Object> options) throws BMotionException {

		if (trace == null) {
			throw new BMotionEvalException("No trace exists", formula);
		}

		if (formula == null) {
			throw new BMotionEvalException("Formula must not be null", formula);
		}

		if (!trace.getCurrentState().isInitialised()) {
			throw new BMotionEvalException("Model must be initialized", formula);
		}

		Object stateId = options.get("stateId");
		if (stateId == null) {
			stateId = trace.getCurrentState().toString();
		}
		return eval(formula, options, String.valueOf(stateId));

	}

	private Object eval(String formula, Map<String, Object> options, String stateId) throws BMotionException {

		try {
			StateSpace space = trace.getStateSpace();
			IEvalElement e = formulas.get(formula);
			if (!space.isSubscribed(e)) {
				e = getEvalElement(formula);
				formulas.put(formula, e);
				space.subscribe(this, e);
			}
			State sId = space.getState(stateId);
			AbstractEvalResult res = sId.getValues().get(e);

			if (res instanceof EvalResult) {

				String stringRes = ((EvalResult) res).getValue();
				boolean translate = false;
				if (options.get("translate") != null) {
					translate = (boolean) options.get("translate");
				}
				if (translate) {
					return translate(stringRes);
				} else {
					return stringRes;
				}

			} else {
				List<String> errors = Collections.emptyList();
				if (res instanceof EvaluationErrorResult) {
					errors = ((EvaluationErrorResult) res).getErrors();
				}
				throw new BMotionException("Formula " + formula + " could not be evaluated " + errors);
			}

		} catch (EvaluationException e) {
			throw new BMotionException("Formula " + formula + " could not be parsed: " + e.getMessage());
		} catch (Exception e) {
			throw new BMotionException("Formula " + formula + " could not be evaluated: " + e.getMessage());
		}

	}

	private Object convert(BObject obj) {
		if (obj instanceof de.prob.translator.types.Set) {
			de.prob.translator.types.Set set = (de.prob.translator.types.Set) obj;
			return set.stream().map(item -> convert(item)).collect(Collectors.toList());
		} else if (obj instanceof de.prob.translator.types.Sequence) {
			de.prob.translator.types.Sequence sequence = (de.prob.translator.types.Sequence) obj;
			return sequence.stream().map(item -> convert(item)).collect(Collectors.toList());
		} else if (obj instanceof de.prob.translator.types.Tuple) {
			de.prob.translator.types.Tuple tuple = (de.prob.translator.types.Tuple) obj;
			List<Object> convertedTuple = new ArrayList<Object>();
			convertedTuple.add(convert(tuple.getFirst()));
			convertedTuple.add(convert(tuple.getSecond()));
			return convertedTuple;
		} else if (obj instanceof de.prob.translator.types.Boolean) {
			return ((de.prob.translator.types.Boolean) obj).booleanValue();
		} else if (obj instanceof de.prob.translator.types.String) {
			return ((de.prob.translator.types.String) obj).getValue();
		} else if (obj instanceof Atom) {
			return ((Atom) obj).getValue();
		}
		return obj;
	}

	public Object translate(String result) throws BMotionException {
		try {
			return convert(Translator.translate(result));
		} catch (BException e) {
			throw new BMotionException("An error occurred while translating " + result + ": " + e.getMessage());
		}
	}

	@Override
	public Object executeEvent(Map<String, String> options) throws BMotionException {

		if (trace == null) {
			throw new BMotionException("Could not execute event because no trace exists.");
		}

		Trace newTrace;

		String transitionName = options.get("name");
		String transitionPredicate = options.get("predicate");

		if (transitionPredicate != null) {
			transitionPredicate = transitionPredicate.length() == 0 ? "TRUE=TRUE" : transitionPredicate;
			newTrace = trace.execute(transitionName, transitionPredicate);
		} else {
			newTrace = trace.execute(transitionName);
		}

		if (newTrace == null) {
			throw new BMotionException("Could not execute event " + transitionName + " " + transitionPredicate);
		}

		transitionExecutors.put(newTrace.getCurrent().getIndex(), options.get("executor"));
		animations.traceChange(newTrace);
		trace = newTrace;

		BEventReturnObject returnObject = new BEventReturnObject(newTrace.getCurrentState().getId());
		returnObject.getReturnValues().addAll(newTrace.getCurrentTransition().getReturnValues());
		return returnObject;

	}

	@Override
	public String getOpString(Transition transition) {
		return transition.getPrettyRep();
	}

	public Map<String, String> convertFormulasToExpressions(List<FormulaObject> list) {

		Map<String, String> convertedMap = new HashMap<String, String>();

		list.forEach(f -> {

			String convertedExpression;

			IEvalElement e = trace.getModel().parseFormula(f.getFormula());
			if (e.getKind() == EvalElementType.PREDICATE.toString()) {
				convertedExpression = "bool(" + e.getCode() + ")";
			} else {
				convertedExpression = e.getCode();
			}

			convertedMap.put(f.getFormula(), convertedExpression);

		});

		return convertedMap;

	}

	public GraphObject createProjectionDiagram(Map<String, FormulaListObject> formulas) {

		List<FormulaObject> allFormulas = new LinkedList<FormulaObject>();
		formulas.values().forEach(formulasObj -> {
			allFormulas.addAll(formulasObj.getFormulas());
		});

		Map<String, String> formulasToExpressionMaps = convertFormulasToExpressions(allFormulas);

		List<String> convertedExpressions = formulasToExpressionMaps.values().stream().map(f -> {
			return f;
		}).collect(Collectors.toList());

		int formulasAmount = convertedExpressions.size();

		IEvalElement eval = trace.getModel().parseFormula(String.join("|->", convertedExpressions));
		GetTransitionDiagramCommand cmd = new GetTransitionDiagramCommand(eval);
		StateSpace stateSpace = trace.getStateSpace();
		stateSpace.execute(cmd);

		// Collect nodes
		List<GraphNodeEdgeObject> nodes = cmd.getNodes().entrySet().stream().map(entry -> {

			DotNode dotNode = entry.getValue();
			GraphNodeEdgeObject node = new GraphNodeEdgeObject("nodes");
			node.getData().put("id", dotNode.getId());
			node.getData().put("color", dotNode.getColor());
			node.getData().put("count", dotNode.getCount());
			node.getData().put("labels", dotNode.getLabels());

			if (!dotNode.getLabels().contains("<< undefined >>") && !dotNode.getId().equals("1")) {

				List<Object> translatedResults = new LinkedList<Object>();
				dotNode.getLabels().forEach(label -> {
					try {
						Object trans = translate(label);
						translatedResults.add(trans);
					} catch (BMotionException e) {
						node.getErrors().add(e.getMessage());
					}
				});

				List<Object> flattenResultList = flattenList(translatedResults, formulasAmount, 0);

				if (node.getErrors().isEmpty()) {

					Map<String, Map<String, Object>> results = formulas.entrySet().stream()
							.collect(Collectors.toMap(Map.Entry::getKey, e -> {

								FormulaListObject list = (FormulaListObject) e.getValue();
								Map<String, Object> collect = list.getFormulas().stream()
										.collect(Collectors.toMap(obj -> obj.getFormula(), obj -> {

											FormulaReturnObject returnObj = new FormulaReturnObject();

											Boolean translate = false;
											Object translateOption = obj.getOptions().get("translate");
											if (translateOption != null) {
												translate = Boolean.valueOf(translateOption.toString());
											}

											Object foundObj = convertedExpressions.stream().filter(t -> {
												return t.equals(formulasToExpressionMaps.get(obj.getFormula()));
											}).findFirst().orElse(null);

											Object fRes = flattenResultList.get(convertedExpressions.indexOf(foundObj));

											if (translate) {
												returnObj.setResult(fRes);
											} else {
												returnObj.setResult(reTranslate(fRes));
											}

											return returnObj;

										}));

								return collect;

							}));

					node.setResults(results);

				}

			}

			return node;

		}).collect(Collectors.toList());

		// Collect edges
		List<GraphNodeEdgeObject> edges = cmd.getEdges().entrySet().stream().map(entry -> {
			GraphNodeEdgeObject edge = new GraphNodeEdgeObject("edges");
			DotEdge dotEdge = entry.getValue();
			edge.getData().put("id", dotEdge.getSource() + dotEdge.getTarget() + "_" + dotEdge.getLabel());
			edge.getData().put("label", dotEdge.getLabel());
			edge.getData().put("style", dotEdge.getStyle());
			edge.getData().put("color", dotEdge.getColor());
			edge.getData().put("source", dotEdge.getSource());
			edge.getData().put("target", dotEdge.getTarget());
			return edge;
		}).collect(Collectors.toList());

		return new GraphObject(nodes, edges);

	}

	@SuppressWarnings("unchecked")
	public List<Object> flattenList(List<Object> nestedList, int nr, int deep) {
		List<Object> flatList = new LinkedList<Object>();
		for (Object obj : nestedList) {
			if (obj instanceof ArrayList && deep < nr - 1) {
				for (Object el : flattenList((ArrayList<Object>) obj, nr, deep + 1)) {
					flatList.add(el);
				}
			} else {
				flatList.add(obj);
			}
		}
		return flatList;
	}

	public String reTranslate(Object obj) {
		if (obj instanceof Boolean) {
			if (Boolean.valueOf(obj.toString())) {
				return "TRUE";
			} else {
				return "FALSE";
			}
		} else {
			return obj.toString();
		}

	}

	protected abstract IEvalElement getEvalElement(String formula);

}
