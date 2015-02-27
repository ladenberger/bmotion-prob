package de.bms.prob

import de.bms.BMotionOptionProvider
import org.apache.commons.cli.OptionBuilder
import org.apache.commons.cli.Options

public class ProBOptionProvider implements BMotionOptionProvider {

    @Override
    void installOptions(Options options) {
        options.addOption(OptionBuilder.withArgName("probPort").hasArg()
                .withDescription("ProB 2 Port").create("probPort"))
    }

}
