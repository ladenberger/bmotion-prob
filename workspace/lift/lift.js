require.config({
    paths: {
        'prob': '/bms/libs/prob/prob'
    }
});
define(["require","prob"], function(require) {
    var bms = require('bmotion')
    $("#door").click(function() {
     bms.callMethod("openCloseDoor", {
         success : function(data) {
             alert("New state id: " + data.newState)
         }
     });
    });
});
