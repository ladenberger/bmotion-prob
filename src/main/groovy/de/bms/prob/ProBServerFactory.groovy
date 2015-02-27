package de.bms.prob

import com.google.common.io.Resources
import de.bms.server.BMotionServer

public class ProBServerFactory {

    public static BMotionServer getServer(args) {
        BMotionServer server = new BMotionServer(args, new ProBOptionProvider())
        URL[] paths = [Resources.getResource("prob")]
        server.setResourcePaths(paths)
        server.setVisualisationProvider(new ProBVisualisationProvider())
        server.setSocketListenerProvider(new ProBSocketListenerProvider())
        return server
    }

}
