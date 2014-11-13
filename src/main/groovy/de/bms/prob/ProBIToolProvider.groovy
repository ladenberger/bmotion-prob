package de.bms.prob

import de.bms.itool.ITool
import de.bms.itool.ToolRegistry
import de.bms.server.BMotionIToolProvider

public class ProBIToolProvider implements BMotionIToolProvider {

    @Override
    ITool get(String tool, ToolRegistry toolRegistry) {
        ITool itool
        switch (tool) {
            case "BAnimation":
                itool = new BAnimation(UUID.randomUUID().toString(), toolRegistry)
                break
            case "CSPAnimation":
                itool = new CSPAnimation(UUID.randomUUID().toString(), toolRegistry)
                break
            default:
                itool = new BAnimation(UUID.randomUUID().toString(), toolRegistry)
        }
        return itool
    }

}
