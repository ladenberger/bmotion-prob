package de.bms.prob.observer

import com.google.gson.Gson
import de.bms.BMotion
import de.bms.observer.BMotionObserver
import de.bms.observer.TransformerObject
import de.bms.observer.TransformerObserver
import de.prob.animator.domainobjects.EvalResult
import de.prob.statespace.Trace
import de.prob.statespace.Transition
import groovy.transform.TupleConstructor

@TupleConstructor
class CSPTraceObserver extends BMotionObserver {

    def String _component

    private objs = []

    private int lastIndex

    private final Gson g = new Gson()

    def static CSPTraceObserver make(Closure cls) {
        new CSPTraceObserver().with cls
    }

    def CSPTraceObserver component(component) {
        this._component = component
        this
    }

    def CSPTraceObserver observe(String exp, Closure cls) {
        def evt = new EventsObserver(exp).with cls
        objs.add(evt)
        this
    }

    def static getOpString(Transition op) {
        def String opName = op.getName()
        def String AsImplodedString = ""
        def List<String> opParameter = op.getParams()
        if (opParameter.size() > 0) {
            String[] inputArray = opParameter.toArray(new String[opParameter
                    .size()]);
            StringBuffer sb = new StringBuffer();
            sb.append(inputArray[0]);
            for (int i = 1; i < inputArray.length; i++) {
                sb.append(".");
                sb.append(inputArray[i]);
            }
            AsImplodedString = "." + sb.toString();
        }
        String opNameWithParameter = opName + AsImplodedString;
        return opNameWithParameter;
    }

    @Override
    def apply(BMotion bms) {

        def List<TransformerObject> tobjects = []

        def Trace newTrace = bms.getTool().getTrace()

        def int newIndex = newTrace.current.getIndex()
        lastIndex = lastIndex ?: newTrace.head.getIndex()

        // Backtrack
        if (lastIndex > newIndex) {
            bms.clients.each { it.sendEvent("revertCSP") }
        }

        def Transition currentTransition = newTrace.getCurrent().getTransition()
        def Transition headTransition = newTrace.getHead().getTransition()

        //TODO: Backtrack provides a wrong current element! This is a workaround!
        if(!currentTransition.equals(headTransition)) {
            currentTransition = newTrace.getTransitionList().get(newTrace.getCurrent().getIndex())
        }

        def boolean stop = false
        def list = newTrace.getTransitionList(true)
        //System.out.println(list)
        for (Transition op : list) {

            if (currentTransition.equals(op)) {
                stop = true
            }

            def fullOp = getOpString(op)
            objs.each { EventsObserver evt ->

                def events = bms.eval(evt.exp)

                if (events instanceof EvalResult) {
                    def eventNames = events.value.replace("{", "").replace("}", "").split(",")
                    if (eventNames.contains(fullOp)) {

                        evt.transformers.each { TransformerObserver gt ->

                            def fselector = (gt._selector instanceof Closure) ? gt._selector(op) : gt._selector
                            def t = new TransformerObject(fselector)
                            t.attributes = gt._attributes.collectEntries {
                                kv -> (kv.value instanceof Closure) ? [kv.key, kv.value(op)] : [kv.key, kv.value]
                            }
                            t.styles = gt._styles.collectEntries {
                                kv -> (kv.value instanceof Closure) ? [kv.key, kv.value(op)] : [kv.key, kv.value]
                            }
                            t.content = (gt._content instanceof Closure) ? gt._content(op) : gt._content
                            tobjects << t

                        }

                    }

                }

            }

            if (stop == true)
                break;

        }

        lastIndex = newIndex

        bms.clients.each { it.sendEvent("applyTransformersCSP", g.toJson(tobjects)) }

    }

    def class EventsObserver {

        def String exp
        def List<TransformerObserver> transformers = []

        def EventsObserver(String exp) {
            this.exp = exp
        }

        def EventsObserver add(TransformerObserver transformer) {
            transformers.add(transformer)
            this
        }

    }

}