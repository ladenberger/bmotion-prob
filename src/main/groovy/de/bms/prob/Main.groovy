package de.bms.prob

import com.corundumstudio.socketio.AckRequest
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.listener.ConnectListener
import com.corundumstudio.socketio.listener.DataListener
import com.google.common.io.Resources
import de.bms.server.BMotionServer
import de.prob.webconsole.WebConsole
import groovy.util.logging.Slf4j

@Slf4j
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

        server.socketServer.getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                client.sendEvent("initProB", [host: "localhost", port: probPort])
            }
        });

        WebConsole.run("127.0.0.1", new Runnable() {
            @Override
            public void run() {
                probPort = WebConsole.getPort()
            }
        });

        /*new Thread(new Runnable() {
            public void run() {
                de.prob.Main m = de.prob.Main.getInjector().getInstance(de.prob.Main.class);
                String[] probargs = ["-local", "-s"]
                m.main(probargs)
            }
        }).start();*/

    }

}
