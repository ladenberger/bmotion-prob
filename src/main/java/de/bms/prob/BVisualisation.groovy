package de.bms.prob

import de.be4.classicalb.core.parser.exceptions.BException
import de.bms.BMotionException
import de.bms.BMotionScriptEngineProvider
import de.prob.animator.domainobjects.EvaluationException
import de.prob.animator.domainobjects.IEvalElement
import de.prob.animator.domainobjects.IdentifierNotInitialised
import de.prob.exception.ProBError
import de.prob.model.representation.AbstractElement
import de.prob.model.representation.Machine
import de.prob.statespace.State
import de.prob.statespace.StateSpace
import de.prob.statespace.Trace
import de.prob.translator.Translator
import de.prob.translator.types.Atom
import de.prob.translator.types.BObject
import groovy.util.logging.Slf4j

@Slf4j
public abstract class BVisualisation extends ProBVisualisation {

    private final Map<String, IEvalElement> formulas = new HashMap<String, IEvalElement>()

    public BVisualisation(final UUID id, final BMotionScriptEngineProvider scriptEngineProvider) {
        super(id, scriptEngineProvider)
    }

    @Override
    protected void updateModelData(Trace t) {

        clientData["model"]["events"] = []
        clientData["model"]["variables"] = []

        AbstractElement mainComponent = t.getStateSpace().getMainComponent()
        if (mainComponent instanceof Machine) {
            // Collect sets
            clientData["model"]["sets"] = mainComponent.getChildrenOfType(de.prob.model.representation.Set.class).collect {
                return [name: it.getName(), comment: it.getComment()]
            }
        }

        updateBModelData(t)

    }

    protected abstract void updateBModelData(Trace t)

    @Override
    public Object eval(String formula) throws BMotionException {
        return eval(formula, getCurrentState())
    }

    public Object eval(String formula, String stateId) throws BMotionException {
        if (trace == null) {
            throw new BMotionException("No trace exists.")
        }
        if (formula == null) {
            throw new BMotionException("Formula must not be null.")
        }
        try {
            def StateSpace space = trace.getStateSpace()
            def IEvalElement e = formulas.get(formula)
            if (!space.isSubscribed(e)) {
                e = trace.getModel().parseFormula(formula)
                formulas.put(formula, e)
                space.subscribe(this, e)
            }
            def State sId = space.getState(stateId)
            def res = sId.getValues().get(formulas.get(formula))
            if (res instanceof IdentifierNotInitialised) {
                throw new BMotionException(res.toString())
            }
            return sId.getValues().get(formulas.get(formula))
        } catch (ProBError e) {
            throw new BMotionException(e.getMessage())
        } catch (EvaluationException e) {
            throw new BMotionException("Formula " + formula + " could not be parsed: " + e.getMessage())
        } catch (Exception e) {
            throw new BMotionException(e.getClass().toString() + " thrown: " + e.getMessage())
        }
    }

    private Object convert(BObject obj) {
        if (obj instanceof de.prob.translator.types.Set) {
            return obj.collect { return convert(it) }
        } else if (obj instanceof de.prob.translator.types.Sequence) {
            return obj.collect { return convert(it) }
        } else if (obj instanceof de.prob.translator.types.Tuple) {
            return [convert(obj.first), convert(obj.second)]
        } else if (obj instanceof de.prob.translator.types.Boolean) {
            return obj.booleanValue()
        } else if (obj instanceof de.prob.translator.types.String) {
            return obj.getValue()
        } else if (obj instanceof Atom) {
            return obj.getValue()
        }
        return obj
    }

    @Override
    public Object translate(String result) throws BMotionException {
        try {
            return convert(Translator.translate(result))
        } catch (BException e) {
            throw new BMotionException("An error occurred while translating " + result + " " + e.getMessage())
        }
    }

    @Override
    public Object evaluateFormulas(final d) throws BMotionException {
        def formulas = [:]
        def String stateId = d.data.stateId ?: getCurrentState()
        d.data.formulas.each {
            def boolean trans = it['translate']
            def String formula = it['formula']
            //def s = v.solutions ?: false
            try {
                def result = eval(formula, stateId);
                if (result != null) {
                    def String resString = result['value']
                    def arr = [result: resString]
                    if (trans) arr['trans'] = translate(resString)
                    formulas.put(formula, arr);
                }
            } catch (BMotionException e) {
                formulas.put(formula, [error: e.getMessage()])
            }
        }
        return formulas
    }

    @Override
    public void animatorStatus(final boolean busy) {}

    @Override
    protected Trace getNewTrace(Trace trace, String transitionName, String transitionPredicate) throws IllegalArgumentException {
        return trace.execute(transitionName, transitionPredicate ? [transitionPredicate] : [])
    }

}
