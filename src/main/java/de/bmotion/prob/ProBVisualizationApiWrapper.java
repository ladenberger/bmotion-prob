package de.bmotion.prob;

import java.util.List;

import de.bmotion.core.BMotion;
import de.bmotion.core.BMotionApiWrapper;
import de.bmotion.prob.model.TransitionObject;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.statespace.TraceElement;

public class ProBVisualizationApiWrapper extends BMotionApiWrapper implements IProBVisualizationApi {

	public ProBVisualizationApiWrapper(BMotion bmotion) {
		super(bmotion);
	}

	@Override
	public AbstractModel getModel() {
		return ((ProBVisualization) bmotion).getModel();
	}

	@Override
	public Trace getTrace() {
		return ((ProBVisualization) bmotion).getTrace();
	}

	@Override
	public AnimationSelector getAnimationSelector() {
		return ((ProBVisualization) bmotion).getAnimationSelector();
	}

	@Override
	public List<TransitionObject> getHistory(TraceElement currentTraceElement) {
		return ((ProBVisualization) bmotion).getHistory(currentTraceElement);
	}

}
