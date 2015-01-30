define(['ngBMotion', 'jquery', 'jquery-cookie', 'jquery-ui', "css!jquery-ui-css", "css!jquery-ui-theme-css"], function (ngBMotion) {

        return ngBMotion
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
                    link: function ($scope, element, attrs) {

                        $scope.openView = function (type) {
                            $scope.$broadcast('open' + type);
                        };

                        initSession.then(function (standalone) {
                            if (standalone) {
                                initProB.then(function (data) {
                                    //$scope.host = data.host;
                                    $scope.port = data.port;
                                    var bmsNavigation = angular.element('<prob-navigation></prob-navigation>');
                                    element.find("body").append($compile(bmsNavigation)($scope))
                                    var probViews = angular.element('<div><prob-view type="CurrentTrace"></prob-view>' +
                                    '<prob-view type="Events"></prob-view>' +
                                    '<prob-view type="StateInspector"></prob-view>' +
                                    '<prob-view type="CurrentAnimations"></prob-view>' +
                                    '<prob-view type="Log"></prob-view>' +
                                    '<prob-view type="GroovyConsoleSession"></prob-view>' +
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
                            $scope.modal.setLabel("Reloading model ...")
                            $scope.modal.show()
                            ws.emit('reloadModel', "", function () {
                                $scope.modal.hide()
                            });
                        }
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

                        var viewtype = attrs.type

                        $scope.$on('open' + viewtype, function () {
                            $(element).dialog("open");
                        });

                        var iframe = $(element.find("iframe"))
                        var aopen = $.cookie("open_" + viewtype) === undefined ? false : $.cookie("open_" + viewtype)

                        function fixSizeDialog(dialog, obj, ox, oy) {
                            var newwidth = dialog.parent().width() - ox
                            var newheight = dialog.parent().height() - oy
                            obj.attr("style", "width:" + (newwidth) + "px;height:" + (newheight - 50) + "px");
                        }

                        $(element).dialog({

                            dragStart: function () {
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
                            resizeStop: function () {
                                iframe.show();
                                fixSizeDialog($(element), iframe, 0, 0);
                            },
                            open: function () {
                                $.cookie("open_" + viewtype, true);
                                iframe.attr("src", document.location.protocol + '//' + document.location.hostname + ":" + $scope.port +
                                "/sessions/" + viewtype);
                                fixSizeDialog($(element), iframe, 0, 0);
                                element.css('overflow', 'hidden'); //this line does the actual hiding
                                var toppos = $.cookie("position_top_" + viewtype)
                                var leftpos = $.cookie("position_left_" + viewtype)
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
            });

    }
);
