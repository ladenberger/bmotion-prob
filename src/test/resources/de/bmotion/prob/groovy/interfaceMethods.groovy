import de.prob.statespace.TraceElement;

def clz = bms.getClass();

// General BMotion API methods
clz.getMethod("log", Object.class);
clz.getMethod("executeEvent", String.class);
clz.getMethod("executeEvent", String.class, Map.class);
clz.getMethod("eval", String.class);
clz.getMethod("eval", String.class, Map.class);
clz.getMethod("registerMethod", String.class, Closure.class);
clz.getMethod("callMethod", String.class, Object[].class);
clz.getMethod("getMethods");
clz.getMethod("getSessionData");
clz.getMethod("getToolData");

// ProB specific API methods
clz.getMethod("getModel");
clz.getMethod("getTrace");
clz.getMethod("getAnimationSelector");
clz.getMethod("getHistory", TraceElement.class);
