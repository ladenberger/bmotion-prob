package de.bmotion.prob;

import de.bmotion.core.BMotion;
import de.bmotion.core.BMotionApiWrapper;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

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

}
