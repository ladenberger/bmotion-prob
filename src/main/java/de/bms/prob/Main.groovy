package de.bms.prob

import de.bms.BMotionServer
import de.prob.webconsole.WebConsole

public class Main {

    public static void main(final String[] args) throws InterruptedException {

        // Start BMotion Server
        BMotionServer server = ProBServerFactory.getServer(args)
        server.setMode(BMotionServer.MODE_INTEGRATED)
        server.startWithJetty()

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

        System.out.println("ProB 2 Server started on port " + WebConsole.getPort())

        server.openBrowser()

    }

}
