package de.bms.prob

import com.corundumstudio.socketio.AckRequest
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.listener.DataListener
import de.bms.DesktopApi
import de.bms.server.BMotionServer
import de.prob.webconsole.WebConsole

public class Main {

    public static void main(final String[] args) throws InterruptedException {

        def probPort = "8081"

        // Start BMotion Server
        BMotionServer server = BMotionProB.start(args)

        if (server.standalone) {
            server.socketServer.getServer().addEventListener("initProB", String.class, new DataListener<String>() {
                @Override
                public void onData(final SocketIOClient client, String str,
                                   final AckRequest ackRequest) {
                    if (ackRequest.isAckRequested())
                        ackRequest.sendAckData([host: "localhost", port: probPort]);
                }
            });
        }

        new Thread(new Runnable() {
            public void run() {
                WebConsole.run("127.0.0.1", new Runnable() {
                    @Override
                    public void run() {
                        probPort = WebConsole.getPort()
                    }
                });
            }
        }).start();

        openBrowser(server)

    }

    static def openBrowser(BMotionServer server) {
        java.net.URI uri = new java.net.URI("http://" + server.host + ":" + server.port + "/bms/")
        DesktopApi.browse(uri)
    }

}
