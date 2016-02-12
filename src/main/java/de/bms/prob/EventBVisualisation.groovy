package de.bms.prob

import de.bms.BMotionScriptEngineProvider
import de.prob.model.eventb.*
import de.prob.model.representation.AbstractElement
import de.prob.model.representation.Machine
import de.prob.statespace.Trace
import groovy.util.logging.Slf4j

@Slf4j
public class EventBVisualisation extends BVisualisation {

    public EventBVisualisation(final UUID id, final BMotionScriptEngineProvider scriptEngineProvider) {
        super(id, scriptEngineProvider)
    }

    @Override
    protected void updateBModelData(Trace t) {

        AbstractElement mainComponent = t.getStateSpace().getMainComponent()
        if (mainComponent instanceof EventBMachine) {
            // Collect events
            clientData["model"]["events"] = mainComponent.getChildrenOfType(Event.class).collect { Event e ->
                return [
                        name     : e.getName(),
                        parameter: e.getParameters().collect { EventParameter p ->
                            return [name: p.getName(), comment: p.getComment()]
                        },
                        guards   : e.getGuards().collect { EventBGuard g ->
                            return [name: g.getName(), comment: g.getComment(), isTheorem: g.isTheorem()]
                        }
                ]
            }
            clientData["model"]["variables"] = mainComponent.getChildrenOfType(EventBVariable.class).collect { EventBVariable v ->
                return [name: v.getName(), comment: v.getComment()]
            }
        }

        if (t.getModel() instanceof EventBModel) {
            EventBModel model = (EventBModel) t.getModel()
            clientData["model"]["refinements"] = model.getMachines().collect { Machine m ->
                return m.getName()
            }
        }

    }

}
