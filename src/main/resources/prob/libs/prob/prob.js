define(['ngProB', 'bms', 'angularAMD', 'jquery', 'tooltipster', 'css!prob-css', 'css!tooltipster-css', 'css!tooltipster-shadow-css'], function (ngProB, bms, angularAMD) {

    var observers = {};
    var formulaObservers = {};

    bms.socket.on('checkObserver', function (trigger) {

        if (observers[trigger] !== undefined) {
            $.each(observers[trigger], function (i, v) {
                v.call(this)
            });
        }

        if (formulaObservers[trigger] !== undefined) {
            bms.socket.emit("observe", {data: formulaObservers[trigger]}, function (data) {
                $.each(formulaObservers[trigger], function (i, v) {
                    v.observer.call(this, data[i])
                });
            });
        }

    });

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

    var addObserver = function (cause, observer) {
        if (observers[cause] === undefined) observers[cause] = [];
        observers[cause].push(observer)
    };

    var addFormulaObserver = function (cause, settings, observer) {
        if (formulaObservers[cause] === undefined) formulaObservers[cause] = {};
        settings.observer = observer;
        formulaObservers[cause][guid()] = settings
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
        addObserver(settings.cause, function () {
            bms.socket.emit("eval", {data: {formula: settings.predicate}}, function (data) {
                if (data.value === "TRUE") {
                    observePredicateHandler(settings.true, $(origin), data.value)
                } else if (data.value === "FALSE") {
                    observePredicateHandler(settings.false, $(origin), data.value)
                }
                origin !== undefined ? settings.callback.call(this, $(origin), data) : settings.callback.call(this, data)
            });
        });
    };

    var removeBlanks = function (context, canvas, imgWidth, imgHeight) {

        var imageData = context.getImageData(0, 0, imgWidth, imgHeight),
            data = imageData.data,
            getRBG = function (x, y) {
                var offset = imgWidth * y + x;
                return {
                    red: data[offset * 4],
                    green: data[offset * 4 + 1],
                    blue: data[offset * 4 + 2],
                    opacity: data[offset * 4 + 3]
                };
            },
            isWhite = function (rgb) {
                // many images contain noise, as the white is not a pure #fff white
                return rgb.red > 200 && rgb.green > 200 && rgb.blue > 200;
            },
            scanY = function (fromTop) {
                var offset = fromTop ? 1 : -1;

                // loop through each row
                for (var y = fromTop ? 0 : imgHeight - 1; fromTop ? (y < imgHeight) : (y > -1); y += offset) {

                    // loop through each column
                    for (var x = 0; x < imgWidth; x++) {
                        var rgb = getRBG(x, y);
                        if (!isWhite(rgb)) {
                            return y;
                        }
                    }
                }
                return null; // all image is white
            },
            scanX = function (fromLeft) {
                var offset = fromLeft ? 1 : -1;

                // loop through each column
                for (var x = fromLeft ? 0 : imgWidth - 1; fromLeft ? (x < imgWidth) : (x > -1); x += offset) {

                    // loop through each row
                    for (var y = 0; y < imgHeight; y++) {
                        var rgb = getRBG(x, y);
                        if (!isWhite(rgb)) {
                            return x;
                        }
                    }
                }
                return null; // all image is white
            };

        var cropTop = scanY(true),
            cropBottom = scanY(false),
            cropLeft = scanX(true),
            cropRight = scanX(false),
            cropWidth = cropRight - cropLeft,
            cropHeight = cropBottom - cropTop;

        var $croppedCanvas = $("<canvas>").attr({width: cropWidth, height: cropHeight});
        $croppedCanvas[0].getContext("2d").drawImage(canvas,
            cropLeft, cropTop, cropWidth, cropHeight,
            0, 0, cropWidth, cropHeight);

        return $croppedCanvas[0];

    };

    var _createDiagram = function (data, origin) {

        console.log(data)

        if (origin === undefined) {
            // TODO: return some useful error message
            console.error("No element defined for custom transition diagram!")
        } else {
            $(function () { // on dom ready

                var cy = cytoscape({

                    container: origin,
                    style: cytoscape.stylesheet()
                        .selector('node')
                        .css({
                            'shape': 'rectangle',
                            'width': 'data(width)',
                            'height': 'data(height)',
                            'content': 'data(labels)',
                            'background-color': 'white',
                            'border-width': 2,
                            'border-color': 'data(color)',
                            'font-size': '11px',
                            'text-valign': 'top',
                            'text-halign': 'center',
                            'background-repeat': 'no-repeat',
                            'background-image': 'data(svg)',
                            'background-fit': 'none',
                            'background-position-x': '15px',
                            'background-position-y': '15px'
                        })
                        .selector('edge')
                        .css({
                            'content': 'data(label)',
                            'target-arrow-shape': 'triangle',
                            'width': 1,
                            'line-color': 'data(color)',
                            'line-style': 'data(style)',
                            'target-arrow-color': 'data(color)',
                            'font-size': '11px',
                            'control-point-distance': 60
                        }),
                    layout: {
                        name: 'cose',
                        animate: false,
                        fit: true,
                        padding: 25,
                        directed: true,
                        roots: '#1',
                        //nodeOverlap: 100, // Node repulsion (overlapping) multiplier
                        nodeRepulsion: 3000000 // Node repulsion (non overlapping) multiplier
                    },
                    elements: {
                        nodes: data.nodes,
                        edges: data.edges
                    }

                });
                $(origin).data("cy", cy);
            }); // on dom ready
        }

    };

    var _loadImage2 = function (property, felements, mcanvas, mcontext, v) {

        var deferred = $.Deferred();

        // Prepare data
        var ele = felements[property].clone;
        var count = felements[property].count;
        var ffval = [];
        $.each(count, function (i2, v2) {
            var trans = v.data.translated[0];
            if (trans != null) {
                ffval.push(trans[v2]);
            }
        });

        var image = new Image(),
            canvas = document.createElement('canvas'),
            context;
        image.crossOrigin = "anonymous";

        // Trigger trigger function that modifies element according to the attached observer
        ele.trigger('trigger', [ffval]);

        // Build image
        // TODO: Get correct initial width and height
        canvas.width = 1000;
        canvas.height = 1000;
        context = canvas.getContext("2d");

        image.onload = function () {
            if (context) {
                context.drawImage(this, 0, 0, this.width, this.height);
                var croppedCanvas = removeBlanks(context, canvas, this.width, this.height);
                v.data["canvas"].push(croppedCanvas);
                deferred.resolve();
            } else {
                // alert('Get a real browser!');
            }
        };

        if (ele.prop("tagName") === 'image') {
            image.src = ele.attr('xlink:href');
        }
        else {
            var html = $('<div>').append(ele).html();
            image.src = 'data:image/svg+xml;base64,' + window.btoa('<svg xmlns="http://www.w3.org/2000/svg" style="background-color:white" xmlns:xlink="http://www.w3.org/1999/xlink" width="1000" height="1000">' + html + '</svg>');
        }

        return deferred.promise();

    };

    var _loadImage = function (v, felements) {

        var deferred = $.Deferred();

        // Set default width and height of node
        v.data['width'] = 0;
        v.data['height'] = 0;

        // Create a new image for the node
        var mcanvas = document.createElement('canvas'),
            mcontext = mcanvas.getContext("2d"),
            val = v.data.labels[0];

        if (val !== "<< undefined >>") {

            v.data["canvas"] = [];

            var loaders = [];
            for (var property in felements) {
                loaders.push(_loadImage2(property, felements, mcanvas, mcontext, v));
            }

            $.when.apply(null, loaders).done(function () {

                var fwidth = 0;
                var fheight = 0;
                var yoffset = 0;
                $.each(v.data.canvas, function (i, v) {
                    fwidth = fwidth < v.width ? v.width : fwidth;
                    fheight = v.height + fheight + 15;
                });
                mcanvas.width = fwidth;
                mcanvas.height = fheight;
                $.each(v.data.canvas, function (i, v) {
                    mcontext.drawImage(v, 0, yoffset);
                    yoffset = v.height + yoffset + 15;
                });
                v.data['width'] = fwidth + 30;
                v.data['height'] = yoffset + 15;
                v.data['svg'] = mcanvas.toDataURL('image/png');
                deferred.resolve();

            });

        }
        else {
            v.data['svg'] = 'data:image/svg+xml;base64,' + window.btoa('<svg xmlns="http://www.w3.org/2000/svg" xmlns:svg="http://www.w3.org/2000/svg"></svg>');
        }

        return deferred.promise();

    };

    var _createTransitionDiagram = function (options, origin) {

        var settings = normalize($.extend({
            elements: []
        }, options), [], origin);

        var formulas = [];
        var felements = {};
        var count = 0;
        $.each(settings.elements, function (i, v) {
            felements[v] = {
                count: [],
                clone: $(v).clone(true)
            };
            $.each($(v).data("formulas"), function () {
                felements[v]['count'].push(count);
                count++;
            });
            formulas = formulas.concat($(v).data("formulas"));
        });

        bms.socket.emit('createCustomTransitionDiagram', {
            data: {expressions: formulas}
        }, function (data) {
            var loaders = [];
            $.each(data.nodes, function (i, v) {
                loaders.push(_loadImage(v, felements));
            });
            $.when.apply(null, loaders).done(function () {
                _createDiagram(data, origin);
            });
        });

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

            addObserver(settings.cause, function () {
                bms.socket.emit("observeCSPTrace", {data: settings}, function (data) {
                    var scope = angular.element(element).scope();
                    scope.$apply(function () {
                        scope.setOrder(data.order);
                        scope.setValues(data.values);
                    });
                });
            });

        }

    };

    var observeRefinement = function (options, origin) {
        var settings = normalize($.extend({
            refinements: [],
            enable: function () {
            },
            disable: function () {
            }
        }, options), ["enable", "disable"], origin);
        addObserver("ModelChanged", function () {
            bms.socket.emit("observeRefinement", {data: settings}, function (data) {
                $.each(settings.refinements, function (i, v) {
                    if ($.inArray(v, data.refinements) > -1) {
                        origin !== undefined ? settings.enable.call(this, $(origin), data) : settings.enable.call(this, data)
                    } else {
                        origin !== undefined ? settings.disable.call(this, $(origin), data) : settings.disable.call(this, data)
                    }
                });
            });
        });
    };

    var observeMethod = function (options, origin) {
        var settings = normalize($.extend({
            name: "",
            cause: "AnimationChanged",
            trigger: function () {
            }
        }, options), ["trigger"], origin);
        addObserver(settings.cause, function () {
            bms.socket.emit("callMethod", {data: settings}, function (data) {
                origin !== undefined ? settings.trigger.call(this, $(origin), data) : settings.trigger.call(this, data)
            });
        });
        return settings
    };

    var observeFormulas = function (options, origin) {

        var settings = normalize($.extend({
            formulas: [],
            cause: "AnimationChanged",
            trigger: function () {
            }
        }, options), ["trigger"], origin);

        if (origin != null) {
            $(origin).attr("data-formulaobserver", "");
            $(origin).on("trigger", function (event, data) {
                settings.trigger.call(this, $(this), data)
            });
        }

        addFormulaObserver(settings.cause, settings, function (data) {
            origin !== undefined ? settings.trigger.call(this, $(origin), data) : settings.trigger.call(this, data)
        });

    };

    //var oldObserveFn = bms.observe;
    //var probObserveFn = function (what, options, origin) {
    var observe = function (what, options, origin) {
        //oldObserveFn(what, options, origin);
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
    //bms.observe = probObserveFn;

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

        /*$.fn.observe = function (what, options) {
         this.each(function (i, v) {
         probObserveFn(what, options, v);
         });
         return this
         };*/

        $.fn.createTransitionDiagram = function (options) {
            this.each(function (i, v) {
                _createTransitionDiagram(options, v);
            });
            return this
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
        ng: angularAMD.bootstrap(ngProB)
    }, bms);

});
