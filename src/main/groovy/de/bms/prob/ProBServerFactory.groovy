package de.bms.prob

import com.google.common.io.Resources
import de.bms.server.BMotionServer

public class ProBServerFactory {

    public static BMotionServer getServer(args) {
        // Start BMotion Server
        BMotionServer server = new BMotionServer(args)
        server.setScriptEngineProvider(new ProBScriptEngineProvider())
        server.setIToolProvider(new ProBIToolProvider())
        URL[] paths = [Resources.getResource("prob")]
        server.setResourcePaths(paths)
        return server
    }

}
