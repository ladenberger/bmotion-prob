package de.bmotion.prob;

import de.bmotion.core.IBMotionApi;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;

public interface IProBVisualizationApi extends IBMotionApi {

	/**
	 * 
	 * Returns the ProB representation of the loaded formal specification.
	 * 
	 * @return The formal specification as {@link AbstractModel}
	 */
	public AbstractModel getModel();

	/**
	 * 
	 * Returns the current {@link Trace} of the animation.
	 * 
	 * @return The current {@link Trace} of the animation
	 */
	public Trace getTrace();

	/**
	 * 
	 * Returns the {@link AnimationSelector} which is the entry point to the
	 * ProB GUI.
	 * 
	 * @return The {@link AnimationSelector} which is the entry point to the
	 *         ProB GUI
	 */
	public AnimationSelector getAnimationSelector();

}
