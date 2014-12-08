package de.bms.prob

import com.corundumstudio.socketio.AckRequest
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.listener.DataListener
import com.google.common.io.Resources
import de.bms.BMotion
import de.bms.server.BMotionServer
import de.bms.server.NameDataObject
import de.prob.statespace.Trace

public class ProBServerFactory {

    public static BMotionServer getServer(args) {
        BMotionServer server = new BMotionServer(args, new ProBVisualisationProvider())
        URL[] paths = [Resources.getResource("prob")]
        server.setResourcePaths(paths)
        server.socketServer.getServer().
                addEventListener("initTooltip", NameDataObject.class, new DataListener<NameDataObject>() {
                    @Override
                    public void onData(final SocketIOClient client, NameDataObject d,
                                       final AckRequest ackRequest) {

                        String path = server.socketServer.clients.get(client)
                        def BMotion bmotion = server.socketServer.sessions.get(path) ?: null
                        if (bmotion != null) {

                            Trace t = bmotion.getTrace()
                            def event = d.name
                            def predicate = d.data.predicate == null ? [] : d.data.predicate
                            def alternative = d.data.alternative == null ? [] : d.data.alternative

                            def eventMap = alternative.collect {
                                def p = it.predicate == null ? [] : it.predicate
                                [name: it.name, predicate: p, canExecute: t.canExecuteEvent(it.name, p)]
                            }
                            eventMap << [name: event, predicate: predicate, canExecute: t.
                                    canExecuteEvent(event, predicate)]

                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData([events: eventMap]);
                            }

                        }

                    }
                });
        return server
    }

}
