package de.bmotion.prob;

import java.util.List;
import java.util.stream.Collectors;

import de.bmotion.prob.model.ConstantObject;
import de.bmotion.prob.model.ModelObject;
import de.bmotion.prob.model.SetObject;
import de.bmotion.prob.model.TransitionObject;
import de.bmotion.prob.model.VariableObject;
import de.prob.model.eventb.Event;
import de.prob.model.eventb.EventBMachine;
import de.prob.model.eventb.EventBModel;
import de.prob.model.eventb.EventBVariable;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.BEvent;
import de.prob.model.representation.Variable;
import de.prob.statespace.Trace;

public class EventBVisualization extends BVisualization {

	public EventBVisualization(String sessionId) {
		super(sessionId);
	}

	public EventBVisualization() {
		super();
	}

	@Override
	protected void updateBModelData(Trace t) {

		AbstractElement mainComponent = t.getStateSpace().getMainComponent();

		if (mainComponent instanceof EventBMachine) {

			ModelObject modelObject = (ModelObject) toolData.get("model");

			// Get model transitions and parameters
			List<TransitionObject> modelTransitions = mainComponent.getChildrenOfType(BEvent.class).stream().map(it -> {
				Event op = (Event) it;
				TransitionObject transitionObject = new TransitionObject(op.getName());
				List<String> parameters = op.getParameters().stream().map(p -> p.getName())
						.collect(Collectors.toList());
				transitionObject.getParameters().addAll(parameters);
				return transitionObject;
			}).collect(Collectors.toList());
			modelObject.getTransitions().addAll(modelTransitions);

			// Get model variables
			List<VariableObject> modelVariables = mainComponent.getChildrenOfType(Variable.class).stream()
					.map(it -> new VariableObject(((EventBVariable) it).getName())).collect(Collectors.toList());
			modelObject.getVariables().addAll(modelVariables);

			if (t.getModel() instanceof EventBModel) {

				EventBModel model = (EventBModel) t.getModel();

				// Get model refinements
				List<String> modelRefinements = model.getMachines().stream().map(it -> it.getName())
						.collect(Collectors.toList());
				modelObject.getRefinements().addAll(modelRefinements);

				model.getContexts().forEach(context -> {

					List<ConstantObject> modelConstants = context.getConstants().stream()
							.map(constant -> new ConstantObject(constant.getName())).collect(Collectors.toList());
					modelObject.getConstants().addAll(modelConstants);

					List<SetObject> modelSets = context.getSets().stream().map(it -> new SetObject(it.getName()))
							.collect(Collectors.toList());
					modelObject.getSets().addAll(modelSets);

				});

				// Get model constants
				/*
				 * List<ConstantObject> modelConstants =
				 * model.getChildrenOfType(Constant.class).stream() .map(it ->
				 * new ConstantObject(((EventBConstant)
				 * it).getName())).collect(Collectors.toList());
				 * modelObject.getConstants().addAll(modelConstants);
				 */

			}

		}

	}

}
