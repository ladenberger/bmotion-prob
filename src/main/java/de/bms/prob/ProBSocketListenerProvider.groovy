package de.bms.prob

import com.corundumstudio.socketio.AckRequest
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.listener.DataListener
import de.bms.*
import de.prob.animator.command.GetTransitionDiagramCommand
import de.prob.animator.domainobjects.EvalElementType
import de.prob.animator.domainobjects.IEvalElement
import de.prob.scripting.Api
import de.prob.statespace.FormalismType
import de.prob.statespace.StateSpace
import de.prob.statespace.Trace
import de.prob.statespace.Transition
import de.prob.webconsole.WebConsole
import groovy.util.logging.Slf4j

@Slf4j
class ProBSocketListenerProvider implements BMotionSocketListenerProvider {

    private final Api api;

    public ProBSocketListenerProvider() {
        api = de.prob.Main.getInjector().getInstance(Api.class)
    }

    @Override
    void installListeners(BMotionSocketServer server) {

        server.getSocket().addEventListener("initProB", String.class, new DataListener<String>() {
            @Override
            public void onData(final SocketIOClient client, String str,
                               final AckRequest ackRequest) {
                if (ackRequest.isAckRequested()) {
                    ackRequest.sendAckData([port: WebConsole.getPort()]);
                }
            }
        });

        server.getSocket().addEventListener("checkEvents", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def BMotion bms = server.getSessions().get((String) d.data['id'])
                if (bms != null && bms instanceof ProBVisualisation) {
                    def ProBVisualisation prob = (ProBVisualisation) bms
                    Trace t = prob.getTrace()
                    def eventMap = d.data.events.collect { op ->
                        def String opName = op.name
                        def String opPredicate = op.predicate
                        def Transition trans
                        if (t.getModel().getFormalismType().equals(FormalismType.CSP)) {
                            trans = t.getNextTransitions().find { it.getName().equals(opName) }
                        } else {
                            def predicate = opPredicate?.size() > 0 ? [opPredicate] : [];
                            trans = t.getCurrentState().findTransition(opName, predicate);
                        }
                        return [
                                name      : opName,
                                predicate : opPredicate,
                                id        : trans != null ? trans.getId() : null,
                                canExecute: trans != null
                        ]
                    }
                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData([events: eventMap]);
                    }
                }
            }
        });

        server.getSocket().addEventListener("observeNextEvents", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def BMotion bms = server.getSessions().get((String) d.data['id'])
                if (bms != null && bms instanceof ProBVisualisation) {
                    def ProBVisualisation prob = (ProBVisualisation) bms
                    def ops = prob.getTrace().getNextTransitions().collect { Transition op ->
                        return [
                                id          : op.getId(),
                                name        : op.getName(),
                                parameter   : op.getParams(),
                                returnValues: op.getReturnValues(),
                                opString    : getOpString(op, bms)
                        ];
                    }
                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData([ops]);
                    }
                }
            }
        });

        server.getSocket().addEventListener("observeHistory", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def BMotion bms = server.getSessions().get((String) d.data['id'])
                if (bms != null && bms instanceof ProBVisualisation) {
                    def ProBVisualisation prob = (ProBVisualisation) bms
                    def trace = prob.getTrace();
                    def Transition currentTransition = trace.getCurrent().getTransition();
                    def ops = trace.getTransitionList(true).collect { Transition op ->
                        return [
                                id          : op.getId(),
                                name        : op.getName(),
                                parameter   : op.getParams(),
                                returnValues: op.getReturnValues(),
                                current     : currentTransition.equals(op),
                                opString    : getOpString(op, bms)
                        ];
                    }
                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData([ops]);
                    }
                }
            }
        });

        server.getSocket().
                addEventListener("createTraceDiagram", JsonObject.class, new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {

                        def BMotion bms = server.getSessions().get((String) d.data['id'])
                        if (bms != null && bms instanceof ProBVisualisation) {
                            def ProBVisualisation prob = (ProBVisualisation) bms

                            def nodes = [[group: 'nodes', data: [id: 'root', label: 'root', results: [:], op: 'root']]]
                            def edges = []

                            prob.getTrace().getTransitionList().each { Transition op ->

                                def sId = op.getSource().getId()
                                def dId = op.getDestination().getId()

                                // Add destination state as node
                                def res2 = [:]
                                if (dId != 'root' && dId != '0') {
                                    res2 = bms.evaluateFormulas([data: [stateId: dId, formulas: d.data.formulas]])
                                }
                                nodes.push([
                                        group: 'nodes',
                                        data : [
                                                id     : dId,
                                                label  : dId,
                                                results: res2,
                                                op     : op.toString()
                                        ]
                                ]);

                                // Add transition as edge
                                edges.push([
                                        group: 'edges',
                                        data : [
                                                id    : 'e' + sId + '' + dId,
                                                source: sId,
                                                target: dId,
                                                label : op.getName()
                                        ]
                                ]);

                            }

                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData([nodes: nodes, edges: edges]);
                            }

                        }
                    }
                });

        server.getSocket().
                addEventListener("createProjectionDiagram", JsonObject.class, new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {

                        def BMotion bms = server.getSessions().get((String) d.data['id'])
                        if (bms != null && bms instanceof BVisualisation) {

                            def BVisualisation bvis = (BVisualisation) bms

                            def _getResults
                            _getResults = { obj ->
                                def list = []
                                if (obj instanceof ArrayList) {
                                    ArrayList l = (ArrayList) obj
                                    l.forEach({
                                        list += _getResults(it)
                                    })
                                } else {
                                    list += obj
                                }
                                return list
                            }

                            def expressions = d.data.formulas.collect {
                                IEvalElement e = bvis.getTrace().getModel().parseFormula((String) it["formula"]);
                                if (e.getKind() == EvalElementType.PREDICATE.toString()) {
                                    return 'bool(' + e.getCode() + ')';
                                } else {
                                    return e.getCode();
                                }
                            }

                            def nodes = [];
                            def edges = [];
                            def errors = [];

                            if (!expressions.isEmpty()) {

                                def IEvalElement eval = bvis.getTrace().getModel().parseFormula(expressions.join("|->"))
                                def GetTransitionDiagramCommand cmd = new GetTransitionDiagramCommand(eval)
                                def StateSpace stateSpace = bvis.getStateSpace()
                                stateSpace.execute(cmd)

                                // Collect nodes
                                nodes = cmd.getNodes().collect {

                                    def nn = it.value.properties
                                    def translated = []

                                    if (!it.value.labels.contains("<< undefined >>") && it.value.id != "1") {
                                        it.value.labels.each { str ->
                                            try {
                                                translated += _getResults(bvis.translate(str))
                                            } catch (BMotionException e) {
                                                errors.push(e.getMessage())
                                            }
                                        }
                                    }

                                    nn["results"] = [:]
                                    d.data.formulas.eachWithIndex { exp, int index ->
                                        nn["results"][(String) exp["formula"]] = [
                                                result: reTranslate(translated[index]),
                                                trans : translated[index]
                                        ]
                                    }

                                    return [data: nn]

                                }

                                // Collect edges
                                edges = cmd.getEdges().collect {
                                    def en = it.value.properties
                                    en['id'] = it.value.source + it.value.target + '_' + it.value.label
                                    return [data: en]
                                }

                            }

                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData([
                                        nodes : nodes.reverse(),
                                        edges : edges.reverse(),
                                        errors: errors
                                ]);
                            }

                        }
                    }
                });

    }

    def static String getOpString(Transition op, BMotion bms) {

        def String paraString = "";
        def List<String> opParameter = op.getParams();
        opParameter.each() {

            if (bms instanceof CSPVisualisation) {
                paraString += ".";
            } else if (it != opParameter.first()) {
                paraString += ",";
            }
            paraString += it;

        }
        return op.getName() + (bms instanceof BVisualisation ? "(" + paraString + ")" : paraString);

    }

    def static String reTranslate(obj) {
        if (obj == true) {
            return "TRUE";
        } else if (obj == false) {
            return "FALSE";
        } else {
            return obj.toString();
        }
    }

}
