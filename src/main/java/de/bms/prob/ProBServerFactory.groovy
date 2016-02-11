package de.bms.prob

import com.google.common.io.Resources
import de.bms.BMotionServer

public class ProBServerFactory {

    public static BMotionServer getServer(String[] args) {
        BMotionServer server = new BMotionServer(args, new ProBOptionProvider())
        server.setVisualisationProvider(new ProBVisualisationProvider())
        server.getSocketListenerProvider() << new ProBSocketListenerProvider()
        URL[] paths = [Resources.getResource("prob")]
        server.setResourcePaths(paths)
        return server
    }

}
