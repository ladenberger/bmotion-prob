package de.bms.prob

import de.bms.BMotion
import de.prob.animator.domainobjects.EvaluationException
import de.prob.animator.domainobjects.IEvalElement
import de.prob.animator.domainobjects.IEvalResult
import de.bms.itool.IllegalFormulaException
import de.bms.itool.ImpossibleStepException
import de.bms.itool.ToolRegistry
import de.prob.statespace.State
import de.prob.statespace.StateSpace
import de.prob.statespace.Trace

public class CSPAnimation extends ProBAnimation {

    private final Map<String, IEvalResult> formulaCache = new HashMap<String, IEvalResult>();

    public CSPAnimation(final String toolId, final ToolRegistry toolRegistry) {
        super(toolId, toolRegistry);
    }

    @Override
    public String doStep(final String stateref, final String event, final String... parameters)
            throws ImpossibleStepException {
        try {
            Trace new_trace = trace.execute(event, Arrays.asList(parameters));
            animations.traceChange(new_trace);
            trace = new_trace;
            toolRegistry.notifyToolChange(BMotion.TRIGGER_ANIMATION_CHANGED, this);
        } catch (Exception e) {
            throw new ImpossibleStepException();
        }
        return trace?.getCurrentState().getId();
    }

    @Override
    public Object evaluate(final String stateref, final String formula) throws IllegalFormulaException {
        if (trace == null) {
            return null;
        }
        if (!formulaCache.containsKey(formula)) {
            IEvalElement e = trace.getModel().parseFormula(formula);
            StateSpace space = trace.getStateSpace();
            State state = space.getState(stateref);
            if (state != null) {
                formulaCache.put(formula, state.eval(e));
            }
        }
        return formulaCache.get(formula);
    }

    @Override
    public List<String> getErrors(final String state, final String formula) {
        List<String> errors = new ArrayList<String>();
        if (trace != null) {
            try {
                IEvalElement e = trace.getModel().parseFormula(formula);
                StateSpace space = trace.getStateSpace();
                State state2 = space.getState(state);
                state2.eval(e);
            } catch (EvaluationException e) {
                errors.add("parse error : " + e.getMessage());
            } catch (Exception e) {
                errors.add("thrown " + e.getClass() + " because " + e.getMessage());
            }
        }
        return errors;
    }

    @Override
    public void animatorStatus(final boolean busy) {}

}
