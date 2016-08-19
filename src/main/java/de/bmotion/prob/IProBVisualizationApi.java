package de.bmotion.prob;

import java.util.List;

import de.bmotion.core.IBMotionApi;
import de.bmotion.prob.model.TransitionObject;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob.statespace.TraceElement;

public interface IProBVisualizationApi extends IBMotionApi {

	public AbstractModel getModel();

	public Trace getTrace();

	public AnimationSelector getAnimationSelector();
	
	public List<TransitionObject> getHistory(TraceElement currentTraceElement);

}
