package de.bms.prob

import com.corundumstudio.socketio.AckRequest
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.listener.DataListener
import com.corundumstudio.socketio.listener.DisconnectListener
import de.bms.*
import de.prob.animator.command.GetTransitionDiagramCommand
import de.prob.animator.domainobjects.EvalElementType
import de.prob.animator.domainobjects.IEvalElement
import de.prob.model.eventb.EventBMachine
import de.prob.model.eventb.EventBModel
import de.prob.statespace.StateSpace
import de.prob.statespace.Trace
import de.prob.statespace.Transition
import de.prob.translator.Translator
import de.prob.translator.types.Tuple
import de.prob.webconsole.WebConsole
import groovy.util.logging.Slf4j

@Slf4j
class ProBSocketListenerProvider implements BMotionSocketListenerProvider {

    public final Map<String, BMotion> sessions = new HashMap<String, BMotion>();

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

        server.getSocket().addEventListener("getWorkspacePath", String.class, new DataListener<String>() {
            @Override
            public void onData(final SocketIOClient client, String str,
                               final AckRequest ackRequest) {
                if (ackRequest.isAckRequested()) {
                    ackRequest.sendAckData([workspace: server.getServer().getWorkspacePath()]);
                }
            }
        });
        server.getSocket().addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {
            }
        });
        server.getSocket().addEventListener("clientClosed", JsonObject.class,
                new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {
                        server.getServer().serverStartedListener?.serverCloseRequest();
                    }
                });
        server.getSocket().addEventListener("executeEvent", JsonObject.class,
                new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {
                        def String traceId = d.data.traceId;
                        def BMotion bmotion = getSession(traceId)
                        if (bmotion != null) {
                            def returnValue = bmotion.executeEvent(d.data)
                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData(returnValue);
                            }
                        }
                    }
                });
        server.getSocket().addEventListener("reloadModel", JsonObject.class,
                new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {
                        def String traceId = d.data.traceId;
                        def ProBVisualisation bmotion = getSession(traceId)
                        if (bmotion != null) {
                            def newTraceId = bmotion.reloadModel()
                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData(newTraceId);
                            }
                        }
                    }
                });

        server.getSocket().addEventListener("loadModel", JsonObject.class,
                new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {

                        def String path = d.data.path
                        def String model = d.data.model
                        def String tool = d.data.tool

                        File templateFolder = new File(path)
                        File modelFile = new File(templateFolder.getPath() + File.separator + model)
                        ProBSocketListenerProvider.log.info "Templatefolder: " + templateFolder
                        ProBSocketListenerProvider.log.info "Modelfile: " + modelFile

                        def ProBVisualisation bmotion = createSession(tool, server.getServer().getVisualisationProvider());
                        bmotion.setMode(server.getServer().getMode())
                        bmotion.initSession(modelFile.getPath())
                        bmotion.setClient(client)
                        sessions.put(bmotion.getTrace().getUUID().toString(), bmotion)
                        BMotionSocketServer.log.info "Created new BMotion session " + bmotion.sessionId

                        Trace t = bmotion.getTrace()
                        if (bmotion.getModel() instanceof EventBModel) {
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
                                ackRequest.sendAckData([refinements                   : _getrefs(eventBMachine.refines).
                                        reverse() << eventBMachine.toString(), stateid: t.getCurrentState().getId(), traceId: t.getUUID().toString()]);
                            }
                        } else {
                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData([stateid: t.getCurrentState().getId(), traceId: t.getUUID().toString()]);
                            }
                        }


                    }
                });
        server.getSocket().addEventListener("observe", JsonObject.class,
                new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {
                        def String traceId = d.data.traceId;
                        def BMotion bmotion = getSession(traceId)
                        if (bmotion != null) {
                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData(bmotion.observe(d));
                            }
                        }
                    }
                });

        server.getSocket().addEventListener("eval", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def String traceId = d.data.traceId;
                def BMotion bmotion = getSession(traceId)
                if (bmotion != null) {
                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData([bmotion.eval(d.data.formula, d.data.stateid)]);
                    }
                }
            }
        });
        server.getSocket().addEventListener("initTooltip", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def String traceId = d.data.traceId;
                def ProBVisualisation bms = getSession(traceId)
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
        server.getSocket().
                addEventListener("createTraceDiagram", JsonObject.class, new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {
                        def String traceId = d.data.traceId;
                        def ProBVisualisation bms = getSession(traceId)
                        if (bms != null) {

                            def nodes = []
                            def edges = []

                            bms.getCurrentTrace().getTransitionList().each { Transition op ->

                                def sId = op.getSource().getId()
                                def dId = op.getDestination().getId()
                                nodes.push([group: 'nodes', data: [id: sId, label: sId]]);
                                nodes.push([group: 'nodes', data: [id: dId, label: dId]]);
                                edges.push(
                                        [group: 'edges', data: [id: 'e' + sId + '' + dId, source: sId, target: dId, label: op.
                                                getName()]]);

                            }

                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData([nodes: nodes, edges: edges]);
                            }

                        }
                    }
                }

                );
        server.getSocket().
                addEventListener("createCustomTransitionDiagram", JsonObject.class, new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {

                        def String traceId = d.data.traceId;
                        def ProBVisualisation bms = getSession(traceId)
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

                            def expressions = d.data.expressions.collect {

                                IEvalElement e = bms.getTrace().getModel().parseFormula(it);
                                if (e.getKind() == EvalElementType.PREDICATE.toString()) {
                                    return 'bool(' + e.getCode() + ')';
                                } else {
                                    return e.getCode();
                                }

                            }

                            def nodes = [];
                            def edges = [];

                            if (!expressions.isEmpty()) {

                                String joinExp = expressions.join("|->")

                                IEvalElement eval = bms.getTrace().getModel().parseFormula(joinExp)

                                GetTransitionDiagramCommand cmd = new GetTransitionDiagramCommand(eval)
                                def StateSpace statespace = bms.getStateSpace()
                                statespace.execute(cmd)

                                nodes = cmd.getNodes().collect {
                                    def nn = it.value.properties
                                    nn["translated"] = []

                                    if (!it.value.labels.contains("<< undefined >>") && it.value.id != "1") {
                                        it.value.labels.each { str ->

                                            def res = Translator.translate(str);
                                            if (res instanceof Tuple) {
                                                nn["translated"] += _getResults(res)
                                            } else {
                                                nn["translated"] << res
                                            }

                                        }
                                    }

                                    nn["results"] = nn["translated"].collect {
                                        reTranslate(it)
                                    }
                                    [data: nn]

                                }

                                edges = cmd.getEdges().collect {
                                    def en = it.value.properties
                                    en['id'] = it.value.source + it.value.target
                                    [data: en]
                                }

                            }

                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData([nodes: nodes.reverse(), edges: edges.reverse()]);
                            }

                        }
                    }

                });
        server.getSocket().addEventListener("observeCSPTrace", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def String traceId = d.data.traceId;
                def ProBVisualisation bms = getSession(traceId)
                if (bms != null) {

                    def ops = [];
                    def exp = [:];

                    d.data.observers.each { o ->
                        def events = bms.eval(o.exp)
                        exp.put(o.exp, events.value);
                    }

                    // Collect trace data
                    def Trace newTrace = bms.getTrace()
                    def Transition currentTransition = newTrace.getCurrent().getTransition()
                    def list = newTrace.getTransitionList(true)
                    for (Transition op : list) {
                        def fullOp = getOpString(op)
                        ops << [name: fullOp, parameter: op.getParams()]
                        if (currentTransition.equals(op) || (d.data.stateid != null && op.getDestination().
                                getId() == d.data.stateid)) {
                            break;
                        }
                    }

                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData([ops: ops, exp: exp]);
                    }

                }
            }
        });

    }

    private BMotion getSession(String traceId) {
        sessions.get(traceId);
    }

    private BMotion createSession(String tool, BMotionVisualisationProvider visualisationProvider) {
        if (tool != null) {
            def visualisation = visualisationProvider.get(tool)
            if (visualisation == null) {
                log.error "BMotion Studio: No visualisation implementation found for " + tool
            } else {
                return visualisation;
            }
        } else {
            log.error "BMotion Studio: Please enter a tool (e.g. BAnimation or CSPAnimation)"
        }
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
