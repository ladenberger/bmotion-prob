package de.bms.prob

import de.bms.BMotionScriptEngineProvider
import de.bms.IllegalFormulaException
import de.prob.animator.domainobjects.IEvalElement
import de.prob.animator.domainobjects.IdentifierNotInitialised
import de.prob.statespace.State
import de.prob.statespace.StateSpace
import de.prob.statespace.Trace

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
    public Object observe(final d) {
        def map = [:]
        d.data.observers.each { String k, v ->
            def result = eval(k);
            def resString = null;
            if (result != null && !(result instanceof IdentifierNotInitialised)) {
                resString = result.value
            }
            map.put(k, [result: resString]);
        }
        return map
    }

    @Override
    public void animatorStatus(final boolean busy) {}

    @Override
    protected Trace getNewTrace(Trace trace, transition) {
        for (cop in trace.getNextTransitions()) {
            if (cop.getRep().equals(transition.name)) {
                return trace.add(cop);
            }
        }
    }

}
