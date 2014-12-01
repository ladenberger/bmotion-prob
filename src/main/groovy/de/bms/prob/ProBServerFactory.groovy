package de.bms.prob

import com.google.common.io.Resources
import de.bms.server.BMotionServer

public class ProBServerFactory {

    public static BMotionServer getServer(args) {
        BMotionServer server = new BMotionServer(args)
        server.setIToolProvider(new ProBVisualisationProvider())
        URL[] paths = [Resources.getResource("prob")]
        server.setResourcePaths(paths)
        return server
    }

}
