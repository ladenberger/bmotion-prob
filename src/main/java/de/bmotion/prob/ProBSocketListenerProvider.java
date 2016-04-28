package de.bmotion.prob;

import java.util.List;
import java.util.stream.Collectors;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DataListener;

import de.bmotion.core.BMotion;
import de.bmotion.core.BMotionSocketServer;
import de.bmotion.core.IBMotionSocketListenerProvider;
import de.bmotion.core.objects.ErrorObject;
import de.bmotion.core.objects.ObserverFormulaListObject;
import de.bmotion.core.objects.SessionObject;
import de.bmotion.prob.model.TransitionObject;
import de.bmotion.prob.objects.GraphObject;
import de.bmotion.prob.objects.SessionTransitionListObject;
import de.prob.statespace.FormalismType;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob.webconsole.WebConsole;

public class ProBSocketListenerProvider implements IBMotionSocketListenerProvider {

	// private final Logger log =
	// LoggerFactory.getLogger(ProBSocketListenerProvider.class);

	@Override
	public void installListeners(BMotionSocketServer server) {

		server.getSocket().addEventListener("initProB", String.class, new DataListener<String>() {
			@Override
			public void onData(final SocketIOClient client, String str, final AckRequest ackRequest) {
				if (ackRequest.isAckRequested()) {
					ackRequest.sendAckData(WebConsole.getPort());
				}
			}
		});

		server.getSocket().addEventListener("checkEvents", SessionTransitionListObject.class,
				new DataListener<SessionTransitionListObject>() {
					@Override
					public void onData(final SocketIOClient client, SessionTransitionListObject transitions,
							final AckRequest ackRequest) {

						BMotion bms = server.getSessions().get(transitions.getSessionId());

						if (bms != null && bms instanceof ProBVisualization) {
							ProBVisualization prob = (ProBVisualization) bms;
							Trace trace = prob.getTrace();

							transitions.getTransitions().stream().forEach(sTransition -> {

								Transition trans;
								if (trace.getModel().getFormalismType().equals(FormalismType.CSP)) {
									trans = trace.getNextTransitions().stream()
											.filter(t -> t.getName().equals(sTransition.getName())).findFirst().get();
								} else {
									String fPredicate = "TRUE=TRUE";
									if (sTransition.getPredicate() != null && sTransition.getPredicate().length() > 0) {
										fPredicate = sTransition.getPredicate();
									}
									trans = trace.getCurrentState().findTransition(sTransition.getName(), fPredicate);
								}

								sTransition.setCanExecute(trans != null);
								sTransition.setId(trans != null ? trans.getId() : null);

							});

							if (ackRequest.isAckRequested()) {
								ackRequest.sendAckData(transitions.getTransitions());
							}

						}

					}
				});

		server.getSocket().addEventListener("observeNextEvents", SessionObject.class,
				new DataListener<SessionObject>() {
					@Override
					public void onData(final SocketIOClient client, SessionObject sessionObject,
							final AckRequest ackRequest) {
						BMotion bms = server.getSessions().get(sessionObject.getSessionId());
						if (bms != null && bms instanceof ProBVisualization) {

							ProBVisualization prob = (ProBVisualization) bms;
							Trace trace = prob.getTrace();
							if (trace != null) {

								List<TransitionObject> sessionTransitions = trace.getNextTransitions().stream()
										.map(transition -> {

											TransitionObject transitionObject = new TransitionObject(
													transition.getName());
											transitionObject.setId(transition.getId());
											transitionObject.setOpString(prob.getOpString(transition));
											transitionObject.getReturnValues().addAll(transition.getReturnValues());
											transitionObject.getParameters().addAll(transition.getParams());

											return transitionObject;

										}).collect(Collectors.toList());

								if (ackRequest.isAckRequested()) {
									ackRequest.sendAckData(sessionTransitions);
								}

							}

						}
					}
				});

		server.getSocket().addEventListener("observeHistory", SessionObject.class, new DataListener<SessionObject>() {
			@Override
			public void onData(final SocketIOClient client, SessionObject sessionObject, final AckRequest ackRequest) {
				BMotion bms = server.getSessions().get(sessionObject.getSessionId());
				if (bms != null && bms instanceof ProBVisualization) {
					ProBVisualization prob = (ProBVisualization) bms;
					Trace trace = prob.getTrace();
					if (ackRequest.isAckRequested()) {
						ackRequest.sendAckData(prob.getHistory(trace.getCurrent()));
					}
				}
			}
		});

		server.getSocket().addEventListener("createTraceDiagram", ObserverFormulaListObject.class,
				new DataListener<ObserverFormulaListObject>() {
					@Override
					public void onData(final SocketIOClient client, ObserverFormulaListObject oList,
							final AckRequest ackRequest) {

						BMotion bms = server.getSessions().get(oList.getSessionId());
						if (bms != null) {

							if (bms instanceof ProBVisualization) {

								ProBVisualization prob = (ProBVisualization) bms;

								GraphObject graph = prob.createTraceDiagram(oList.getFormulas());

								if (ackRequest.isAckRequested()) {
									ackRequest.sendAckData(graph.getNodes(), graph.getEdges());
								}

							} else {
								ackRequest.sendAckData(new ErrorObject(
										"Projection diagram feature only supported by EventB, ClassicalB and CSP visualizations."));
							}

						} else {
							ackRequest.sendAckData(
									new ErrorObject("Session with id " + oList.getSessionId() + " does not exists!"));
						}
					}
				});

		server.getSocket().addEventListener("createProjectionDiagram", ObserverFormulaListObject.class,
				new DataListener<ObserverFormulaListObject>() {
					@Override
					public void onData(final SocketIOClient client, ObserverFormulaListObject oList,
							final AckRequest ackRequest) {

						BMotion bms = server.getSessions().get(oList.getSessionId());
						if (bms != null) {

							if (bms instanceof BVisualization) {

								BVisualization bvis = (BVisualization) bms;

								if (ackRequest.isAckRequested()) {
									GraphObject projectionDiagram = bvis.createProjectionDiagram(oList.getFormulas());
									ackRequest.sendAckData(projectionDiagram.getNodes(), projectionDiagram.getEdges());
								}

							} else {
								ackRequest.sendAckData(new ErrorObject(
										"Projection diagram feature only supported by EventB and ClassicalB visualizations."));
							}

						} else {
							ackRequest.sendAckData(
									new ErrorObject("Session with id " + oList.getSessionId() + " does not exists!"));
						}

					}
				});

	}

}
