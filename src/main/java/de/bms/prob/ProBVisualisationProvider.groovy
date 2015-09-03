package de.bms.prob

import de.bms.BMotion
import de.bms.BMotionVisualisationProvider

public class ProBVisualisationProvider implements BMotionVisualisationProvider {

    @Override
    BMotion get(String id, String type) {
        BMotion visualisation
        def scriptEngineProvider = new ProBScriptEngineProvider()
        def UUID uuid = UUID.fromString(id);
        switch (type) {
            case "BAnimation":
                visualisation = new BVisualisation(uuid, scriptEngineProvider)
                break
            case "CSPAnimation":
                visualisation = new CSPVisualisation(uuid, scriptEngineProvider)
                break
        }
        return visualisation
    }

}
