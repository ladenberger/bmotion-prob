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
            log.error "BMotion Studio: No trace exists."
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

    @Override
    public void animatorStatus(final boolean busy) {}

}
