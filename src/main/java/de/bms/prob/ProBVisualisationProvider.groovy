package de.bms.prob

import de.bms.BMotion
import de.bms.BMotionVisualisationProvider

public class ProBVisualisationProvider implements BMotionVisualisationProvider {

    @Override
    public BMotion get(String id, String type) {
        def scriptEngineProvider = new ProBScriptEngineProvider()
        def UUID uuid = UUID.fromString(id);
        switch (type) {
            case "BAnimation":
                return new BVisualisation(uuid, scriptEngineProvider)
            case "CSPAnimation":
                return new CSPVisualisation(uuid, scriptEngineProvider)
        }
        return null
    }

}
