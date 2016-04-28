package de.bmotion.prob.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphNodeEdgeObject {
	
	private String group;

	private final List<String> errors;
	
	private final Map<String, Object> data;
	
	private Map<String, Map<String, Object>> results;
	
	public GraphNodeEdgeObject(String group) {
		this.group = group;
		this.data = new HashMap<String, Object>();
		this.errors = new ArrayList<String>();
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public Map<String, Map<String, Object>> getResults() {
		return results;
	}

	public void setResults(Map<String, Map<String, Object>> results) {
		this.results = results;
	}

	public List<String> getErrors() {
		return errors;
	}

}
