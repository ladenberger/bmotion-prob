package de.bmotion.prob.model;


import java.util.ArrayList;
import java.util.List;

public class ModelObject {
	
	private List<TransitionObject> transitions;
	private List<VariableObject> variables;
	private List<ConstantObject> constants;
	private List<SetObject> sets;
	private List<String> refinements;

	public ModelObject() {
		this.transitions = new ArrayList<TransitionObject>();
		this.variables = new ArrayList<VariableObject>();
		this.constants = new ArrayList<ConstantObject>();
		this.sets = new ArrayList<SetObject>();
		this.refinements = new ArrayList<String>();
	}

	public List<TransitionObject> getTransitions() {
		return transitions;
	}

	public List<VariableObject> getVariables() {
		return variables;
	}

	public List<SetObject> getSets() {
		return sets;
	}

	public List<String> getRefinements() {
		return refinements;
	}

	public List<ConstantObject> getConstants() {
		return constants;
	}	

}
