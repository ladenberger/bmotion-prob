package de.bmotion.prob;

import java.util.List;
import java.util.stream.Collectors;

import de.bmotion.prob.model.ConstantObject;
import de.bmotion.prob.model.ModelObject;
import de.bmotion.prob.model.SetObject;
import de.bmotion.prob.model.TransitionObject;
import de.bmotion.prob.model.VariableObject;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.model.classicalb.ClassicalBConstant;
import de.prob.model.classicalb.ClassicalBMachine;
import de.prob.model.classicalb.ClassicalBVariable;
import de.prob.model.classicalb.Operation;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.BEvent;
import de.prob.model.representation.Constant;
import de.prob.model.representation.Set;
import de.prob.model.representation.Variable;
import de.prob.statespace.Trace;

public class ClassicalBVisualization extends BVisualization {

	public ClassicalBVisualization(String sessionId) {
		super(sessionId);
	}

	public ClassicalBVisualization() {
		super();
	}

	@Override
	protected void updateBModelData(Trace t) {

		AbstractElement mainComponent = t.getStateSpace().getMainComponent();
		if (mainComponent instanceof ClassicalBMachine) {

			ModelObject modelObject = (ModelObject) toolData.get("model");

			// Get model transitions and parameters
			List<TransitionObject> modelTransitions = mainComponent.getChildrenOfType(BEvent.class).stream().map(it -> {
				Operation op = (Operation) it;
				TransitionObject transitionObject = new TransitionObject(op.getName());
				transitionObject.getParameters().addAll(op.getParameters());
				transitionObject.getReturnValues().addAll(op.getOutput());
				return transitionObject;
			}).collect(Collectors.toList());
			modelObject.getTransitions().addAll(modelTransitions);

			// Get model constants
			List<ConstantObject> modelConstants = mainComponent.getChildrenOfType(Constant.class).stream()
					.map(it -> new ConstantObject(((ClassicalBConstant) it).getName())).collect(Collectors.toList());
			modelObject.getConstants().addAll(modelConstants);

			// Get model variables
			List<VariableObject> modelVariables = mainComponent.getChildrenOfType(Variable.class).stream()
					.map(it -> new VariableObject(((ClassicalBVariable) it).getName())).collect(Collectors.toList());
			modelObject.getVariables().addAll(modelVariables);

			// Get model sets
			List<SetObject> modelSets = mainComponent.getChildrenOfType(Set.class).stream()
					.map(it -> new SetObject(it.getName())).collect(Collectors.toList());
			modelObject.getSets().addAll(modelSets);

		}

	}

	@Override
	protected IEvalElement getEvalElement(String formula) {
		return new ClassicalB(formula, FormulaExpand.expand);
	}

}
