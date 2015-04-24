package de.bms.prob

import de.bms.BMotionServer
import de.bms.SocketServerListener

public class Standalone {

    public static void main(final String[] args) throws InterruptedException {

        def Process p = null;

        // Start BMotion Server
        BMotionServer server = ProBServerFactory.getServer(args)
        server.setMode(BMotionServer.MODE_STANDALONE)
        server.setServerStartedListener(new SocketServerListener() {

            @Override
            void serverStarted(String clientApp) {
                // Open Client
                try {
                    Runtime runTime = Runtime.getRuntime();
                    p = runTime.exec(clientApp);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            void serverCloseRequest() {
                System.out.println("Exit application")
                if (p != null) {
                    p.destroy()
                    System.exit(0);
                }
            }

        });
        server.start()

        // Start ProB Server
        new Thread(new Runnable() {
            public void run() {
                def probargs = ["-s", "-multianimation"]
                if (server.cmdLine.hasOption("local")) {
                    probargs << "-local"
                }
                if (server.cmdLine.hasOption("probPort")) {
                    probargs << "-port"
                    probargs << server.cmdLine.getOptionValue("probPort")
                }
                de.prob.Main.main(probargs.toArray(new String[probargs.size()]))
            }
        }).start();

    }

}
