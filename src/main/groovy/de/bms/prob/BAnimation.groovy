package de.bms.prob

import de.bms.BMotion
import de.bms.itool.IllegalFormulaException
import de.bms.itool.ImpossibleStepException
import de.bms.itool.ToolRegistry
import de.prob.animator.domainobjects.EvaluationException
import de.prob.animator.domainobjects.IEvalElement
import de.prob.animator.domainobjects.IEvalResult
import de.prob.model.representation.AbstractModel
import de.prob.statespace.State
import de.prob.statespace.StateSpace
import de.prob.statespace.Trace

public class BAnimation extends ProBAnimation {

    private final Map<String, IEvalElement> formulas = new HashMap<String, IEvalElement>();

    public BAnimation(final String toolId, final ToolRegistry toolRegistry) {
        super(toolId, toolRegistry);
    }

    @Override
    public String doStep(final String stateref, final String event, final String... parameters)
            throws ImpossibleStepException {
        if (trace == null) {
            return null;
        }
        try {
            Trace new_trace = trace.execute(event, parameters);
            animations.traceChange(new_trace);
            trace = new_trace;
            toolRegistry.notifyToolChange(BMotion.TRIGGER_ANIMATION_CHANGED, this);
        } catch (Exception e) {
            throw new ImpossibleStepException();
        }
        return trace.getCurrentState().getId();
    }

    @Override
    public AbstractModel getModel() {
        return Object.getModel();
    }

    @Override
    public Object evaluate(final String stateref, final String formula) throws IllegalFormulaException {
        if (trace == null) {
            throw new IllegalFormulaException("BMotion Studio: No trace exists.");
        }
        try {
            StateSpace space = trace.getStateSpace();
            IEvalElement e = formulas.get(formula);
            if (e == null) {
                e = trace.getModel().parseFormula(formula);
                formulas.put(formula, e);
                space.subscribe(this, e);
            }
            State sId = space.getState(stateref);
            IEvalResult result = sId.getValues().get(formulas.get(formula));
            return result;
        } catch (EvaluationException e) {
            throw new IllegalFormulaException(
                    "BMotion Studio: Formula " + formula + " could not be parsed: " + e.getMessage());
        } catch (Exception e) {
            throw new IllegalFormulaException("BMotion Studio: " + e.getClass() + " thrown: " + e.getMessage());
        }
    }

    @Override
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
