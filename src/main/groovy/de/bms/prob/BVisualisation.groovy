package de.bms.prob

import de.bms.IllegalFormulaException
import de.bms.server.BMotionScriptEngineProvider
import de.prob.animator.domainobjects.*
import de.prob.statespace.State
import de.prob.statespace.StateSpace
import groovy.util.logging.Slf4j

@Slf4j
public class BVisualisation extends ProBVisualisation {

    private final Map<String, IEvalElement> formulas = new HashMap<String, IEvalElement>();

    public BVisualisation(final UUID sessionId, final String templatePath,
                          final BMotionScriptEngineProvider scriptEngineProvider) {
        super(sessionId, templatePath, scriptEngineProvider);
    }

    @Override
    public Object eval(final String formula) throws IllegalFormulaException {
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
            State sId = space.getState(getCurrentState());
            IEvalResult result = sId.getValues().get(formulas.get(formula));
            return result;
        } catch (EvaluationException e) {
            log.error "BMotion Studio: Formula " + formula + " could not be parsed: " + e.getMessage()
        } catch (Exception e) {
            log.error "BMotion Studio: " + e.getClass() + " thrown: " + e.getMessage()
        }
    }

    public TranslatedEvalResult translate(String formula) throws IllegalFormulaException {
        if (trace == null) {
            log.error "BMotion Studio: No trace exists."
        }
        try {
            StateSpace space = trace.getStateSpace();
            IEvalElement e = formulas.get(formula);
            if (e == null || e instanceof AbstractEvalElement) {
                e = new TranslateFormula(formula as EventB)
                formulas.put(formula, e);
            }
            State sId = space.getState(getCurrentState());
            def result = sId.eval(e)
            return result;
        } catch (EvaluationException e) {
            log.error "BMotion Studio: Formula " + formula + " could not be parsed: " + e.getMessage()
        } catch (Exception e) {
            log.error "BMotion Studio: " + e.getClass() + " thrown: " + e.getMessage()
        }
    }

    @Override
    public Object observe(final d) {
        def map = [:]
        d.data.each { k, v ->
            def t = v.translate ?: false
            def s = v.solutions ?: false
            map.put(k, v.formulas.collect { String formula ->
                def res = !t ? eval(formula) : translate(formula)
                if(res != null) {
                    return s ? res : res.value
                } else {
                    return ""
                }
            })
        }
        return map
    }

    @Override
    public void animatorStatus(final boolean busy) {}

}
