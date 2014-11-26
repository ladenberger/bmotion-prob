package de.bms.prob

import com.google.common.io.Resources
import de.bms.server.BMotionServer

public class BMotionProB {

    public static BMotionServer start(args) {
        // Start BMotion Server
        BMotionServer server = new BMotionServer(args)
        server.setScriptEngineProvider(new ProBScriptEngineProvider())
        server.setIToolProvider(new ProBIToolProvider())
        String[] paths = [Resources.getResource("prob").toString()]
        server.setResourcePaths(paths)
        server.start()
        return server
    }

}
