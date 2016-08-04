bms.registerMethod("someMethodWithoutParameter", {
	return "Call without parameter";
});

bms.registerMethod("someMethodWith1Parameter", { arg ->
	return "Call with one parameter " + arg;
});

bms.registerMethod("someMethodWith2Parameter", { arg1, arg2 ->
	return "Call with two parameters " + arg1 + " " + arg2;
});

bms.registerMethod("returnInteger", { 
	return 0;
});