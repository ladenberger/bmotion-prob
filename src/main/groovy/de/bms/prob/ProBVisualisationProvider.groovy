package de.bms.prob

import de.bms.BMotion
import de.bms.BMotionVisualisationProvider

public class ProBVisualisationProvider implements BMotionVisualisationProvider {

    @Override
    BMotion get(String type) {
        BMotion visualisation
        def scriptEngineProvider = new ProBScriptEngineProvider()
        def sessionId = UUID.randomUUID()
        switch (type) {
            case "BAnimation":
                visualisation = new BVisualisation(sessionId, scriptEngineProvider)
                break
            case "CSPAnimation":
                visualisation = new CSPVisualisation(sessionId, scriptEngineProvider)
                break
        }
        return visualisation
    }

}
