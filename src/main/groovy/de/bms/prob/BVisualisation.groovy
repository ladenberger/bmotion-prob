package de.bms.prob

import de.bms.IllegalFormulaException
import de.bms.server.BMotionScriptEngineProvider
import de.prob.animator.domainobjects.EvaluationException
import de.prob.animator.domainobjects.IEvalElement
import de.prob.animator.domainobjects.IdentifierNotInitialised
import de.prob.animator.domainobjects.TranslatedEvalResult
import de.prob.statespace.State
import de.prob.statespace.StateSpace
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

    public Object eval(final String formula, final String stateid) throws IllegalFormulaException {
        if (trace == null) {
            log.error "BMotion Studio: No trace exists."
        }
        try {
            StateSpace space = trace.getStateSpace();
            IEvalElement e = formulas.get(formula);
            if (!space.isSubscribed(e)) {
                e = trace.getModel().parseFormula(formula);
                formulas.put(formula, e);
                space.subscribe(this, e);
            }
            State sId = space.getState(stateid);
            return sId.getValues().get(formulas.get(formula));
        } catch (EvaluationException e) {
            log.error "BMotion Studio: Formula " + formula + " could not be parsed: " + e.getMessage()
        } catch (Exception e) {
            log.error "BMotion Studio: " + e.getClass() + " thrown: " + e.getMessage()
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
    public Object observe(final d) {
        def map = [:]
        def stateId = d.data.stateId ?: getCurrentState()
        d.data.observers.each { String k, v ->
            def t = v.translate ?: false
            //def s = v.solutions ?: false
            def result = eval(k, stateId);
            def resString = null;
            if (result != null && !(result instanceof IdentifierNotInitialised)) {
                resString = result.value
            }
            def resTranslate = t ? translate(k, stateId) : null;
            map.put(k, [result: resString, translate: resTranslate]);
        }
        return map
    }

    @Override
    public void animatorStatus(final boolean busy) {}

}
