package de.bmotion.prob.model;

import java.util.ArrayList;
import java.util.List;

public class TransitionObject {

	private String name;

	private String predicate;

	private String id;

	private boolean canExecute;

	private String opString;

	private String group;

	private List<String> parameters;

	private List<String> returnValues;
	
	private int index;
	
	private String executor;

	public TransitionObject() {
		
	}
	
	public TransitionObject(String name) {
		super();
		this.name = name;
		this.parameters = new ArrayList<String>();
		this.returnValues = new ArrayList<String>();
	}

	public TransitionObject(String name, String predicate) {
		super();
		this.name = name;
		this.predicate = predicate;
		this.parameters = new ArrayList<String>();
		this.returnValues = new ArrayList<String>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isCanExecute() {
		return canExecute;
	}

	public void setCanExecute(boolean canExecute) {
		this.canExecute = canExecute;
	}

	public String getOpString() {
		return opString;
	}

	public void setOpString(String opString) {
		this.opString = opString;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public List<String> getParameters() {
		return parameters;
	}

	public void setParameters(List<String> parameters) {
		this.parameters = parameters;
	}

	public List<String> getReturnValues() {
		return returnValues;
	}

	public void setReturnValues(List<String> returnValues) {
		this.returnValues = returnValues;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getExecutor() {
		return executor;
	}

	public void setExecutor(String executor) {
		this.executor = executor;
	}
	
}
