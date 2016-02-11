package de.bms.prob

import de.bms.BMotion
import de.bms.BMotionVisualisationProvider

public class ProBVisualisationProvider implements BMotionVisualisationProvider {

    @Override
    public BMotion get(options) {

        def ProBScriptEngineProvider scriptEngineProvider = new ProBScriptEngineProvider()
        def UUID uuid = UUID.randomUUID()

        switch (getFormalism((String) options.model)) {
            case { it == "eventb" || it == "b" || it == "tla" }:
                return new BVisualisation(uuid, scriptEngineProvider)
            case "csp":
                return new CSPVisualisation(uuid, scriptEngineProvider)
        }

        return null

    }

    private String getFormalism(final String modelPath) {
        switch (modelPath[-3..-1]) {
            case "csp":
                return "csp";
                break
            case { it == "buc" || it == "bcc" || it == "bum" || it == "bcm" }:
                return "eventb";
                break
            case "mch":
                return "b";
                break
            case "tla":
                return "tla";
                break
            default:
                return null;
                break;
        }
    }

}
