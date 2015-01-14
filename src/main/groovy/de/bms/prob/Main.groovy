package de.bms.prob

import com.corundumstudio.socketio.AckRequest
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.listener.DataListener
import de.bms.server.BMotionServer
import de.prob.webconsole.WebConsole

public class Main {

    public static void main(final String[] args) throws InterruptedException {

        // Start BMotion Server
        BMotionServer server = ProBServerFactory.getServer(args)
        server.start()
        if (server.standalone) {
            server.socketServer.getServer().addEventListener("initProB", String.class, new DataListener<String>() {
                @Override
                public void onData(final SocketIOClient client, String str,
                                   final AckRequest ackRequest) {
                    if (ackRequest.isAckRequested())
                        ackRequest.sendAckData([host: "localhost", port: WebConsole.getPort()]);
                }
            });
        }

        new Thread(new Runnable() {
            public void run() {
                String[] probargs = ["-local", "-s"]
                de.prob.Main.main(probargs)
            }
        }).start();

    }

}
