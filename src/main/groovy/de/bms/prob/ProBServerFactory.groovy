package de.bms.prob

import de.bms.BMotionServer

public class ProBServerFactory {

    public static BMotionServer getServer(args) {
        BMotionServer server = new BMotionServer(args, new ProBOptionProvider())
        server.setVisualisationProvider(new ProBVisualisationProvider())
        server.setSocketListenerProvider(new ProBSocketListenerProvider())
        return server
    }

}
