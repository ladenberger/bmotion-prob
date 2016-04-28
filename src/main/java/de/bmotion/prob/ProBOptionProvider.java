package de.bmotion.prob;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import de.bmotion.core.IBMotionOptionProvider;

public class ProBOptionProvider implements IBMotionOptionProvider {

	@Override
	public void installOptions(Options options) {
		options.addOption(Option.builder("probPort").hasArg().desc("ProB 2 Port").argName("probPort").build());
	}

}
