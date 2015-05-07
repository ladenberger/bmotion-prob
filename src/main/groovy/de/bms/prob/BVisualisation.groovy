package de.bms.prob

import de.bms.BMotionException
import de.bms.BMotionScriptEngineProvider
import de.bms.IllegalFormulaException
import de.prob.animator.domainobjects.EvaluationException
import de.prob.animator.domainobjects.IEvalElement
import de.prob.animator.domainobjects.IdentifierNotInitialised
import de.prob.animator.domainobjects.TranslatedEvalResult
import de.prob.exception.ProBError
import de.prob.statespace.State
import de.prob.statespace.StateSpace
import de.prob.statespace.Trace
import groovy.util.logging.Slf4j

@Slf4j
public class BVisualisation extends ProBVisualisation {

    private final Map<String, IEvalElement> formulas = new HashMap<String, IEvalElement>();

    public BVisualisation(final UUID sessionId, final BMotionScriptEngineProvider scriptEngineProvider) {
        super(sessionId, scriptEngineProvider);
    }

    @Override
    public Object eval(final String formula) throws IllegalFormulaException {
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
            StateSpace space = trace.getStateSpace();
            IEvalElement e = formulas.get(formula);
            if (!space.isSubscribed(e)) {
                e = trace.getModel().parseFormula(formula);
                formulas.put(formula, e);
                space.subscribe(this, e);
            }
            State sId = space.getState(stateId);
            return sId.getValues().get(formulas.get(formula))
        } catch (ProBError e) {
            throw new BMotionException(e.getMessage());
        } catch (EvaluationException e) {
            throw new BMotionException("Formula " + formula + " could not be parsed: " + e.getMessage());
        } catch (Exception e) {
            throw new BMotionException(e.getClass().toString() + " thrown: " + e.getMessage());
        }
    }

    public TranslatedEvalResult translate(String formula) throws IllegalFormulaException {
        return translate(formula, getCurrentState());
    }

    public TranslatedEvalResult translate(String formula, String stateId) throws IllegalFormulaException {
        if (trace == null) {
            log.error "BMotion Studio: No trace exists."
        }
        return null;
        /*try {
            StateSpace space = trace.getStateSpace();
            IEvalElement e = formulas.get(formula);
            if (e == null || e instanceof AbstractEvalElement) {
                e = new TranslateFormula(formula as EventB)
                formulas.put(formula, e);
            }
            State sId = space.getState(stateId);
            def result = sId.eval(e)
            return result;
        } catch (EvaluationException e) {
            log.error "BMotion Studio: Formula " + formula + " could not be parsed: " + e.getMessage()
        } catch (Exception e) {
            log.error "BMotion Studio: " + e.getClass() + " thrown: " + e.getMessage()
        }*/
    }

    @Override
    public Object evaluateFormulas(final d) throws BMotionException {
        def formulas = [:]
        def stateId = d.data.stateId ?: getCurrentState()
        d.data.formulas.each { String formula ->
            //def t = v.translate ?: false
            //def s = v.solutions ?: false
            try {
                def result = eval(formula, stateId);
                def resString = null;
                if (result != null && !(result instanceof IdentifierNotInitialised)) {
                    resString = result.value
                }
                //def resTranslate = t ? translate(k, stateId) : null;
                //map.put(k, [result: resString, translate: resTranslate]);
                formulas.put(formula, [result: resString]);
            } catch (BMotionException e) {
                formulas.put(formula, [error: e.getMessage()]);
            }
        }
        return formulas
    }

    @Override
    public void animatorStatus(final boolean busy) {}

    @Override
    protected Trace getNewTrace(Trace trace, transition) {
        try {
            trace.execute(transition.name, transition.predicate ?: [])
        } catch (Exception e) {
            null
        }
    }

}
