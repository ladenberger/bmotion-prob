package de.bms.prob

import de.bms.BMotionServer
import groovy.util.logging.Slf4j

@Slf4j
public class Standalone {

    public Standalone(String[] args) {

        // Start BMotion Server
        BMotionServer server = ProBServerFactory.getServer(args)
        server.setMode(BMotionServer.MODE_STANDALONE)
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

    public static void main(final String[] args) throws InterruptedException {
        new Standalone(args);
    }

}
