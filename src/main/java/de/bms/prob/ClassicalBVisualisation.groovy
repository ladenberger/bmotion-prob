package de.bms.prob

import de.bms.BMotionScriptEngineProvider
import de.prob.model.classicalb.ClassicalBMachine
import de.prob.model.classicalb.ClassicalBVariable
import de.prob.model.classicalb.Operation
import de.prob.model.representation.AbstractElement
import de.prob.model.representation.BEvent
import de.prob.statespace.Trace
import groovy.util.logging.Slf4j

@Slf4j
public class ClassicalBVisualisation extends BVisualisation {

    public ClassicalBVisualisation(final UUID id, final BMotionScriptEngineProvider scriptEngineProvider) {
        super(id, scriptEngineProvider)
    }

    @Override
    protected void updateBModelData(Trace t) {

        AbstractElement mainComponent = t.getStateSpace().getMainComponent()
        if (mainComponent instanceof ClassicalBMachine) {
            // Collect operations
            mainComponent.getChildrenOfType(BEvent.class).each { BEvent o ->
                if (o instanceof Operation) {
                    clientData["model"]["events"] << [
                            name     : o.getName(),
                            parameter: ((Operation) o).getParameters().collect {
                                return [name: it]
                            }
                    ]
                }
            }
            clientData["model"]["variables"] = mainComponent.getChildrenOfType(ClassicalBVariable.class).collect { ClassicalBVariable v ->
                return [ name: v.getName() ]
            }
        }

    }

}
