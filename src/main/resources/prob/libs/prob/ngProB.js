define(['ngBMotion', 'jquery', 'jquery-cookie', 'jquery-ui', "css!jquery-ui-css", "css!jquery-ui-theme-css", "xeditable", "css!xeditable-css", 'cytoscape'], function () {

        return angular.module('probModule', ['bmsModule', 'xeditable'])
            .run(function (editableOptions, $rootScope) {
                editableOptions.theme = 'bs3'; // bootstrap3 theme. Can be also 'bs2', 'default'
            })
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

                        $scope.openView = function (type) {
                            $scope.$broadcast('open' + type);
                        };

                        initSession.then(function (standalone) {
                            if (standalone) {
                                initProB.then(function (data) {
                                    //$scope.host = data.host;
                                    $scope.port = data.port;
                                    var bmsNavigation = angular.element('<prob-navigation></prob-navigation>');
                                    element.find("body").append($compile(bmsNavigation)($scope));
                                    var probViews = angular.element('<div><prob-view type="CurrentTrace"></prob-view>' +
                                    '<prob-view type="Events"></prob-view>' +
                                    '<prob-view type="StateInspector"></prob-view>' +
                                    '<prob-view type="CurrentAnimations"></prob-view>' +
                                    '<prob-view type="Log"></prob-view>' +
                                    '<prob-view type="GroovyConsoleSession"></prob-view>' +
                                    //'<element-projection-view type="ElementProjection"></element-projection-view>' +
                                    '<prob-view type="ModelCheckingUI"></prob-view></div>');
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
                        $scope.reloadModel = function () {
                            $scope.modal.setLabel("Reloading model ...");
                            $scope.modal.show();
                            ws.emit('reloadModel', "", function () {
                                $scope.modal.hide()
                            });
                        };
                    },
                    link: function ($scope, element, attrs) {
                    }
                }
            }])
            .directive('probView', function () {
                return {
                    replace: true,
                    template: '<div><iframe src="" frameBorder="0"></iframe></div>',
                    link: function ($scope, element, attrs) {

                        var viewtype = attrs.type;
                        var iframe = $(element.find("iframe"));
                        var aopen = $.cookie("open_" + viewtype) === undefined ? false : $.cookie("open_" + viewtype);

                        $scope.fixSize = function (dialog, obj, ox, oy) {
                            var newwidth = dialog.parent().width() - ox;
                            var newheight = dialog.parent().height() - oy;
                            obj.attr("style", "width:" + (newwidth) + "px;height:" + (newheight - 50) + "px");
                        };

                        $scope.$on('open' + viewtype, function () {
                            $(element).dialog("open");
                        });

                        $(element).dialog({

                            dragStart: function () {
                                iframe.hide();
                            },
                            dragStop: function (event, ui) {
                                iframe.show();
                                $.cookie("position_top_" + viewtype, ui.position.top);
                                $.cookie("position_left_" + viewtype, ui.position.left)
                            },
                            resize: function () {
                                iframe.hide();
                            },
                            resizeStart: function () {
                                iframe.hide();
                            },
                            resizeStop: function () {
                                iframe.show();
                                $scope.fixSize($(element), iframe, 0, 0);
                            },
                            open: function () {
                                $.cookie("open_" + viewtype, true);
                                iframe.attr("src", document.location.protocol + '//' + document.location.hostname + ":" + $scope.port +
                                "/sessions/" + viewtype);
                                $scope.fixSize($(element), iframe, 0, 0);
                                element.css('overflow', 'hidden'); //this line does the actual hiding
                                var toppos = $.cookie("position_top_" + viewtype);
                                var leftpos = $.cookie("position_left_" + viewtype);
                                if (toppos !== undefined && leftpos !== undefined) {
                                    element.parent().css("top", toppos + "px").css("left", leftpos + "px")
                                }
                            },
                            close: function () {
                                $.removeCookie("open_" + viewtype);
                                $.removeCookie("position_top" + viewtype);
                                $.removeCookie("position_left_" + viewtype);
                            },
                            autoOpen: aopen,
                            width: 350,
                            height: 400,
                            title: viewtype

                        });

                    }
                }
            })
            .controller('elementProjectionCtrl', ['$scope', 'ws', 'elementProjectionGraph', function ($scope, ws, elementProjectionGraph) {

                $scope.cyLoaded = false;

                $scope.refresh = function () {
                    elementProjectionGraph.refresh()
                };

                ws.on('checkObserver', function (trigger) {
                    // TODO: Force user to reload graph
                });

                $scope.isOpen = false;

                $scope.user = {
                    status: []
                };

                $scope.init = function () {

                    if ($scope.isOpen && $scope.user.status.length > 0) {
                        var elements = [];
                        angular.forEach($scope.formulaElements, function (s) {
                            if ($scope.user.status.indexOf(s.value) >= 0) {
                                elements.push(s.text);
                            }
                        });
                        elementProjectionGraph(elements).then(function () {
                            $scope.cyLoaded = true;
                        });
                    }

                };

                $scope.$watch('user.status', function () {
                    $scope.init();
                });

                $scope.showStatus = function () {
                    var selected = [];
                    angular.forEach($scope.formulaElements, function (s) {
                        if ($scope.user.status.indexOf(s.value) >= 0) {
                            selected.push(s.text);
                        }
                    });
                    return selected.length ? selected.join(', ') : 'Not set';
                };

            }])
            .factory('elementProjectionGraph', ['$q', 'ws', function ($q, ws) {

                var cy;

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

                var _loadImage2 = function (property, felements, mcanvas, mcontext, v) {

                    var deferred = $.Deferred();

                    // Prepare data
                    var ele = felements[property].clone;
                    var count = felements[property].count;
                    var ffval = [];
                    $.each(count, function (i2, v2) {
                        var trans = v.data.translated[0];
                        if (trans != null && trans.length > 0) {
                            ffval.push(trans[v2]);
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
                            var croppedCanvas = removeBlanks(context, canvas, this.width, this.height);
                            v.data["canvas"].push(croppedCanvas);
                            deferred.resolve();
                        } else {
                            // alert('Get a real browser!');
                        }
                    };

                    if (ffval.length > 0) {
                        // Trigger trigger function that modifies element according to the attached observer
                        ele.trigger('trigger', [ffval]);
                        if (ele.prop("tagName") === 'image') {
                            image.src = ele.attr('xlink:href');
                        }
                        else {
                            var html = $('<div>').append(ele).html();
                            image.src = 'data:image/svg+xml;base64,' + window.btoa('<svg xmlns="http://www.w3.org/2000/svg" style="background-color:white" xmlns:xlink="http://www.w3.org/1999/xlink" width="1000" height="1000">' + html + '</svg>');
                        }

                    } else {
                        image.src = 'data:image/svg+xml;base64,' + window.btoa('<svg xmlns="http://www.w3.org/2000/svg" style="background-color:white" xmlns:xlink="http://www.w3.org/1999/xlink"></svg>');
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

                    // if (val !== "<< undefined >>") {

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

                    //}
                    //else {
                    //    v.data['svg'] = 'data:image/svg+xml;base64,' + window.btoa('<svg
                    // xmlns="http://www.w3.org/2000/svg" xmlns:svg="http://www.w3.org/2000/svg"></svg>');
                    // deferred.resolve(); }

                    return deferred.promise();

                };

                var elementProjectionGraph = function (elements) {

                    var deferred = $q.defer();

                    var felements = {};
                    var formulas = [];
                    var count = 0;
                    $.each(elements, function (i, v) {
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

                    ws.emit('createCustomTransitionDiagram', {
                        data: {expressions: formulas}
                    }, function (data) {
                        var loaders = [];
                        $.each(data.nodes, function (i, v) {
                            loaders.push(_loadImage(v, felements));
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
                            }); // on dom ready

                        });
                    });

                    return deferred.promise;

                };

                elementProjectionGraph.refresh = function () {
                    cy.load(cy.elements().jsons())
                };

                return elementProjectionGraph;

            }])
            .directive('elementProjectionView', function () {
                return {
                    replace: true,
                    templateUrl: '/bms/libs/prob/elementProjectView.html',
                    link: function ($scope, element, attrs) {

                        var viewtype = attrs.type;
                        var aopen = $.cookie("open_" + viewtype) === undefined ? false : $.cookie("open_" + viewtype);
                        var diagramElement = $(element).find(".diagram");

                        $scope.fixSize = function (dialog, obj, ox, oy) {
                            var newwidth = dialog.parent().width() - ox;
                            var newheight = dialog.parent().height() - oy;
                            obj.attr("style", "width:" + (newwidth) + "px;height:" + (newheight - 50) + "px");
                        };

                        $scope.$on('open' + viewtype, function () {
                            $(element).dialog("open");
                        });

                        $(element).dialog({

                            dragStart: function () {
                            },
                            dragStop: function (event, ui) {
                                $.cookie("position_top_" + viewtype, ui.position.top);
                                $.cookie("position_left_" + viewtype, ui.position.left)
                            },
                            resize: function () {
                            },
                            resizeStart: function () {
                            },
                            resizeStop: function () {
                                $scope.fixSize($(element), diagramElement, 0, 0);
                                $scope.refresh();
                            },
                            open: function () {
                                $.cookie("open_" + viewtype, true);
                                $scope.fixSize($(element), diagramElement, 0, 0);
                                $scope.isOpen = true;
                                $scope.init();
                            },
                            close: function () {
                                $.removeCookie("open_" + viewtype);
                                $.removeCookie("position_top" + viewtype);
                                $.removeCookie("position_left_" + viewtype);
                                $scope.isOpen = false;
                            },
                            autoOpen: aopen,
                            width: 350,
                            height: 400,
                            title: viewtype

                        });

                    }
                }
            });

    }
);
