package de.bmotion.prob.objects;

import java.util.ArrayList;
import java.util.List;

public class BEventReturnObject {
	
	private List<String> returnValues;
	
	private String stateId;

	public BEventReturnObject(String stateId) {
		super();
		this.returnValues = new ArrayList<String>();
		this.stateId = stateId;
	}

	public List<String> getReturnValues() {
		return returnValues;
	}

	public String getStateId() {
		return stateId;
	}

}
