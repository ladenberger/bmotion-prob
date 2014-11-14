import de.bms.observer.TransformerObserver

TransformerObserver.make {
    selector "#door"
    set "fill", { (bms.eval("door_open").value == "TRUE") ? "white" : "lightgray" }
    set "y", {
        switch ( bms.eval("cur_floor").value ) {
            case "-1":
                "275"
                break
            case "0":
                "175"
                break
            case "1":
                "60"
                break
            default:
                "275"
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
    selector "#txt_floor" + bms.eval("cur_floor").value
    style "font-weight", "bold"
    register(bms)
}

TransformerObserver.make {
    selector "#txt_cur_floor"
    set "text", { bms.eval("cur_floor").value }
    register(bms)
}

bms.registerMethod("openCloseDoor", {
    def t = bms.getTool().getTrace()
    def sId = t.getCurrentState()
    def statespace = bms.getTool().getStateSpace()
    def op = getOp(sId,statespace,"open_door","TRUE=TRUE") ?: getOp(sId,statespace,"close_door","TRUE=TRUE")
    if(op != null) {
        animations.traceChange(t.add(op.getId()))
        return [executedOperation:op.getName()]
    }
})

// --------------------------
// Helper methods
// --------------------------
def getOp(sId,statepsace,name,pred) {
    try {
        def ops = statepsace.opFromPredicate(sId,name,pred,1)
        ops[0]
    } catch (Exception e) {
        null
    }
}
