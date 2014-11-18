package de.bms.prob

import com.corundumstudio.socketio.AckRequest
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.listener.DataListener
import com.google.common.io.Resources
import de.bms.BMotion
import de.bms.DesktopApi
import de.bms.server.BMotionServer
import de.prob.webconsole.WebConsole

public class Main {

    public static void main(final String[] args) throws InterruptedException {

        def probPort = "8081"

        // Start BMotion Server
        BMotionServer server = new BMotionServer(args)

        server.setScriptEngineProvider(new ProBScriptEngineProvider())
        server.setIToolProvider(new ProBIToolProvider())

        String[] paths = [Resources.getResource("prob").toString()]
        server.setResourcePaths(paths)

        server.start()

        server.socketServer.getServer().addEventListener("initProB", String.class, new DataListener<String>() {
            @Override
            public void onData(final SocketIOClient client, String str,
                               final AckRequest ackRequest) {

                def String url = server.socketServer.clients.get(client)
                def BMotion session = server.socketServer.sessions.get(url)
                if (session != null)
                    session.getTool().refresh()
                if (ackRequest.isAckRequested())
                    ackRequest.sendAckData([host: "localhost", port: probPort]);

            }
        });

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

        /*new Thread(new Runnable() {
            public void run() {
                de.prob.Main m = de.prob.Main.getInjector().getInstance(de.prob.Main.class);
                String[] probargs = ["-local", "-s"]
                m.main(probargs)
            }
        }).start();*/

    }

    static def openBrowser(BMotionServer server) {
        java.net.URI uri = new java.net.URI("http://" + server.host + ":" + server.port + "/bms/")
        DesktopApi.browse(uri)
    }

}
