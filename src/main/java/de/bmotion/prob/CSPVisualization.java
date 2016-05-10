package de.bmotion.prob;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.bmotion.core.BMotionException;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;

public class CSPVisualization extends ProBVisualization {

	private final Map<String, EvalResult> formulaCache = new HashMap<String, EvalResult>();

	public CSPVisualization(String sessionId) {
		super(sessionId);
	}

	public CSPVisualization() {
		super();
	}

	@Override
	protected void updateModelData(Trace t) {
	}

	@Override
	public Object executeEvent(Map<String, String> options) throws BMotionException {

		if (trace == null) {
			throw new BMotionException("Could not execute event because no trace exists.");
		}

		String transitionName = options.get("name");
		Transition transition = trace.getNextTransitions().stream().filter(t -> {
			String parameterString = String.join(".", t.getParams());
			String eventName = t.getParams().size() > 0 ? t.getName() + "." : t.getName();
			return (eventName + parameterString).equals(transitionName);
		}).findFirst().orElse(null);

		if (transition == null) {
			throw new BMotionException("Could not execute event " + transitionName);
		}

		Trace newTrace = trace.add(transition);
		transitionExecutors.put(newTrace.getCurrent().getIndex(), options.get("executor"));
		animations.traceChange(newTrace);
		trace = newTrace;

		return null;

	}

	@Override
	public Object eval(String formula, Map<String, Object> options) throws BMotionException {

		if (trace == null) {
			throw new BMotionException("No trace exists.");
		}

		if (formula == null) {
			throw new BMotionException("Formula must not be null.");
		}

		return eval(formula, getTrace().getCurrentState().toString());

	}

	private Object eval(final String formula, final String stateId) throws BMotionException {

		try {

			if (!formulaCache.containsKey(formula) && formula != null) {

				IEvalElement e = getTrace().getModel().parseFormula(formula);
				StateSpace space = getTrace().getStateSpace();
				State state = space.getState(stateId);

				AbstractEvalResult res = state.eval(e);

				if (res instanceof EvalResult) {
					formulaCache.put(formula, (EvalResult) res);
				} else {
					List<String> errors = Collections.emptyList();
					if (res instanceof EvaluationErrorResult) {
						errors = ((EvaluationErrorResult) res).getErrors();
					}
					throw new BMotionException("Formula " + formula + " could not be evaluated " + errors);
				}

			}

		} catch (EvaluationException e) {
			throw new BMotionException("Formula " + formula + " could not be parsed: " + e.getMessage());
		} catch (Exception e) {
			throw new BMotionException("Formula " + formula + " could not be evaluated: " + e.getMessage());
		}

		return formulaCache.get(formula).getValue();

	}

	@Override
	public String getOpString(Transition transition) {
		return transition.getPrettyRep();
	}

}
