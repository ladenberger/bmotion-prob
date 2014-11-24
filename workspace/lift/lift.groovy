import de.bms.observer.TransformerObserver
import de.prob.statespace.Trace

TransformerObserver.make {
    selector "#door"
    set "fill", { (bms.eval("door_open").value == "TRUE") ? "white" : "lightgray" }
    set "y", {
        switch (bms.eval("cur_floor").value) {
            case "-1": "275"
                break
            case "0": "175"
                break
            case "1": "60"
                break
            default: "275"
        }
    }
    register(bms)
}

// --------------------------
// Floor Labels
// --------------------------
//Reset floor labels
TransformerObserver.make {
    selector "[id^=txt_floor]"
    style "font-weight", "normal"
    register(bms)
}
// Set current floor label to bold
TransformerObserver.make {
    selector { "#txt_floor" + bms.eval("cur_floor").value }
    style "font-weight", "bold"
    register(bms)
}

TransformerObserver.make {
    selector "#txt_cur_floor"
    set "text", { bms.eval("cur_floor").value }
    register(bms)
}

bms.registerMethod("openCloseDoor", {
    def Trace t = bms.getTool().getTrace()
    def Trace newTrace = executeEvent(t, "open_door", []) ?: executeEvent(t, "close_door", [])
    if (newTrace != null) {
        animations.traceChange(newTrace)
        return [newState: newTrace.getCurrentState().id]
    }
})

def Trace executeEvent(t, name, pred) {
    try {
        t.execute(name, pred)
    } catch (IllegalArgumentException e) {
        null
    }
}
