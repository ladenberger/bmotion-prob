define(['ngProB', 'bms', 'angularAMD', 'jquery', 'tooltipster', 'css!prob-css', 'css!tooltipster-css', 'css!tooltipster-shadow-css'], function (ngProB, bms, angularAMD) {

    var observePredicateHandler = function (tf, el, data) {
        if (Object.prototype.toString.call(tf) === '[object Array]') {
            $.each(tf, function (i, v) {
                el.attr(v.attr, v.value)
            });
        } else if (isFunction(tf)) {
            if (el === undefined) {
                tf.call(this, data)
            } else {
                el.each(function (i, v) {
                    tf.call(this, $(v), data)
                });
            }
        }
    };

    var observePredicate = function (options, origin) {
        var settings = normalize($.extend({
            predicate: "",
            selector: null,
            true: [],
            false: [],
            cause: "AnimationChanged",
            callback: function () {
            }
        }, options), ["callback", "false", "true"], origin);
        bms.addObserver(settings.cause, function () {
            bms.socket.emit("eval", {data: {formula: settings.predicate}}, function (data) {
                var el = settings.selector !== null ? $(settings.selector) : origin;
                if (data.value === "TRUE") {
                    observePredicateHandler(settings.true, el, data.value)
                } else if (data.value === "FALSE") {
                    observePredicateHandler(settings.false, el, data.value)
                }
                if (el === undefined) {
                    settings.callback.call(this, data)
                } else {
                    el.each(function (i, v) {
                        settings.callback.call(this, $(v), data)
                    });
                }
            });
        });
    };

    var observeRefinement = function (options, origin) {
        var settings = normalize($.extend({
            refinements: [],
            enable: function () {
            },
            disable: function () {
            }
        }, options), ["enable", "disable"], origin);
        bms.addObserver("checkObserver_ModelChanged", function () {
            bms.socket.emit("observeRefinement", {data: settings}, function (data) {
                $.each(settings.refinements, function (i, v) {
                    if ($.inArray(v, data.refinements) > -1) {
                        origin !== undefined ? settings.enable.call(this, origin, data) : settings.enable.call(this, data)
                    } else {
                        origin !== undefined ? settings.disable.call(this, origin, data) : settings.disable.call(this, data)
                    }
                });
            });
        });
    };

    var oldObserveFn = bms.observe
    var probObserveFn = function (what, options, origin) {
        oldObserveFn(what, options, origin);
        if (what === "refinement") {
            return observeRefinement(options, origin)
        }
        if (what === "predicate") {
            return observePredicate(options, origin)
        }
    };
    bms.observe = probObserveFn

        // ---------------------
        // jQuery extension
        // ---------------------
    (function ($) {

        $.fn.observe = function (what, options) {
            probObserveFn(what, options, this);
            return this
        }

        $.fn.executeEvent = function (options) {
            var settings = $.extend({
                events: [],
                tooltip: true,
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
                bms.executeEvent(options, $(e.target))
            }).css('cursor', 'pointer')
            if (settings.tooltip) {
                this.tooltipster({
                    position: "top-left",
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
                        bms.socket.emit('initTooltip', {
                            data: normalize(settings, ["callback"], origin)
                        }, function (data) {

                            var container = $('<ul></ul>')
                            $.each(data.events, function (i, v) {
                                var spanClass = v.canExecute ? 'glyphicon glyphicon-ok-circle' : 'glyphicon glyphicon-remove-circle'
                                var span = $('<span aria-hidden="true"></span>').addClass(spanClass)
                                var link = $('<span> ' + v.name + ' ' + v.predicate + '</span>')
                                if (v.canExecute) {
                                    link = $('<a href="#"> ' + v.name + '(' + v.predicate + ')</a>').click(function () {
                                        bms.executeEvent({
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
            }
            return this
        }

    }(jQuery));

    return $.extend({
        observe: probObserveFn,
        ng: angularAMD.bootstrap(ngProB)
    }, bms);

});
