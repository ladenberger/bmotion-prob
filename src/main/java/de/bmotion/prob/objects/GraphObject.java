package de.bmotion.prob.objects;

import java.util.List;

public class GraphObject {

	private List<GraphNodeEdgeObject> nodes;

	private List<GraphNodeEdgeObject> edges;

	public GraphObject(List<GraphNodeEdgeObject> nodes, List<GraphNodeEdgeObject> edges) {
		this.nodes = nodes;
		this.edges = edges;
	}

	public List<GraphNodeEdgeObject> getNodes() {
		return nodes;
	}

	public List<GraphNodeEdgeObject> getEdges() {
		return edges;
	}

}
