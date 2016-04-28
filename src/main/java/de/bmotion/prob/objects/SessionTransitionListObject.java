package de.bmotion.prob.objects;

import java.util.List;

import de.bmotion.prob.model.TransitionObject;

public class SessionTransitionListObject {
	
	private String sessionId;

	private String traceId;

	private List<TransitionObject> transitions;
	
	public SessionTransitionListObject() {
	}
	
	public SessionTransitionListObject(String sessionId, String traceId, List<TransitionObject> transitions) {
		super();
		this.sessionId = sessionId;
		this.traceId = traceId;
		this.transitions = transitions;		
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public List<TransitionObject> getTransitions() {
		return transitions;
	}

	public void setTransitions(List<TransitionObject> transitions) {
		this.transitions = transitions;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}


}
