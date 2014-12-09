package de.bms.prob

import de.bms.IllegalFormulaException
import de.bms.server.BMotionScriptEngineProvider
import de.prob.animator.domainobjects.EvaluationException
import de.prob.animator.domainobjects.IEvalElement
import de.prob.animator.domainobjects.IEvalResult
import de.prob.statespace.State
import de.prob.statespace.StateSpace

public class CSPVisualisation extends ProBVisualisation {

    private final Map<String, IEvalResult> formulaCache = new HashMap<String, IEvalResult>();

    public CSPVisualisation(final UUID sessionId, final String templatePath,
                            final BMotionScriptEngineProvider scriptEngineProvider) {
        super(sessionId, templatePath, scriptEngineProvider);
    }

    @Override
    public Object eval(final String formula) throws IllegalFormulaException {
        if (trace == null) {
            return null;
        }
        if (!formulaCache.containsKey(formula)) {
            IEvalElement e = trace.getModel().parseFormula(formula);
            StateSpace space = trace.getStateSpace();
            State state = space.getState(getCurrentState());
            if (state != null) {
                formulaCache.put(formula, state.eval(e));
            }
        }
        return formulaCache.get(formula);
    }

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
