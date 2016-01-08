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
import de.prob.translator.types.BObject
import de.prob.translator.types.Boolean
import de.prob.translator.types.Set
import de.prob.translator.types.Tuple
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

    private Object convert(BObject obj) {
        if(obj instanceof  Boolean) {
            return obj.booleanValue();
        } else if(obj instanceof Set) {
            return obj.collect {
                return convert(it);
            }
        } else if(obj instanceof Tuple) {
            return [convert(obj.first), convert(obj.second)]
        }
        return obj;
    }

    public Object translate(String result) throws BMotionException {
        try {
            return convert(Translator.translate(result));
        } catch (BException e) {
            throw new BMotionException("Error while translating " + result + " " + e.getMessage());
        }
    }

    @Override
    public Object evaluateFormulas(final d) throws BMotionException {
        def formulas = [:]
        def stateId = d.data.stateId ?: getCurrentState()
        d.data.formulas.each {
            def t = it['translate']
            def f = it['formula']
            //def s = v.solutions ?: false
            try {
                def result = eval(it['formula'], stateId);
                if (result != null) {
                    def resString = result['value']
                    def arr = [result: resString];
                    if (t) {
                        arr.put('trans', translate(resString))
                    }
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
