package de.bms.prob

import de.bms.server.BMotionServer

public class Main {

    public static void main(final String[] args) throws InterruptedException {

        // Start BMotion Server
        BMotionServer server = ProBServerFactory.getServer(args)
        server.start()

        new Thread(new Runnable() {
            public void run() {
                String[] probargs = args.contains("-local") ? ["-local", "-s", "-multianimation"] :
                        ["-s", "-multianimation"]
                de.prob.Main.main(probargs)
            }
        }).start();

        server.openBrowser();

    }

}
