package de.bms.prob

import de.bms.BMotionException
import de.bms.BMotionScriptEngineProvider
import de.prob.animator.domainobjects.EvaluationException
import de.prob.animator.domainobjects.IEvalElement
import de.prob.exception.ProBError
import de.prob.statespace.State
import de.prob.statespace.StateSpace
import de.prob.statespace.Trace
import de.prob.statespace.Transition
import groovy.util.logging.Slf4j

@Slf4j
public class CSPVisualisation extends ProBVisualisation {

    private final formulaCache = [:]

    public CSPVisualisation(final UUID id, final BMotionScriptEngineProvider scriptEngineProvider) {
        super(id, scriptEngineProvider)
    }

    @Override
    public Object eval(final String formula) throws BMotionException {
        return eval(formula, getCurrentState())
    }

    public Object eval(final String formula, final String stateId) throws BMotionException {
        if (trace == null) {
            throw new BMotionException("No trace exists.")
        }
        if (formula == null) {
            throw new BMotionException("Formula must not be null.")
        }
        try {
            if (!formulaCache.containsKey(formula) && formula != null) {
                IEvalElement e = trace.getModel().parseFormula(formula)
                StateSpace space = trace.getStateSpace()
                State state = space.getState(stateId)
                if (state != null) {
                    formulaCache.put(formula, state.eval(e))
                }
            }
        } catch (ProBError e) {
            throw new BMotionException(e.getMessage())
        } catch (EvaluationException e) {
            throw new BMotionException("Formula " + formula + " could not be parsed: " + e.getMessage())
        } catch (Exception e) {
            throw new BMotionException(e.getClass().toString() + " thrown: " + e.getMessage())
        }
        return formulaCache.get(formula)
    }

    @Override
    public Object translate(String result) throws BMotionException {
        throw new BMotionException("Translation not supported in CSP")
    }

    @Override
    public Object evaluateFormulas(final d) throws BMotionException {
        def formulas = [:]
        def String stateId = d.data.stateId ?: getCurrentState()
        d.data.formulas.each {
            def String f = it['formula']
            try {
                def result = eval(f, stateId)
                def resString = null
                if (result != null) {
                    resString = result['value']
                }
                formulas.put(f, [result: resString])
            } catch (BMotionException e) {
                formulas.put(f, [error: e.getMessage()])
            }
        }
        return formulas
    }

    @Override
    public void animatorStatus(final boolean busy) {}

    @Override
    protected Trace getNewTrace(Trace trace, String transitionName, String transitionPredicate) {
        def cop = trace.getNextTransitions().find { Transition t ->
            t.getRep().equals(transitionName)
        }
        return cop != null ? trace.add(cop) : trace
    }

}
