require.config({
    paths: {
        'bmotion': '/bms/libs/bmotion/bmotion'
    }
});
define(['require','bmotion'], function(require,bmotion) {

    $("#bmotion-label").html("BMotion Studio for ProB")

    bmotion.socket.on('initProB', function(data) {

        $(function(){

            // TODO: Is there a better way to append a large amount of html code?
            $("body").append('<div data-view="Events" title="Events"><iframe src="" frameBorder="0"></iframe></div>'+
            '<div data-view="CurrentTrace" title="History"><iframe src="" frameBorder="0"></iframe></div>'+
            '<div data-view="StateInspector" title="State"><iframe src="" frameBorder="0"></iframe></div>'+
            '<div data-view="CurrentAnimations" title="Animations"><iframe src="" frameBorder="0"></iframe></div>'+
            '<div data-view="Log" title="Log"><iframe src="" frameBorder="0"></iframe></div>'+
            '<div data-view="ModelCheckingUI" title="ModelChecking"><iframe src="" frameBorder="0"></iframe></div>'+
            '<div data-view="GroovyConsoleSession" title="Console"><iframe src="" frameBorder="0"></iframe></div>')

            $("#bmotion-navigation").append('<li class="dropdown">'+
            '                        <a href="#" class="dropdown-toggle" data-toggle="dropdown">Open View <span class="caret"></span></a>'+
            '                        <ul class="dropdown-menu" role="menu">'+
            '                            <li><a href="#" id="bt_open_CurrentTrace"><i class="glyphicon glyphicon-indent-left"></i> History</a></li>'+
            '                            <li><a href="#" id="bt_open_Events"><i class="glyphicon glyphicon-align-left"></i> Events</a></li>'+
            '                            <li><a href="#" id="bt_open_StateInspector"><i class="glyphicon glyphicon-list-alt"></i> State</a></li>'+
            '                            <li><a href="#" id="bt_open_CurrentAnimations"><i class="glyphicon glyphicon-th-list"></i> Animations</a></li>'+
            '                            <li><a href="#" id="bt_open_Log"><i class="glyphicon glyphicon-file"></i> Log</a></li>'+
            '                            <li><a href="#" id="bt_open_GroovyConsoleSession"><i class="glyphicon glyphicon-phone"></i> Console</a></li>'+
            '                            <li><a href="#" id="bt_open_ModelCheckingUI"><i class="glyphicon glyphicon-ok"></i> Model Checking</a></li>'+
            '                         </ul>'+
            '                    </li>')

            // Initialise dialogs
            $.each($("div[data-view]"), function( i, e ) {
              var el = $(e)
              var viewtype = el.attr("data-view")
              var iframe = el.find("iframe")
              var aopen = $.cookie("open_"+viewtype) === undefined ? false : $.cookie("open_"+viewtype)
              el.dialog({
                  dragStart: function( event, ui) {
                      iframe.hide();
                  },
                  dragStop: function( event, ui) {
                     iframe.show();
                     $.cookie("position_top_"+viewtype, ui.position.top)
                     $.cookie("position_left_"+viewtype, ui.position.left)
                  },
                  resize: function() {
                      iframe.hide();
                  },
                  resizeStart: function() {
                      iframe.hide();
                  },
                  resizeStop: function(ev, ui){
                      iframe.show();
                      fixSizeDialog(el,iframe,0,0);
                  },
                  open: function(ev, ui){
                      $.cookie("open_"+viewtype, true);
                      iframe.attr("src","http://"+data.host+":"+data.port+"/sessions/"+viewtype);
                      fixSizeDialog(el,iframe,0,0);
                      el.css('overflow', 'hidden'); //this line does the actual hiding
                      var toppos = $.cookie("position_top_"+viewtype)
                      var leftpos = $.cookie("position_left_"+viewtype)
                      if(toppos !== undefined && leftpos !== undefined) {
                        el.parent().css("top",toppos+"px").css("left",leftpos+"px")
                      }
                  },
                  close: function(ev, ui){
                      $.removeCookie("open_"+viewtype);
                      $.removeCookie("position_top"+viewtype);
                      $.removeCookie("position_left_"+viewtype);
                   },
                  autoOpen: aopen,
                  width: 350,
                  height: 400
              });
              $("#bt_open_"+viewtype).click(function() {
                el.dialog( "open" );
              });
            });

        });

    });

});

function fixSizeDialog(dialog,obj,ox,oy) {
	var newwidth = dialog.parent().width() - ox
	var newheight = dialog.parent().height() - oy
	obj.attr("style","width:"+(newwidth)+"px;height:"+(newheight-50)+"px");
}
