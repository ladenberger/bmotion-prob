package de.bmotion.prob;

import de.bmotion.core.BMotionException;
import de.bmotion.core.BMotionServer;

public class ProBServerFactory {

	public static BMotionServer getServer(String[] args) throws BMotionException {
		BMotionServer server = new BMotionServer(args, new ProBOptionProvider(), new ProBVisualizationProvider());
		server.getSocketListenerProvider().add(new ProBSocketListenerProvider());
		// URL[] paths = new URL[] { Resources.getResource("prob") };
		// server.setResourcePaths(paths);
		return server;
	}

}
