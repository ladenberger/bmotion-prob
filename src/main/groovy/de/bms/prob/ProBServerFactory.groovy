package de.bms.prob

import com.corundumstudio.socketio.AckRequest
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.listener.DataListener
import com.google.common.io.Resources
import de.bms.server.BMotionServer
import de.bms.server.JsonObject
import de.prob.animator.command.GetTransitionDiagramCommand
import de.prob.animator.domainobjects.*
import de.prob.model.eventb.EventBMachine
import de.prob.statespace.StateSpace
import de.prob.statespace.Trace
import de.prob.statespace.Transition

public class ProBServerFactory {

    public static BMotionServer getServer(args) {
        BMotionServer server = new BMotionServer(args, new ProBVisualisationProvider())
        URL[] paths = [Resources.getResource("prob")]
        server.setResourcePaths(paths)
        server.socketServer.getServer().
                addEventListener("initTooltip", JsonObject.class, new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {
                        def ProBVisualisation bms = server.socketServer.getSession(client)
                        if (bms != null) {
                            Trace t = bms.getTrace()
                            def eventMap = d.data.events.collect {
                                def p = it.predicate == null ? [] : it.predicate
                                [name: it.name, predicate: p, canExecute: t.canExecuteEvent(it.name, p)]
                            }
                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData([events: eventMap]);
                            }
                        }
                    }
                });
        server.socketServer.getServer().
                addEventListener("observeRefinement", JsonObject.class, new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {
                        def ProBVisualisation bms = server.socketServer.getSession(client)
                        if (bms != null) {
                            Trace t = bms.getTrace()
                            def EventBMachine eventBMachine = t.getModel().getMainComponent()
                            def _getrefs
                            _getrefs = { refines ->
                                return refines.collect() {
                                    def refs = it.refines
                                    if (refs) {
                                        [it.toString(), _getrefs(refs)]
                                    } else {
                                        it.toString()
                                    }
                                }.flatten()
                            }
                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData([refinements: _getrefs(eventBMachine.refines).
                                        reverse() << eventBMachine.toString()]);
                            }
                        }
                    }
                });
        server.socketServer.getServer().
                addEventListener("createCustomTransitionDiagram", JsonObject.class, new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {

                        def ProBVisualisation bms = server.socketServer.getSession(client)
                        if (bms != null) {

                            def _getResults
                            _getResults = { t ->
                                def list = []
                                if (t.first instanceof Tuple) {
                                    list += _getResults(t.first)
                                } else {
                                    list << t.first
                                }
                                list << t.second
                                return list
                            }

                            String joinExp = d.data.expressions.join("|->")
                            IEvalElement eval = bms.getTrace().getModel().parseFormula(joinExp)

                            GetTransitionDiagramCommand cmd = new GetTransitionDiagramCommand(eval)
                            def StateSpace statespace = bms.getStateSpace()
                            statespace.execute(cmd)

                            def nodes = cmd.getNodes().collect {
                                def nn = it.value.properties
                                nn["translated"] = it.value.labels.collect { str ->
                                    if (str != "<< undefined >>" && !d.data.expressions.contains(str)) {
                                        def formula = new TranslateFormula(str as EventB)
                                        def res = bms.getStateSpace().getRoot().eval(formula)
                                        if (res instanceof TranslatedEvalResult) {
                                            if (res.value instanceof Tuple) {
                                                return _getResults(res.value)
                                            } else {
                                                return [res.value]
                                            }
                                        }
                                    } else {
                                        return []
                                    }
                                }
                                [data: nn]
                            }

                            def edges = cmd.getEdges().collect {
                                def en = it.value.properties
                                en['id'] = it.value.source + it.value.target
                                [data: en]
                            }

                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData([nodes: nodes.reverse(), edges: edges.reverse()]);
                            }

                        }
                    }
                });
        server.socketServer.getServer().
                addEventListener("observeCSPTrace", JsonObject.class, new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {
                        def ProBVisualisation bms = server.socketServer.getSession(client)
                        if (bms != null) {

                            def values = [:]
                            def order = []

                            def Trace newTrace = bms.getTrace()
                            def Transition currentTransition = newTrace.getCurrent().getTransition()
                            //def Transition headTransition = newTrace.getHead().getTransition()
                            //TODO: Backtrack provides a wrong current element! This is a workaround!
                            //if (!currentTransition.equals(headTransition)) {
                            //    currentTransition = newTrace.getTransitionList().get(newTrace.getCurrent().getIndex())
                            //}

                            def boolean stop = false
                            def list = newTrace.getTransitionList(true)
                            for (Transition op : list) {

                                if (currentTransition.equals(op)) {
                                    stop = true
                                }

                                def fullOp = getOpString(op)
                                d.data.observers.each { o ->

                                    def events = bms.eval(o.exp)

                                    if (events instanceof EvalResult) {
                                        def eventNames = events.value.replace("{", "").replace("}", "").split(",")
                                        if (eventNames.contains(fullOp)) {
                                            o.actions.each { a ->
                                                def String selector = replaceParameter(a.selector, op)
                                                def String attr = replaceParameter(a.attr, op)
                                                def String value = replaceParameter(a.value, op)
                                                order << selector
                                                def attrs = values.get(selector)
                                                if (attrs == null) {
                                                    attrs = [:]
                                                    values.put(selector, attrs)
                                                }
                                                attrs.put(attr, value)
                                            }
                                        }
                                    }

                                }

                                if (stop == true)
                                    break;

                            }

                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData([values: values, order: order]);
                            }

                        }
                    }
                });

        return server
    }

    def static String replaceParameter(String str, Transition op) {
        op.getParams().eachWithIndex { item, index -> str = str.replace("{{a" + (index + 1) + "}}", item)
        }
        return str;
    }

    def static String getOpString(Transition op) {
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

}
