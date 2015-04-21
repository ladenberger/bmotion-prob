package de.bms.prob

import de.bms.IllegalFormulaException
import de.bms.server.BMotionScriptEngineProvider
import de.prob.animator.domainobjects.IEvalElement
import de.prob.statespace.State
import de.prob.statespace.StateSpace

public class CSPVisualisation extends ProBVisualisation {

    private final formulaCache = [:]

    public CSPVisualisation(final UUID sessionId, final BMotionScriptEngineProvider scriptEngineProvider) {
        super(sessionId, scriptEngineProvider);
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
