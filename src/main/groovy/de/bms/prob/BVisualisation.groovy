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
            log.error "BMotion Studio: No currentTrace exists."
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
            throw new IllegalFormulaException("BMotion Studio: No currentTrace exists.");
        }
        try {
            StateSpace space = trace.getStateSpace();
            IEvalElement e = formulas.get(formula);
            if (e == null) {
                e = new TranslateFormula(formula as EventB)
                formulas.put(formula, e);
            }
            State sId = space.getState(getCurrentState());
            def result = sId.eval(e)
            return result;
        } catch (EvaluationException e) {
            throw new IllegalFormulaException(
                    "BMotion Studio: Formula " + formula + " could not be parsed: " + e.getMessage());
        } catch (Exception e) {
            throw new IllegalFormulaException("BMotion Studio: " + e.getClass() + " thrown: " + e.getMessage());
        }
    }

    public List<String> getErrors(final String state, final String formula) {
        List<String> errors = new ArrayList<String>();
        if (trace != null) {
            try {
                IEvalElement e = trace.getModel().parseFormula(formula);
                StateSpace space = trace.getStateSpace();
                State sId = space.getState(state);
                if (!sId.isExplored()) {
                    sId.explore();
                }
                if (!sId.isInitialised()) {
                    errors.add("State not initialized");
                }
                sId.eval(e);
            } catch (EvaluationException e) {
                errors.add("Could not parse: " + e.getMessage());
            } catch (Exception e) {
                errors.add(e.getClass() + " thrown: " + e.getMessage());
            }
        }
        return errors;
    }

    @Override
    public void animatorStatus(final boolean busy) {}

}
