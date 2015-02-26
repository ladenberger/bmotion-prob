define(['bms', 'jquery', 'tooltipster', 'css!tooltipster-css', 'css!tooltipster-shadow-css'], function (bms) {

    bms.socket.on('checkObserver', function (trigger) {
        checkObserver({
            trigger: trigger
        });
    });

    var checkObserver = function (options) {

        var deferred = $.Deferred();

        var settings = $.extend({
            trigger: "AnimationChanged",
            parent: $('body'),
            stateId: null
        }, options);

        var formulaObservers = {};
        var formulaElements = {};
        var elementsWithObservers = settings.parent.find('[data-hasobserver]');
        elementsWithObservers.each(function (i, ele) {

            var observers = $(ele).data('observer')[settings.trigger];
            for (var property in observers) {
                if (observers.hasOwnProperty(property)) {
                    if (property !== 'formula') {
                        $.each(observers[property], function (i, v) {
                            v.caller.call(this);
                        });
                    } else {
                        $.each(observers[property], function (i, v) {
                            var id = guid();
                            formulaObservers[id] = v;
                            formulaElements[id] = ele;
                        });
                    }
                }
            }

        });

        // Execute formula observer at once (performance boost)
        bms.socket.emit("observe", {data: {formulas: formulaObservers, stateId: settings.stateId}}, function (data) {
            $.each(formulaObservers, function (i, v) {
                v.caller.call(this, data[i], formulaElements[i]);
            });
            deferred.resolve();
        });

        return deferred.promise();

    };

    bms.socket.on('applyTransformers', function (data) {
        var d1 = JSON.parse(data);
        var i1 = 0;
        for (; i1 < d1.length; i1++) {
            var t = d1[i1];
            if (t.selector) {
                var selector = $(t.selector);
                var content = t.content;
                if (content != undefined) selector.html(content);
                selector.attr(t.attributes);
                selector.css(t.styles)
            }
        }
    });

    var guid = (function () {
        function s4() {
            return Math.floor((1 + Math.random()) * 0x10000)
                .toString(16)
                .substring(1);
        }

        return function () {
            return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
                s4() + '-' + s4() + s4() + s4();
        };
    })();

    var addObserver = function (type, settings, caller, origin) {

        var ele = $(origin);
        ele.attr("data-hasobserver", "");
        var observer = ele.data("observer");
        if (observer === undefined) {
            observer = {};
            ele.data("observer", observer)
        }
        if (observer[settings.cause] === undefined) observer[settings.cause] = {};
        if (observer[settings.cause][type] === undefined) observer[settings.cause][type] = [];
        observer[settings.cause][type].push({
            observer: settings,
            caller: caller
        });
        ele.data("clone", ele.clone(true));

    };

    var executeEvent = function (options, origin) {
        var settings = normalize($.extend({
            events: [],
            callback: function () {
            }
        }, options), ["callback"], origin);
        bms.socket.emit("executeEvent", {data: normalize(settings, ["callback"], origin)}, function (data) {
            origin !== undefined ? settings.callback.call(this, origin, data) : settings.callback.call(this, data)
        });
        return settings
    };

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
            true: [],
            false: [],
            cause: "AnimationChanged",
            callback: function () {
            }
        }, options), ["callback", "false", "true"], origin);
        addObserver("predicate", settings, function () {
            bms.socket.emit("eval", {data: {formula: settings.predicate}}, function (data) {
                if (data.value === "TRUE") {
                    observePredicateHandler(settings.true, $(origin), data.value)
                } else if (data.value === "FALSE") {
                    observePredicateHandler(settings.false, $(origin), data.value)
                }
                origin !== undefined ? settings.callback.call(this, $(origin), data) : settings.callback.call(this, data)
            });
        }, origin);
    };

    var observeCSPTrace = function (options, origin) {

        var settings = normalize($.extend({
            observers: [],
            selector: "",
            cause: "AnimationChanged"
        }, options), [], origin);

        var element = origin !== undefined ? origin : $(settings.selector);
        if (element !== undefined) {

            $(element).attr("bms-visualisation", "");
            var $injector = angular.injector(['ng', 'bmsModule']);
            $injector.invoke(function ($rootScope, $compile) {
                $compile(element)($rootScope);
            });

            addObserver("csp-event", settings, function () {
                bms.socket.emit("observeCSPTrace", {data: settings}, function (data) {
                    var scope = angular.element(element).scope();
                    scope.$apply(function () {
                        scope.setOrder(data.order);
                        scope.setValues(data.values);
                    });
                });
            }, origin);

        }

    };

    var observeRefinement = function (options, origin) {
        var settings = normalize($.extend({
            refinements: [],
            cause: "ModelChanged",
            enable: function () {
            },
            disable: function () {
            }
        }, options), ["enable", "disable"], origin);
        addObserver("refinement", settings, function () {
            bms.socket.emit("observeRefinement", {data: settings}, function (data) {
                $.each(settings.refinements, function (i, v) {
                    if ($.inArray(v, data.refinements) > -1) {
                        origin !== undefined ? settings.enable.call(this, $(origin), data) : settings.enable.call(this, data)
                    } else {
                        origin !== undefined ? settings.disable.call(this, $(origin), data) : settings.disable.call(this, data)
                    }
                });
            });
        }, origin);
    };

    var observeMethod = function (options, origin) {
        var settings = normalize($.extend({
            name: "",
            cause: "AnimationChanged",
            trigger: function () {
            }
        }, options), ["trigger"], origin);
        addObserver("method", settings, function () {
            bms.socket.emit("callMethod", {data: settings}, function (data) {
                origin !== undefined ? settings.trigger.call(this, $(origin), data) : settings.trigger.call(this, data)
            });
        }, origin);
        return settings
    };

    var observeFormulas = function (options, origin) {
        var settings = normalize($.extend({
            formulas: [],
            cause: "AnimationChanged",
            trigger: function () {
            }
        }, options), ["trigger"], origin);
        addObserver("formula", settings, function (data, element) {
            settings.trigger.call(this, $(element), data);
        }, origin);
    };

    var observe = function (what, options, origin) {
        if (what === "formula") {
            observeFormulas(options, origin)
        }
        if (what === "method") {
            observeMethod(options, origin)
        }
        if (what === "refinement") {
            observeRefinement(options, origin)
        }
        if (what === "predicate") {
            observePredicate(options, origin)
        }
        if (what === "csp-event") {
            observeCSPTrace(options, origin)
        }
    };

    // ---------------------
    // jQuery extension
    // ---------------------
    (function ($) {

        $.fn.observe = function (what, options) {
            this.each(function (i, v) {
                observe(what, options, v);
            });
            return this
        };

        $.fn.executeEvent = function (options) {
            return this.click(function (e) {
                executeEvent(options, e.target)
            }).css('cursor', 'pointer')
        };

        $.fn.executeEvent = function (options) {
            var settings = $.extend({
                events: [],
                tooltip: true,
                callback: function () {
                }
            }, options);
            var obj = this;
            $(document).bind("eventHighlight", function () {
                var offset = obj.offset();
                var width = obj[0].getBoundingClientRect().width;
                var height = obj[0].getBoundingClientRect().height;
                var max = Math.max(width, height);
                var centerX = width > max ? offset.left : offset.left - (max - width) / 2;
                var centerY = height > max ? offset.top : offset.top - (max - height) / 2;
                var d = $('<div class="overlay" style="width:' + max + 'px;height:' + max + 'px;top:' + centerY + 'px;left:' + centerX + 'px"></div>');
                $('body').append(d)
            });
            this.click(function (e) {
                executeEvent(options, $(e.target))
            }).css('cursor', 'pointer');
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

                            var container = $('<ul></ul>');
                            $.each(data.events, function (i, v) {
                                var spanClass = v.canExecute ? 'glyphicon glyphicon-ok-circle' : 'glyphicon glyphicon-remove-circle'
                                var span = $('<span aria-hidden="true"></span>').addClass(spanClass);
                                var link = $('<span> ' + v.name + '(' + v.predicate + ')</span>');
                                if (v.canExecute) {
                                    link = $('<a href="#"> ' + v.name + '(' + v.predicate + ')</a>').click(function () {
                                        executeEvent({
                                            events: [{name: v.name, predicate: v.predicate}],
                                            callback: function () {
                                                // Update tooltip
                                                origin.tooltipster('hide');
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
        executeEvent: executeEvent,
        observeFormulas: observeFormulas,
        observeMethod: observeMethod,
        observe: observe,
        addObserver: addObserver,
        checkObserver: checkObserver
    }, bms);

});
