package de.bms.prob

import de.bms.BMotion
import de.bms.BMotionVisualisationProvider

public class ProBVisualisationProvider implements BMotionVisualisationProvider {

    @Override
    BMotion get(String type, String templatePath) {
        BMotion visualisation
        def scriptEngineProvider = new ProBScriptEngineProvider()
        def sessionId = UUID.randomUUID()
        switch (type) {
            case "BAnimation":
                visualisation = new BVisualisation(sessionId, templatePath, scriptEngineProvider)
                break
            case "CSPAnimation":
                visualisation = new CSPVisualisation(sessionId, templatePath, scriptEngineProvider)
                break
        }
        return visualisation
    }

}
