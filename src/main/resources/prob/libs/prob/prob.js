require.config({
    map: {
        '*': {
            'css': '/bms/libs/require-css/css.min.js'
        }
    },
    paths: {
        'bmotion': '/bms/libs/bmotion/bmotion',
        'prob-css': '/bms/libs/prob/prob'
    }
});
define(['require', 'bmotion', 'css!prob-css'], function (require, bmotion) {

    // --------------------- Extend jQuery
    $.extend(bmotion, {
        observeRefinement: function (options, origin) {
            var settings = normalize($.extend({
                refinements: [],
                enable: function () {
                },
                disable: function () {
                }
            }, options), ["enable", "disable"], origin);
            $(document).bind("checkObserver_ModelChanged", function () {
                bmotion.socket.emit("observeRefinement", {data: settings}, function (data) {
                    $.each(settings.refinements, function (i, v) {
                        if ($.inArray(v, data.refinements) > -1) {
                            origin !== undefined ? settings.enable.call(this, origin, data) : settings.enable.call(this, data)
                        } else {
                            origin !== undefined ? settings.disable.call(this, origin, data) : settings.disable.call(this, data)
                        }
                    });
                });
            });
        }
    });

    $.fn.observeRefinement = function (options) {
        bmotion.observeRefinement(options, this)
    }

    $.fn.executeEvent = function (options) {
        var settings = $.extend({
            events: [],
            callback: function () {
            }
        }, options)
        var obj = this
        $(document).bind("eventHighlight", function () {
            var offset = obj.offset();
            var width = obj[0].getBoundingClientRect().width
            var height = obj[0].getBoundingClientRect().height
            var max = Math.max(width, height)
            var centerX = width > max ? offset.left : offset.left - (max - width) / 2
            var centerY = height > max ? offset.top : offset.top - (max - height) / 2
            var d = $('<div class="overlay" style="width:' + max + 'px;height:' + max + 'px;top:' + centerY + 'px;left:' + centerX + 'px"></div>')
            $('body').append(d)
        });
        this.click(function (e) {
            bmotion.executeEvent(options, e.target)
        }).css('cursor', 'pointer')
        this.tooltipster({
            position: "bottom-right",
            animation: "fade",
            hideOnClick: true,
            updateAnimation: false,
            offsetY: 15,
            delay: 500,
            content: 'Loading...',
            theme: 'tooltipster-shadow',
            interactive: true,
            functionBefore: function (origin, continueTooltip) {

                continueTooltip();
                bmotion.socket.emit('initTooltip', {
                    data: normalize(settings, ["callback"], origin)
                }, function (data) {

                    var container = $('<ul></ul>')
                    $.each(data.events, function (i, v) {
                        var spanClass = v.canExecute ? 'glyphicon glyphicon-ok-circle' : 'glyphicon glyphicon-remove-circle'
                        var span = $('<span aria-hidden="true"></span>').addClass(spanClass)
                        var link = $('<span> ' + v.name + ' ' + v.predicate + '</span>')
                        if (v.canExecute) {
                            link = $('<a href="#"> ' + v.name + '(' + v.predicate + ')</a>').click(function () {
                                bmotion.executeEvent({
                                    events: [{name: v.name, predicate: v.predicate}],
                                    callback: function () {
                                        // Update tooltip
                                        origin.tooltipster('hide')
                                        origin.tooltipster('show')
                                    }
                                })
                            });
                        }
                        container.append($('<li></li>').addClass(v.canExecute ? 'enabled' : 'disabled').append(span, link))
                    });
                    origin.tooltipster('content', container)

                });

            }
        });
        return this
    }
    // ---------------------

    bmotion.socket.on('initialisation', function () {

        bmotion.socket.emit('initProB', function (data) {

            $(function () {

                $("#bmotion-label").html("BMotion Studio for ProB")

                // TODO: Is there a better way to append a large amount of html code?
                $("body").append('<div data-view="Events" title="Events"><iframe src="" frameBorder="0"></iframe></div>' +
                '<div data-view="CurrentTrace" title="History"><iframe src="" frameBorder="0"></iframe></div>' +
                '<div data-view="StateInspector" title="State"><iframe src="" frameBorder="0"></iframe></div>' +
                '<div data-view="CurrentAnimations" title="Animations"><iframe src="" frameBorder="0"></iframe></div>' +
                '<div data-view="Log" title="Log"><iframe src="" frameBorder="0"></iframe></div>' +
                '<div data-view="ModelCheckingUI" title="ModelChecking"><iframe src="" frameBorder="0"></iframe></div>' +
                '<div data-view="GroovyConsoleSession" title="Console"><iframe src="" frameBorder="0"></iframe></div>')

                $("#bmotion-navigation-model").append('<li><a href="#" id="bt_show_event"> Show Click Handler</a></li>')

                $("body").prepend('<div id="fade-wrapper"></div>')

                $("#bmotion-navigation").append('<li class="dropdown">' +
                '                        <a href="#" class="dropdown-toggle" data-toggle="dropdown">Open View <span class="caret"></span></a>' +
                '                        <ul class="dropdown-menu" role="menu">' +
                '                            <li><a href="#" id="bt_open_CurrentTrace"><i class="glyphicon glyphicon-indent-left"></i> History</a></li>' +
                '                            <li><a href="#" id="bt_open_Events"><i class="glyphicon glyphicon-align-left"></i> Events</a></li>' +
                '                            <li><a href="#" id="bt_open_StateInspector"><i class="glyphicon glyphicon-list-alt"></i> State</a></li>' +
                '                            <li><a href="#" id="bt_open_CurrentAnimations"><i class="glyphicon glyphicon-th-list"></i> Animations</a></li>' +
                '                            <li><a href="#" id="bt_open_Log"><i class="glyphicon glyphicon-file"></i> Log</a></li>' +
                '                            <li><a href="#" id="bt_open_GroovyConsoleSession"><i class="glyphicon glyphicon-phone"></i> Console</a></li>' +
                '                            <li><a href="#" id="bt_open_ModelCheckingUI"><i class="glyphicon glyphicon-ok"></i> Model Checking</a></li>' +
                '                         </ul>' +
                '                    </li>')

                var remove = false;
                $("#bt_show_event").click(function () {

                    if (!remove) {

                        //bmotion.socket.emit('getEnabledEvents', function (data) {
                        //    console.log(data)

                        $('#fade-wrapper').fadeIn();
                        $(document).trigger({
                            type: "eventHighlight"
                        });

                        //});
                        remove = true

                    } else {
                        $(".overlay").remove()
                        $('#fade-wrapper').fadeOut();
                        remove = false
                    }

                })

                $('#fade-wrapper').click(function () {
                    $(".overlay").remove()
                    $(this).fadeOut();
                    remove = false
                });

                // Initialise dialogs
                $.each($("div[data-view]"), function (i, e) {
                    var el = $(e)
                    var viewtype = el.attr("data-view")
                    var iframe = el.find("iframe")
                    var aopen = $.cookie("open_" + viewtype) === undefined ? false : $.cookie("open_" + viewtype)
                    el.dialog({
                        dragStart: function (event, ui) {
                            iframe.hide();
                        },
                        dragStop: function (event, ui) {
                            iframe.show();
                            $.cookie("position_top_" + viewtype, ui.position.top)
                            $.cookie("position_left_" + viewtype, ui.position.left)
                        },
                        resize: function () {
                            iframe.hide();
                        },
                        resizeStart: function () {
                            iframe.hide();
                        },
                        resizeStop: function (ev, ui) {
                            iframe.show();
                            fixSizeDialog(el, iframe, 0, 0);
                        },
                        open: function (ev, ui) {
                            $.cookie("open_" + viewtype, true);
                            iframe.attr("src", "http://" + data.host + ":" + data.port + "/sessions/" + viewtype);
                            fixSizeDialog(el, iframe, 0, 0);
                            el.css('overflow', 'hidden'); //this line does the actual hiding
                            var toppos = $.cookie("position_top_" + viewtype)
                            var leftpos = $.cookie("position_left_" + viewtype)
                            if (toppos !== undefined && leftpos !== undefined) {
                                el.parent().css("top", toppos + "px").css("left", leftpos + "px")
                            }
                        },
                        close: function (ev, ui) {
                            $.removeCookie("open_" + viewtype);
                            $.removeCookie("position_top" + viewtype);
                            $.removeCookie("position_left_" + viewtype);
                        },
                        autoOpen: aopen,
                        width: 350,
                        height: 400
                    });
                    $("#bt_open_" + viewtype).click(function () {
                        el.dialog("open");
                    });
                });

            });

        });
    });

    bmotion.socket.on('applyTransformersCSP', function (data) {
        var d1 = JSON.parse(data)
        var i1 = 0
        for (; i1 < d1.length; i1++) {
            var t = d1[i1]
            if (t.selector) {
                var selector = $(t.selector)
                var content = t.content
                if (content != undefined) selector.html(content)
                $.each(selector, function (index, e) {
                    var elem = $(e)
                    for (var property in t.attributes) {
                        if (elem.data(property) === undefined) {
                            var oldval = elem.attr(property)
                            if (oldval === undefined)
                                oldval = "UNAVAILABLE"
                            elem.data(property, oldval)
                        }
                    }
                    elem.attr("data-bms-csp", true)
                });
                selector.attr(t.attributes)
                selector.css(t.styles)

            }
        }
    });

    bmotion.socket.on('revertCSP', function () {

        var revertElemens = $("[data-bms-csp]")
        var i1 = 0
        for (; i1 < revertElemens.length; i1++) {

            var el = $(revertElemens[i1])
            var data = el.data()
            for (var property in data) {
                if (data[property] === "UNAVAILABLE") {
                    el.removeAttr(property)
                } else {
                    el.attr(property, data[property])
                }
            }


        }

    });

})
;

function fixSizeDialog(dialog, obj, ox, oy) {
    var newwidth = dialog.parent().width() - ox
    var newheight = dialog.parent().height() - oy
    obj.attr("style", "width:" + (newwidth) + "px;height:" + (newheight - 50) + "px");
}
