package de.bms.prob

import de.bms.IllegalFormulaException
import de.bms.ImpossibleStepException
import de.bms.server.BMotionScriptEngineProvider
import de.prob.animator.domainobjects.*
import de.prob.statespace.State
import de.prob.statespace.StateSpace
import de.prob.statespace.Trace
import groovy.util.logging.Slf4j

@Slf4j
public class BVisualisation extends ProBVisualisation {

    private final Map<String, IEvalElement> formulas = new HashMap<String, IEvalElement>();

    public BVisualisation(final UUID sessionId, final String templatePath,
                          final BMotionScriptEngineProvider scriptEngineProvider) {
        super(sessionId, templatePath, scriptEngineProvider);
    }

    @Override
    public Object executeEvent(final data) throws ImpossibleStepException {

        if (trace == null) {
            log.error "BMotion Studio: No currentTrace exists."
        }

        def Trace new_trace
        for (def alt : data.events) {
            new_trace = alt.predicate != null ? executeEventHelper(trace, alt.name, alt.predicate) :
                    executeEventHelper(trace, alt.name, [])
            if (new_trace != null)
                break;
        }
        if (new_trace != null) {
            animations.traceChange(new_trace)
            currentTrace = new_trace
        } else {
            log.error "BMotion Studio: Could not execute any event ..."
        }

        return trace.getCurrentState().getId();

    }

    private Trace executeEventHelper(t, name, pred) {
        try {
            t.execute(name, pred)
        } catch (Exception e) {
            null
        }
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
                e = new TranslateFormula(formula as ClassicalB)
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
