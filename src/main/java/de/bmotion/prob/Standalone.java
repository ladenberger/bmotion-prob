package de.bmotion.prob;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.bmotion.core.BMotionException;
import de.bmotion.core.BMotionServer;
import de.prob.webconsole.WebConsole;

public class Standalone {

	private final static Logger log = LoggerFactory.getLogger(Standalone.class);

	public static void main(final String[] args) throws InterruptedException, BMotionException {

		// Start BMotion Server
		BMotionServer server = ProBServerFactory.getServer(args);
		server.setMode(BMotionServer.MODE_STANDALONE);
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

		log.info("ProB 2 Server started on port " + WebConsole.getPort());

	}

}
