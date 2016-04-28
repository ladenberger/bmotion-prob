package de.bmotion.prob;

import java.util.Map;

import de.bmotion.core.BMotion;
import de.bmotion.core.IBMotionVisualizationProvider;

public class ProBVisualizationProvider implements IBMotionVisualizationProvider {

	@Override
	public BMotion get(String sessionId, String modelPath, Map<String, String> options) {

		switch (getFormalism(modelPath)) {
		case "b":
			return new ClassicalBVisualization(sessionId);
		case "eventb":
			return new EventBVisualization(sessionId);
		case "csp":
			return new CSPVisualization(sessionId);
		default:
			return null;
		}

	}

	private String getFormalism(final String modelPath) {

		String fileExtension = modelPath.substring(modelPath.length() - 3);
		switch (fileExtension) {
		case "csp":
			return "csp";
		case "buc":
		case "bcc":
		case "bum":
		case "bcm":
			return "eventb";
		case "mch":
			return "b";
		case "tla":
			return "tla";
		default:
			return null;
		}

	}

}
