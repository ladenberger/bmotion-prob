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
import de.prob.scripting.Api
import de.prob.statespace.FormalismType
import de.prob.statespace.StateSpace
import de.prob.statespace.Trace
import de.prob.statespace.Transition
import de.prob.webconsole.WebConsole
import groovy.util.logging.Slf4j

@Slf4j
class ProBSocketListenerProvider implements BMotionSocketListenerProvider {

    public final Map<String, BMotion> sessions = new HashMap<String, BMotion>();
    public final Map<SocketIOClient, String> clients = new HashMap<SocketIOClient, String>();
    public final long waitTime = 10000;
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
                CliVersionNumber v = api.getVersion();
                def String version = v ?: null;
                def String revision = v ? v.revision : null
                if (ackRequest.isAckRequested()) {
                    ackRequest.sendAckData([version: version, revision: revision]);
                }
            }
        });

        /*server.getSocket().addEventListener("downloadProBCli", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject obj,
                               final AckRequest ackRequest) {
                def String targetVersion = obj.data.version
                def version = api.upgrade(targetVersion)
                log.info("Upgraded to ProB clic version " + version)
                api.downloader.installCSPM()
                if (ackRequest.isAckRequested()) {
                    ackRequest.sendAckData([version: version]);
                }
            }
        });*/

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
                    def isEmptyClient = server.getSocket().getAllClients().isEmpty()
                    log.info("Check if no clients exist " + isEmptyClient)
                    if (server.getSocket().getAllClients().isEmpty()) {
                        startTimer(server);
                    }
                }

            }
        });

        server.getSocket().addEventListener("executeEvent", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def BMotion bms = getSession((String) d.data['id'])
                if (bms != null) {
                    def returnValue = bms.executeEvent(d.data)
                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData(returnValue)
                    }
                }
            }
        });

        server.getSocket().addEventListener("initView", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def String id = d.data.id
                def BMotion bms = getSession(id)
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
                        ackRequest.sendAckData(bms.getClientData());
                    }
                } else {
                    ackRequest.sendAckData([errors: ["Session with id " + id + " does not exists!"]]);
                }
            }
        });

        server.getSocket().addEventListener("destroySession", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def String id = d.data.id
                def BMotion bms = getSession(id)
                if (bms != null) {
                    bms.disconnect();
                }
            }
        });

        server.getSocket().addEventListener("initSession", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {

                try {

                    def String tool = d.data.tool
                    def String manifest = d.data.manifest
                    def String modelPath
                    def String model = d.data.model
                    def options = d.data.options

                    def BMotion bms

                    def String templateFolder = ""
                    if (manifest != null) { // If manifest file exists, load visualization
                        templateFolder = new File(manifest).getParent().toString()
                    }

                    // Get correct path to model
                    if (BMotionServer.MODE_ONLINE.equals(server.getServer().getMode())) {
                        modelPath = server.getServer().getWorkspacePath() + File.separator + templateFolder + File.separator + model
                    } else {
                        if (new File(model).isAbsolute()) {
                            modelPath = model
                        } else {
                            modelPath = templateFolder + File.separator + model
                        }
                    }
                    bms = initSession(server, modelPath, tool, options)
                    bms.addClientData('templateFolder', templateFolder)

                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData(bms.getId());
                    }

                } catch (BMotionException e) {
                    ackRequest.sendAckData([errors: [e.getMessage()]])
                }

            }
        });

        server.getSocket().addEventListener("evaluateFormulas", JsonObject.class, new DataListener<JsonObject>() {
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

        /*server.getSocket().addEventListener("getModelData", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def String id = d.data.id;
                def ProBVisualisation bms = getSession(id);
                if (bms != null) {
                    def String what = d.data.what;
                    def data = [];
                    if(what == 'transitions') {
                        data = bms.getModelTransitions()
                    }
                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData([data]);
                    }
                }
            }
        });*/

        server.getSocket().addEventListener("checkEvents", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def BMotion bms = getSession((String) d.data['id'])
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
                def BMotion bms = getSession((String) d.data['id'])
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
                def BMotion bms = getSession((String) d.data['id'])
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

                        def BMotion bms = getSession((String) d.data['id'])
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

                        def BMotion bms = getSession((String) d.data['id'])
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
                    log.info("Timer thread interrupted " + e.getMessage())
                } finally {
                    log.info("Exit timer thread")
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
                        log.info("Session timer thread interrupted " + e.getMessage())
                    } finally {
                        log.info("Exit session timer thread")
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

    private BMotion initSession(BMotionSocketServer server, String modelPath, String tool, options) throws BMotionException {

        def String sessionId = UUID.randomUUID() // The id should come from the client!
        def BMotion bms = createSession(sessionId, tool, server.getServer().getVisualisationProvider());
        if (bms != null && bms instanceof ProBVisualisation) {

            bms.setMode(server.getServer().getMode())
            bms.initSession(modelPath, options)
            sessions.put(sessionId, bms)
            log.info "Created new BMotion session " + sessionId
            Trace t = ((ProBVisualisation) bms).getTrace()
            bms.addClientData("id", bms.getId().toString())
            bms.addClientData("stateId", t.getCurrentState().getId())
            bms.addClientData("traceId", t.getUUID())
            bms.addClientData("initialised", t.getCurrentState().isInitialised())

            return bms;

        } else {
            throw new BMotionException("BMotion Studio session could not be initialised!")
        }

    }

    private
    static BMotion createSession(String id, String tool, BMotionVisualisationProvider visualisationProvider) throws BMotionException {
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
