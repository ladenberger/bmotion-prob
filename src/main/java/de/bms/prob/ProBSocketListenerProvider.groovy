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
import de.prob.cli.CliVersionNumber
import de.prob.model.eventb.EventBModel
import de.prob.scripting.Api
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
    public final long sessionWaitTime = 10000;
    private Thread exitThread;
    private final Map<String, Thread> sessionThreads = new HashMap<String, Thread>();
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

        server.getSocket().addEventListener("getWorkspacePath", String.class, new DataListener<String>() {
            @Override
            public void onData(final SocketIOClient client, String str,
                               final AckRequest ackRequest) {
                if (ackRequest.isAckRequested()) {
                    ackRequest.sendAckData([workspace: server.getServer().getWorkspacePath()]);
                }
            }
        });

        server.getSocket().addEventListener("checkProBCli", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject obj,
                               final AckRequest ackRequest) {
                CliVersionNumber version = api.getVersion();
                log.info("ProB cli version " + version)
                if (ackRequest.isAckRequested()) {
                    ackRequest.sendAckData([version: version]);
                }
            }
        });

        server.getSocket().addEventListener("downloadProBCli", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject obj,
                               final AckRequest ackRequest) {
                def version = api.upgrade("latest")
                log.info("Upgraded to ProB clic version " + version)
                api.downloader.installCSPM()
                if (ackRequest.isAckRequested()) {
                    ackRequest.sendAckData([version: version]);
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

                    //sessions.remove(id)
                    clients.remove(client)
                    bms.clients.remove(client)
                    //bms.disconnect();

                    if (bms.clients.isEmpty()) {
                        startSessionTimer(bms)
                    }

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

        server.getSocket().addEventListener("initView", JsonObject.class,
                new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {
                        def String id = d.data.id
                        def ProBVisualisation bms = getSession(id)
                        if (bms != null) {
                            def sessionId = bms.getId().toString()
                            bms.clients.add(client)
                            def sessionThread = sessionThreads.get(sessionId)
                            if (sessionThread != null) {
                                sessionThread.interrupt()
                                sessionThreads.remove(sessionId)
                            }
                            clients.put(client, id)
                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData(bms.clientData);
                            }
                        }
                    }
                });

        server.getSocket().addEventListener("initSession", JsonObject.class,
                new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {

                        // Data from client
                        def String tool = d.data.tool
                        def String id = UUID.randomUUID() // The id should come from the client!
                        def String manifest = d.data.manifest
                        def String templateFolder = new File(manifest).getParent().toString()
                        def String modelPath = templateFolder + File.separator + d.data.model

                        try {

                            def ProBVisualisation bms = createSession(id, tool, server.getServer().getVisualisationProvider());
                            if (bms != null) {
                                bms.clientData = d.data
                                bms.clientData.templateFolder = templateFolder
                                bms.setMode(server.getServer().getMode())
                                bms.initSession(modelPath)
                                sessions.put(id, bms)
                                de.bms.BMotion.log.info "Created new BMotion session " + id
                                Trace t = bms.getTrace()

                                bms.clientData.put('id', bms.getId().toString())
                                bms.clientData.put('stateId', t.getCurrentState().getId())
                                bms.clientData.put('initialised', t.getCurrentState().isInitialised())

                                if (bms.getModel() instanceof EventBModel) {
                                    def EventBModel eventBModel = t.getModel()
                                    if (ackRequest.isAckRequested()) {
                                        bms.clientData.put('refinements', eventBModel.getMachines().collect {
                                            return it.name;
                                        })
                                        ackRequest.sendAckData(id);
                                    }
                                } else {
                                    if (ackRequest.isAckRequested()) {
                                        ackRequest.sendAckData(id);
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
                        ackRequest.sendAckData([events: ops]);
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
                            def formulaMap = []

                            d.data.formulas.each {
                                formulaMap += [formula: it, translate: false]
                            }

                            bms.getCurrentTrace().getTransitionList().each { Transition op ->

                                def sId = op.getSource().getId()
                                def dId = op.getDestination().getId()

                                def res1 = [:]
                                def res2 = [:]
                                if (sId != 'root' && sId != '0') {
                                    res1 = bms.evaluateFormulas([data: [stateId: sId, formulas: formulaMap]])
                                }
                                if (dId != 'root' && dId != '0') {
                                    res2 = bms.evaluateFormulas([data: [stateId: dId, formulas: formulaMap]])
                                }

                                nodes.push([group: 'nodes', data: [id: sId, label: sId, results: res1]]);
                                nodes.push([group: 'nodes', data: [id: dId, label: dId, results: res2]]);

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
                addEventListener("createProjectionDiagram", JsonObject.class, new DataListener<JsonObject>() {
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

                            def expressions = d.data.formulas.collect {

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

                                    nn["results"] = [:]

                                    expressions.eachWithIndex { exp, index ->
                                        nn["results"][(exp)] = [result: reTranslate(nn["translated"][index])]
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

    private void startSessionTimer(BMotion bms) {

        log.info("Going to start session timer thread")
        //ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        //singleThreadExecutor.execute();

        def sessionId = bms.getId().toString()
        def sessionThread = sessionThreads.get(sessionId)

        if (sessionThread == null) {

            sessionThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(sessionWaitTime);
                        if (bms.clients.isEmpty()) {
                            log.info("Remove session " + bms.getId())
                            bms.disconnect()
                            sessions.remove(sessionId)
                        }
                    } catch (InterruptedException e) {
                        log.info("Session timer thread interrupted")
                        //e.printStackTrace();
                    } finally {
                        log.info("Exit session timer thread")
                        return;
                    }
                }
            });
            sessionThreads.put(sessionId, sessionThread)
            sessionThread.start()

        }

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
