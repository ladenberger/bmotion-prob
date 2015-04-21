package de.bms.prob

import de.bms.server.BMotionServer

public class Main {

    public static void main(final String[] args) throws InterruptedException {

        // Start BMotion Server
        BMotionServer server = ProBServerFactory.getServer(args)
        server.start()

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

        //server.openBrowser();

    }

}
