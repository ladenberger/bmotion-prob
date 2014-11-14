require.config({
    paths: {
        'prob': '/bms/libs/prob/prob'
    }
});
define(["require","prob"], function(require) {

    var bms = require('bmotion')

    $(function() {
       $("#door").click(function() {
         bms.callMethod("openCloseDoor", {
             success : function(data) {
                 console.log(data)
             }
         });
       });
     });

});
