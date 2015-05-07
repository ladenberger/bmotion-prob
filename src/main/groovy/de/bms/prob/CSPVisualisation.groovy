package de.bms.prob

import de.bms.BMotionException
import de.bms.BMotionScriptEngineProvider
import de.prob.animator.domainobjects.EvaluationException
import de.prob.animator.domainobjects.IEvalElement
import de.prob.animator.domainobjects.IdentifierNotInitialised
import de.prob.exception.ProBError
import de.prob.statespace.State
import de.prob.statespace.StateSpace
import de.prob.statespace.Trace
import groovy.util.logging.Slf4j

@Slf4j
public class CSPVisualisation extends ProBVisualisation {

    private final formulaCache = [:]

    public CSPVisualisation(final UUID sessionId, final BMotionScriptEngineProvider scriptEngineProvider) {
        super(sessionId, scriptEngineProvider);
    }

    @Override
    public Object eval(final String formula) throws BMotionException {
        return eval(formula, getCurrentState());
    }

    public Object eval(final String formula, final String stateId) throws BMotionException {
        if (trace == null) {
            throw new BMotionException("No trace exists.");
        }
        if (formula == null) {
            throw new BMotionException("BMotion Studio: Formula must not be null.");
        }
        try {
            if (!formulaCache.containsKey(formula) && formula != null) {
                IEvalElement e = trace.getModel().parseFormula(formula);
                StateSpace space = trace.getStateSpace();
                State state = space.getState(stateId);
                if (state != null) {
                    formulaCache.put(formula, state.eval(e));
                }
            }
        } catch (ProBError e) {
            throw new BMotionException(e.getMessage());
        } catch (EvaluationException e) {
            throw new BMotionException("Formula " + formula + " could not be parsed: " + e.getMessage());
        } catch (Exception e) {
            throw new BMotionException(e.getClass().toString() + " thrown: " + e.getMessage());
        }
        return formulaCache.get(formula);
    }

    @Override
    public Object evaluateFormulas(final d) throws BMotionException {
        def formulas = [:]
        d.data.formulas.each { String formula ->
            if (formula != null) {
                try {
                    def result = eval(formula);
                    def resString = null;
                    if (result != null && !(result instanceof IdentifierNotInitialised)) {
                        resString = result.value
                    }
                    formulas.put(formula, [result: resString]);
                } catch (BMotionException e) {
                    formulas.put(formula, [error: e.getMessage()]);
                }
            }
        }
        return formulas
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
