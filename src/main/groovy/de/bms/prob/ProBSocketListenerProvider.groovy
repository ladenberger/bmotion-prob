package de.bms.prob

import com.corundumstudio.socketio.AckRequest
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.listener.ConnectListener
import com.corundumstudio.socketio.listener.DataListener
import com.corundumstudio.socketio.listener.DisconnectListener
import de.bms.*
import de.prob.animator.command.GetTransitionDiagramCommand
import de.prob.animator.domainobjects.EvalElementType
import de.prob.animator.domainobjects.IEvalElement
import de.prob.model.eventb.EventBMachine
import de.prob.model.eventb.EventBModel
import de.prob.statespace.FormalismType
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
    public final Map<SocketIOClient, String> clients = new HashMap<SocketIOClient, String>();
    public final long waitTime = 50000;
    private Thread exitThread;

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

        server.getSocket().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                log.info("Client connected")
                if (exitThread) exitThread.interrupt();
            }
        });

        server.getSocket().addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {

                def String id = clients.get(client)
                def BMotion bms = getSession(id)
                if (bms != null) {
                    sessions.remove(id)
                    clients.remove(client)
                    bms.disconnect();
                }

                // In standalone mode exit server when no client exists
                if (server.getServer().getMode() == BMotionServer.MODE_STANDALONE) {
                    log.info("Check if no clients exist " + server.getSocket().getAllClients().isEmpty().toString())
                    if (server.getSocket().getAllClients().isEmpty()) {
                        startTimer(server);
                    }
                }

            }
        });

        server.getSocket().addEventListener("executeEvent", JsonObject.class,
                new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {
                        def String id = d.data.id
                        def ProBVisualisation bms = getSession(id)
                        if (bms != null) {
                            def returnValue = bms.executeEvent(d.data)
                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData(returnValue);
                            }
                        }
                    }
                });

        /*server.getSocket().addEventListener("reloadModel", JsonObject.class,
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
                });*/

        server.getSocket().addEventListener("initSession", JsonObject.class,
                new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {

                        // Data from client
                        def String path = d.data.path
                        def String model = d.data.model
                        def String tool = d.data.tool
                        def String id = d.data.id

                        File templateFolder = new File(path)
                        String modelFilePath = templateFolder.getPath() + File.separator + model

                        de.bms.BMotion.log.info "BMotion Studio:  Templatefolder = " + templateFolder
                        de.bms.BMotion.log.info "BMotion Studio: Modelfile = " + modelFilePath

                        try {

                            def ProBVisualisation bms = createSession(id, tool, server.getServer().getVisualisationProvider());
                            if (bms != null) {
                                bms.setMode(server.getServer().getMode())
                                bms.initSession(modelFilePath)
                                bms.setClient(client)
                                clients.put(client, id)
                                sessions.put(id, bms)
                                de.bms.BMotion.log.info "Created new BMotion session " + id
                                Trace t = bms.getTrace()

                                def clientData = [id: bms.getId().toString(), stateId: t.getCurrentState().getId(), traceId: t.getUUID().toString(), initialised: t.getCurrentState().isInitialised()]

                                if (bms.getModel() instanceof EventBModel) {
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
                                        clientData.put('refinements', _getrefs(eventBMachine.refines).reverse() << eventBMachine.toString())
                                        ackRequest.sendAckData(clientData);
                                    }
                                } else {
                                    if (ackRequest.isAckRequested()) {
                                        ackRequest.sendAckData(clientData);
                                    }
                                }
                            }

                        } catch (BMotionException e) {
                            ackRequest.sendAckData([errors: [e.getMessage()]]);
                            return;
                        }

                    }
                });

        server.getSocket().addEventListener("evaluateFormulas", JsonObject.class,
                new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {
                        def String id = d.data.id
                        def BMotion bms = getSession(id)
                        if (bms != null) {
                            try {
                                ackRequest.sendAckData(bms.evaluateFormulas(d));
                            } catch (BMotionException e) {
                                ackRequest.sendAckData([errors: [e.getMessage()]]);
                            }
                        }
                    }
                });

        server.getSocket().addEventListener("getHistory", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def String id = d.data.id
                def ProBVisualisation bms = getSession(id)
                if (bms != null) {
                    def ops = [];
                    // Collect trace data
                    def Trace newTrace = bms.getTrace()
                    def Transition currentTransition = newTrace.getCurrent().getTransition()
                    def list = newTrace.getTransitionList(true)
                    for (Transition op : list) {
                        def fullOp = getOpString(op)
                        ops << [name: fullOp, parameter: op.getParams()]
                        if (currentTransition.equals(op) || (d.data.stateId != null && op.getDestination().
                                getId() == d.data.stateId)) {
                            break;
                        }
                    }
                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData([ops: ops]);
                    }
                }
            }
        });

        server.getSocket().addEventListener("eval", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def String id = d.data.id
                def ProBVisualisation bms = getSession(id)
                if (bms != null) {
                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData([bms.eval(d.data.formula, d.data.stateId)]);
                    }
                }
            }
        });

        server.getSocket().addEventListener("initTooltip", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def String id = d.data.id
                def ProBVisualisation bms = getSession(id)
                if (bms != null) {
                    Trace t = bms.getTrace()
                    def eventMap = d.data.events.collect { op ->
                        def Transition trans
                        if (t.getModel().getFormalismType().equals(FormalismType.CSP)) {
                            for (cop in t.getNextTransitions()) {
                                if (cop.getRep().equals(op.name)) {
                                    trans = cop;
                                    break;
                                }
                            }
                        } else {
                            trans = t.getCurrentState().findTransition(op.name, op.predicate == null ? [] : op.predicate);
                        }
                        def canExecute = trans != null;
                        def transId = trans != null ? trans.getId() : null;
                        [name: op.name, predicate: op.predicate, id: transId, canExecute: canExecute]
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
                        def String id = d.data.id
                        def ProBVisualisation bms = getSession(id)
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
                });

        server.getSocket().
                addEventListener("createCustomTransitionDiagram", JsonObject.class, new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {
                        def String id = d.data.id
                        def ProBVisualisation bms = getSession(id)
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

    }

    private void startTimer(BMotionSocketServer server) {

        log.info("Going to start timer thread")
        //ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        //singleThreadExecutor.execute();

        exitThread = new Thread(new Runnable() {
            @Override
            public void run() {
                log.info("Timer thread started")
                try {
                    Thread.sleep(waitTime);
                    log.info("Check if still no clients exist")
                    if (server.getSocket().getAllClients().isEmpty()) {
                        log.info("Close BMotion Studio for ProB server process")
                        System.exit(-1)
                    }
                } catch (InterruptedException e) {
                    log.info("Timer thread interrupted")
                    //e.printStackTrace();
                } finally {
                    log.info("Exit timer thread")
                    return;
                }
            }
        });
        exitThread.start();
        //log.info("Is alive? " + exitThread.isAlive().toString())

    }

    private BMotion getSession(String id) {
        sessions.get(id);
    }

    private BMotion createSession(String id, String tool, BMotionVisualisationProvider visualisationProvider) throws BMotionException {
        if (tool != null) {
            def visualisation = visualisationProvider.get(id, tool)
            if (visualisation == null) {
                throw new BMotionException("No visualisation implementation found for " + tool)
            } else {
                return visualisation;
            }
        } else {
            throw new BMotionException("Please specify a tool in bmotion.json (e.g. BAnimation or CSPAnimation)")
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
