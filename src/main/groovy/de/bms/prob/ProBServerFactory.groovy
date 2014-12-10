package de.bms.prob

import com.corundumstudio.socketio.AckRequest
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.listener.DataListener
import com.google.common.io.Resources
import de.bms.BMotion
import de.bms.server.BMotionServer
import de.bms.server.JsonObject
import de.prob.model.eventb.EventBMachine
import de.prob.statespace.Trace

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
                        String path = server.socketServer.clients.get(client)
                        def BMotion bmotion = server.socketServer.sessions.get(path) ?: null
                        if (bmotion != null) {
                            Trace t = bmotion.getTrace()
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
                        String path = server.socketServer.clients.get(client)
                        def BMotion bmotion = server.socketServer.sessions.get(path) ?: null
                        if (bmotion != null) {
                            Trace t = bmotion.getTrace()
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
                                ackRequest.sendAckData([_getrefs(eventBMachine.refines).reverse() << eventBMachine.toString()]);
                            }
                        }
                    }
                });
        return server
    }

}
