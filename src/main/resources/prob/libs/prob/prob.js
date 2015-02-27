define(['probFunctions', 'angularAMD', '/bms/libs/bmotion/config.js', 'ngBMotion', 'jquery', 'jquery-cookie', 'jquery-ui', 'css!jquery-ui-css', 'css!jquery-ui-theme-css', 'xeditable', 'css!xeditable-css', 'cytoscape', 'css!prob-css'], function (probFunctions, angularAMD, config) {

        var probModule = angular.module('probModule', ['bmsModule', 'xeditable'])
            .run(["$rootScope", 'editableOptions', function ($rootScope, editableOptions) {
                $rootScope.formulaElements = [];
                $rootScope.loadElements = function () {
                    $rootScope.formulaElements = [];
                    $('[data-hasobserver]').each(function (i, v) {
                        var el = $(v);
                        var observer = el.data("observer")["AnimationChanged"];
                        if (observer["formula"]) {
                            if (el.parents('svg').length) {
                                var id = $(v).attr("id");
                                if (id !== undefined) {
                                    $rootScope.formulaElements.push({
                                        value: $rootScope.formulaElements.length + 1,
                                        text: '#' + id
                                    })
                                }
                            }
                        }
                    });
                };
                $rootScope.getFormulaElements = function () {
                    return $rootScope.formulaElements;
                };
                editableOptions.theme = 'bs3'; // bootstrap3 theme. Can be also 'bs2', 'default'
            }])
            .factory('initProB', ['$q', 'ws', function ($q, ws) {
                var defer = $q.defer();
                ws.emit('initProB', "", function (data) {
                    defer.resolve(data)
                });
                return defer.promise;
            }])
            .directive('bmsApp', ['$compile', 'initProB', 'initSession', function ($compile, initProB, initSession) {
                return {
                    priority: 2,
                    link: function ($scope, element) {

                        initSession.then(function (standalone) {
                            if (standalone) {
                                initProB.then(function (data) {
                                    //$scope.host = data.host;
                                    $scope.port = data.port;
                                    $scope.traceId = data.traceId;
                                    var bmsNavigation = angular.element('<prob-navigation></prob-navigation>');
                                    element.find("body").append($compile(bmsNavigation)($scope));
                                    var probViews = angular.element('<div ng-controller="bmsDialogCtrl">' +
                                    '<div bms-dialog type="CurrentTrace"><div prob-view></div></div>' +
                                    '<div bms-dialog type="Events"><div prob-view></div></div>' +
                                    '<div bms-dialog type="StateInspector"><div prob-view></div></div>' +
                                    '<div bms-dialog type="CurrentAnimations"><div prob-view></div></div>' +
                                    '<div bms-dialog type="GroovyConsoleSession"><div prob-view></div></div>' +
                                    '<div bms-dialog type="ModelCheckingUI"><div prob-view></div></div>' +
                                    '<div bms-dialog type="ElementProjection"><div diagram-element-projection-view></div></div>' +
                                    '<div bms-dialog type="TraceDiagram"><div diagram-trace-view></div></div>' +
                                    '</div>');
                                    element.find("body").append($compile(probViews)($scope))
                                })
                            }
                        })

                    }
                }
            }])
            .directive('probNavigation', ['ws', function (ws) {
                return {
                    restrict: 'E',
                    replace: true,
                    templateUrl: '/bms/libs/prob/probNavigation.html',
                    controller: function ($scope) {
                        $scope.openView = function (type) {
                            $scope.$broadcast('open' + type);
                        };
                        $scope.reloadModel = function () {
                            $scope.modal.setLabel("Reloading model ...");
                            $scope.modal.show();
                            ws.emit('reloadModel', "", function () {
                                $scope.modal.hide()
                            });
                        };
                    },
                    link: function ($scope, element, attrs) {

                        if (config.socket.host !== 'localhost') {
                            $(element).find('#bt_GroovyConsoleSession').css("display", "none");
                            $(element).find('#bt_ModelCheckingUI').css("display", "none");
                            $(element).find('#bt_CurrentAnimations').css("display", "none");
                        }

                    }
                }
            }])
            .factory('bmsDialogService', function () {
                return {
                    isOpen: function (type) {
                        return $.cookie("open_" + type) === undefined ? false : $.cookie("open_" + type);
                    },
                    open: function (element, type) {
                        $.cookie("open_" + type, true);
                        var toppos = $.cookie("position_top_" + type);
                        var leftpos = $.cookie("position_left_" + type);
                        var width = $.cookie("width_" + type);
                        var height = $.cookie("height_" + type);
                        if (toppos !== undefined && leftpos !== undefined) {
                            element.parent().css("top", toppos + "px").css("left", leftpos + "px")
                        }
                        if (width !== undefined && height !== undefined) {
                            element.parent().css("width", width + "px").css("height", height + "px")
                        }
                    },
                    close: function (type) {
                        $.removeCookie("open_" + type);
                        $.removeCookie("position_top" + type);
                        $.removeCookie("position_left_" + type);
                        $.removeCookie("width_" + type);
                        $.removeCookie("height_" + type);
                    },
                    dragStop: function (ui, type) {
                        $.cookie("position_top_" + type, ui.position.top);
                        $.cookie("position_left_" + type, ui.position.left)
                    },
                    resizeStop: function (ui, type) {
                        $.cookie("width_" + type, ui.size.width);
                        $.cookie("height_" + type, ui.size.height);
                    },
                    fixSize: function (dialog, ox, oy) {
                        var newwidth = dialog.parent().width() - ox;
                        var newheight = dialog.parent().height() - oy;
                        dialog.first().css("width", (newwidth) + "px").css("height", (newheight - 38) + "px");
                    }
                }
            })
            .factory('renderingService', function () {

                var renderingService = {

                    removeBlanks: function (context, canvas, imgWidth, imgHeight) {

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

                    },
                    getStyle: function (path) {
                        var deferred = $.Deferred();
                        $.when($.get(path)).done(function (response) {
                            deferred.resolve(response);
                        });
                        return deferred.promise();
                    },
                    getStyles: function () {
                        var deferred = $.Deferred();
                        var bmsStyles = $('head').find('[data-bms-style]');
                        var styleLoaders = [];
                        $.each(bmsStyles, function (i, v) {
                            var href = $(v).attr('href');
                            styleLoaders.push(renderingService.getStyle(href));
                        });
                        $.when.apply(null, styleLoaders).done(function () {
                            var styles = '';
                            $.each(arguments, function (i, css) {
                                styles = styles + '\n' + css;
                            });
                            deferred.resolve('<style type="text/css">\n<![CDATA[\n' + styles + '\n]]>\n</style>');
                        });
                        return deferred.promise();
                    }

                };

                return renderingService;

            })
            .controller('bmsDialogCtrl', ['$scope', function ($scope) {
                $scope.isOpen = false;
            }])
            .directive('probView', function () {
                return {
                    replace: true,
                    scope: true,
                    template: '<div style="width:100%;height:100%"><iframe src="" frameBorder="0" style="width:100%;height:100%"></iframe></div>',
                    link: function ($scope, element, attrs) {
                        var iframe = $(element).find("iframe");
                        $scope.$on('dragStart', function () {
                            iframe.hide();
                        });
                        $scope.$on('dragStop', function () {
                            iframe.show();
                        });
                        $scope.$on('resize', function () {
                            iframe.hide();
                        });
                        $scope.$on('resizeStart', function () {
                            iframe.hide();
                        });
                        $scope.$on('resizeStop', function () {
                            iframe.show();
                        });
                        $scope.$on('open', function () {
                            iframe.attr("src", document.location.protocol + '//' + document.location.hostname + ":" + $scope.port +
                            "/sessions/" + $scope.type + "/" + $scope.traceid);
                        });
                    }
                }
            })
            .directive('bmsDialog', ['bmsDialogService', function (bmsDialogService) {
                return {
                    scope: true,
                    link: function ($scope, element, attrs) {

                        $scope.type = attrs.type;
                        $scope.isOpen = bmsDialogService.isOpen($scope.type);
                        $scope.$on('open' + $scope.type, function () {
                            $(element).dialog("open");
                        });

                        $(element).first().css("overflow", "hidden");
                        $(element).dialog({

                            dragStart: function () {
                                $scope.$broadcast('dragStart');
                            },
                            dragStop: function (event, ui) {
                                bmsDialogService.dragStop(ui, $scope.type);
                                $scope.$broadcast('dragStop');
                            },
                            resize: function () {
                                $scope.$broadcast('resize');
                            },
                            resizeStart: function () {
                                $scope.$broadcast('resizeStart');
                            },
                            resizeStop: function (event, ui) {
                                bmsDialogService.resizeStop(ui, $scope.type);
                                bmsDialogService.fixSize($(element), 0, 0);
                                $scope.$broadcast('resizeStop');
                            },
                            open: function () {
                                bmsDialogService.open(element, $scope.type);
                                bmsDialogService.fixSize($(element), 0, 0);
                                $scope.isOpen = true;
                                $scope.$broadcast('open');
                            },
                            close: function () {
                                bmsDialogService.close($scope.type);
                                $scope.isOpen = false;
                                $scope.$broadcast('close');
                            },
                            autoOpen: $scope.isOpen,
                            width: 350,
                            height: 400,
                            title: $scope.type

                        });

                    }
                }
            }])
            .factory('diagramElementProjectionGraph', ['$q', 'ws', 'renderingService', function ($q, ws, renderingService) {

                var cy;

                var _loadImage2 = function (property, felements, mcanvas, mcontext, v, styleTag) {

                    var deferred = $.Deferred();

                    // Prepare data
                    var ele = felements[property].clone;
                    var type = felements[property].type;
                    var ffval = [];

                    $.each(felements[property].count, function (i2, v2) {
                        var trans = v.data.translated;
                        var res = v.data.results;
                        var fres = felements[property].translate ? trans[v2] : res[v2];
                        if (fres !== undefined) {
                            ffval.push(fres);
                        }
                    });

                    var image = new Image(),
                        canvas = document.createElement('canvas'),
                        context;
                    image.crossOrigin = "anonymous";

                    // Build image
                    // TODO: Get correct initial width and height
                    canvas.width = 1000;
                    canvas.height = 1000;
                    context = canvas.getContext("2d");

                    image.onload = function () {
                        if (context) {
                            context.drawImage(this, 0, 0, this.width, this.height);
                            var croppedCanvas = renderingService.removeBlanks(context, canvas, this.width, this.height);
                            v.data["canvas"].push(croppedCanvas);
                            deferred.resolve();
                        } else {
                            // alert('Get a real browser!');
                        }
                    };

                    if (ffval.length > 0) {

                        // Trigger trigger function that modifies element according to the attached observer
                        var formulaObservers = ele.data("observer")["AnimationChanged"];
                        $.each(formulaObservers["formula"], function (i, v) {
                            v.observer.trigger.call(this, ele, ffval);
                        });
                        if (ele.prop("tagName") === 'image') {
                            image.src = ele.attr('xlink:href');
                        } else {
                            var html = $('<div>').append(ele);
                            if (type === 'svg') {
                                image.src = 'data:image/svg+xml;base64,' + window.btoa('<svg xmlns="http://www.w3.org/2000/svg" style="background-color:white" xmlns:xlink="http://www.w3.org/1999/xlink" width="1000" height="1000">\n' + styleTag + '\n' + html.html() + '</svg>');
                            }
                        }
                    } else {
                        image.src = 'data:image/svg+xml;base64,' + window.btoa('<svg xmlns="http://www.w3.org/2000/svg" style="background-color:white" xmlns:xlink="http://www.w3.org/1999/xlink"></svg>');
                    }
                    return deferred.promise();

                };

                var _loadImage = function (v, felements, styleTag) {

                    var deferred = $.Deferred();

                    // Set default width and height of node
                    v.data['width'] = 0;
                    v.data['height'] = 0;

                    // Create a new image for the node
                    var mcanvas = document.createElement('canvas'),
                        mcontext = mcanvas.getContext("2d");

                    v.data["canvas"] = [];

                    var loaders = [];
                    for (var property in felements) {
                        loaders.push(_loadImage2(property, felements, mcanvas, mcontext, v, styleTag));
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

                    return deferred.promise();

                };

                var elementProjectionGraph = {

                    build: function (elements) {

                        var deferred = $q.defer();

                        var felements = {};
                        var formulas = [];
                        var count = 0;
                        $.each(elements, function (i, v) {
                            var el = $(v);
                            felements[v] = {
                                count: [],
                                clone: el.data('clone'),
                                type: el.parents('svg').length ? 'svg' : 'html',
                                translate: el.data('translate')
                            };
                            $.each($(v).data("formulas"), function () {
                                felements[v]['count'].push(count);
                                count++;
                            });
                            formulas = formulas.concat($(v).data("formulas"));
                        });

                        ws.emit('createCustomTransitionDiagram', {
                            data: {expressions: formulas}
                        }, function (data) {

                            renderingService.getStyles().done(function (css) {

                                var loaders = [];
                                $.each(data.nodes, function (i, v) {
                                    loaders.push(_loadImage(v, felements, css));
                                });
                                $.when.apply(null, loaders).done(function () {

                                    $(function () { // on dom ready

                                        cy = cytoscape({

                                            container: $('#cy')[0],
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

                                        deferred.resolve();

                                    }); // on dom ready

                                });

                            });

                        });

                        return deferred.promise;

                    },

                    refresh: function () {
                        if (cy) {
                            cy.load(cy.elements().jsons())
                        }
                    }

                };

                return elementProjectionGraph;

            }])
            .directive('diagramElementProjectionView', ['diagramElementProjectionGraph', function (diagramElementProjectionGraph) {
                return {
                    replace: true,
                    scope: true,
                    templateUrl: '/bms/libs/prob/elementProjectView.html',
                    controller: function ($scope, $element) {

                        $scope.cyLoaded = false;

                        $scope.refresh = function () {
                            diagramElementProjectionGraph.refresh()
                        };

                        $scope.elements = {
                            selected: []
                        };

                        $scope.init = function () {

                            if ($scope.isOpen && $scope.elements.selected.length > 0) {
                                var elements = [];
                                angular.forEach($scope.getFormulaElements(), function (s) {
                                    if ($scope.elements.selected.indexOf(s.value) >= 0) {
                                        elements.push(s.text);
                                    }
                                });
                                diagramElementProjectionGraph.build(elements).then(function () {
                                    $scope.cyLoaded = true;
                                });
                            }

                        };

                        $scope.$watch('elements.selected', function () {
                            $scope.init();
                        });

                        $scope.showStatus = function () {
                            var selected = [];
                            angular.forEach($scope.getFormulaElements(), function (s) {
                                if ($scope.elements.selected.indexOf(s.value) >= 0) {
                                    selected.push(s.text);
                                }
                            });
                            return selected.length ? selected.join(', ') : 'Not set';
                        };

                    },
                    link: function ($scope, element, attrs) {
                        $scope.$on('dragStart', function () {
                        });
                        $scope.$on('dragStop', function () {
                        });
                        $scope.$on('resize', function () {
                        });
                        $scope.$on('resizeStart', function () {
                        });
                        $scope.$on('resizeStop', function () {
                            $scope.refresh();
                        });
                        $scope.$on('open', function () {
                            $scope.init();
                        });
                    }
                }

            }])
            .factory('diagramTraceGraph', ['$q', 'ws', 'renderingService', function ($q, ws, renderingService) {

                var cy;

                var _loadImage2 = function (v, html, width, height) {

                    var deferred = $.Deferred();

                    // Create a new image for the node
                    var image = new Image(),
                        canvas = document.createElement('canvas'),
                        context;

                    image.crossOrigin = "anonymous";
                    canvas.width = width;
                    canvas.height = height;
                    context = canvas.getContext("2d");

                    image.onload = function () {
                        if (context) {
                            context.drawImage(this, 0, 0, this.width, this.height);
                            var croppedCanvas = renderingService.removeBlanks(context, canvas, this.width, this.height);
                            v.data['svg'] = croppedCanvas.toDataURL('image/png');
                            v.data['width'] = croppedCanvas.width;
                            v.data['height'] = croppedCanvas.height;
                            deferred.resolve();
                        } else {
                            // alert('Get a real browser!');
                        }
                    };
                    image.src = 'data:image/svg+xml;base64,' + window.btoa(html);

                    return deferred.promise();

                };

                var _loadImage = function (v, element, width, height) {

                    var deferred = $.Deferred();

                    if (v.data.id !== 'root' && v.data.id !== '0') {
                        probFunctions.checkObserver({
                            parent: element,
                            stateId: v.data.id
                        }).done(function () {
                            _loadImage2(v, element.html(), width, height).done(function () {
                                deferred.resolve();
                            });
                        });
                    } else {
                        var clonedElement = element.clone();
                        clonedElement.find('svg').attr("width", "50").attr("height", "50");
                        clonedElement.find('svg').empty();
                        _loadImage2(v, clonedElement.html(), 50, 50).done(function () {
                            deferred.resolve();
                        });
                    }

                    return deferred.promise();

                };

                var elementProjectionGraph = {

                    build: function () {

                        var deferred = $q.defer();

                        ws.emit('createTraceDiagram', {}, function (data) {

                            renderingService.getStyles().done(function (css) {

                                var svgElement = $('svg').clone(true);
                                svgElement.prepend($(css));
                                var loaders = [];
                                var wrapper = $('<div>').append(svgElement);
                                $.each(data.nodes, function (i, v) {
                                    loaders.push(_loadImage(v, wrapper, svgElement.attr("width"), svgElement.attr("height")));
                                });
                                $.when.apply(null, loaders).done(function () {

                                    $(function () { // on dom ready

                                        cy = cytoscape({

                                            container: $('#cys')[0],
                                            style: cytoscape.stylesheet()
                                                .selector('node')
                                                .css({
                                                    'shape': 'rectangle',
                                                    'content': 'data(label)',
                                                    'width': 'data(width)',
                                                    'height': 'data(height)',
                                                    'background-color': 'white',
                                                    'border-width': 2,
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
                                                    'line-color': 'black',
                                                    'target-arrow-color': 'black',
                                                    'color': 'black',
                                                    'font-size': '20px',
                                                    'control-point-distance': 60
                                                }),
                                            layout: {
                                                name: 'circle',
                                                animate: false,
                                                fit: true,
                                                padding: 30,
                                                directed: true,
                                                avoidOverlap: true,
                                                roots: '#root'
                                            },
                                            elements: {
                                                nodes: data.nodes,
                                                edges: data.edges
                                            }

                                        });

                                        deferred.resolve();

                                    }); // on dom ready

                                });

                            });

                        });


                        return deferred.promise;

                    },

                    refresh: function () {
                        if (cy) {
                            cy.load(cy.elements().jsons())
                        }
                    }

                };

                return elementProjectionGraph;

            }])
            .directive('diagramTraceView', ['diagramTraceGraph', 'ws', function (diagramTraceGraph, ws) {
                return {
                    replace: true,
                    scope: true,
                    templateUrl: '/bms/libs/prob/traceDiagramView.html',
                    controller: function ($scope, $element) {

                        ws.on('checkObserver', function (trigger) {
                            if (trigger === 'AnimationChanged') {
                                $scope.init();
                            }
                        });

                        $scope.cyLoaded = false;

                        $scope.refresh = function () {
                            diagramTraceGraph.refresh()
                        };

                        $scope.init = function () {
                            if ($scope.isOpen) {
                                diagramTraceGraph.build().then(function () {
                                    $scope.cyLoaded = true;
                                });
                            }
                        };

                        $scope.init();

                    },
                    link: function ($scope, element, attrs) {
                        $scope.$on('open', function () {
                            $scope.init();
                        });
                        $scope.$on('resizeStop', function () {
                            $scope.refresh();
                        });
                    }
                }

            }]);

        angularAMD.bootstrap(probModule);

        return probFunctions;

    }
)
;
