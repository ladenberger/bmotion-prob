package de.bmotion.prob;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.bmotion.core.BMotionException;
import de.bmotion.core.BMotionServer;
import de.prob.webconsole.WebConsole;

public class Online {

	public static void main(final String[] args) throws InterruptedException, BMotionException {

		if (!Arrays.asList(args).contains("-workspace")) {
			throw new InterruptedException("Please specify workspace.");
		}

		// Start BMotion Server
		BMotionServer server = ProBServerFactory.getServer(args);
		server.setMode(BMotionServer.MODE_ONLINE);
		server.start();

		// Start ProB Server
		new Thread(new Runnable() {
			public void run() {
				List<String> probargs = new ArrayList<String>();
				probargs.add("-s");
				probargs.add("-multianimation");
				if (server.getCmdLine().hasOption("local")) {
					probargs.add("-local");
				}
				if (server.getCmdLine().hasOption("probPort")) {
					probargs.add("-port");
					probargs.add(server.getCmdLine().getOptionValue("probPort"));
				}
				de.prob.servlet.Main.main(probargs.toArray(new String[probargs.size()]));
			}
		}).start();

		System.out.println("ProB 2 Server started on port " + WebConsole.getPort());

	}

}
