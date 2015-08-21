package de.bms.prob

import de.be4.classicalb.core.parser.exceptions.BException
import de.bms.BMotionException
import de.bms.BMotionScriptEngineProvider
import de.prob.animator.domainobjects.EvalResult
import de.prob.animator.domainobjects.EvaluationException
import de.prob.animator.domainobjects.IEvalElement
import de.prob.animator.domainobjects.IdentifierNotInitialised
import de.prob.exception.ProBError
import de.prob.statespace.State
import de.prob.statespace.StateSpace
import de.prob.statespace.Trace
import de.prob.translator.Translator
import groovy.util.logging.Slf4j

@Slf4j
public class BVisualisation extends ProBVisualisation {

    private final Map<String, IEvalElement> formulas = new HashMap<String, IEvalElement>();

    public BVisualisation(final UUID id, final BMotionScriptEngineProvider scriptEngineProvider) {
        super(id, scriptEngineProvider);
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
            throw new BMotionException("Formula must not be null.");
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
            def res = sId.getValues().get(formulas.get(formula))
            if (res instanceof IdentifierNotInitialised) {
                throw new BMotionException(res.toString());
            }
            return sId.getValues().get(formulas.get(formula))
        } catch (ProBError e) {
            throw new BMotionException(e.getMessage());
        } catch (EvaluationException e) {
            throw new BMotionException("Formula " + formula + " could not be parsed: " + e.getMessage());
        } catch (Exception e) {
            throw new BMotionException(e.getClass().toString() + " thrown: " + e.getMessage());
        }
    }

    public Object translate(String formula) throws BMotionException {
        return translate(formula, getCurrentState());
    }

    public Object translate(String formula, String stateId) throws BMotionException {
        def res = eval(formula, stateId);
        if (res instanceof EvalResult) {
            return translate(res)
        }
    }

    public Object translate(EvalResult er) throws BMotionException {
        try {
            return Translator.translate(er.value);
        } catch (BException e) {
            throw new BMotionException("Error while translating formula " + formula + " " + e.getMessage());
        }
    }

    @Override
    public Object evaluateFormulas(final d) throws BMotionException {
        def formulas = [:]
        def stateId = d.data.stateId ?: getCurrentState()
        d.data.formulas.each {
            System.out.println(it)
            def t = it['translate']
            def f = it['formula']
            //def s = v.solutions ?: false
            try {
                def result = eval(it['formula'], stateId);
                if (result != null) {
                    def resString = result['value']
                    def arr = [result: resString];
                    if (t) arr.put('trans', translate(result))
                    formulas.put(f, arr);
                }
            } catch (BMotionException e) {
                formulas.put(f, [error: e.getMessage()]);
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
