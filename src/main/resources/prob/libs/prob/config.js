require.config({
    paths: {
        "probFunctions": "/bms/libs/prob/probFunctions",
        "prob": "/bms/libs/prob/prob",
        'prob-css': "/bms/libs/prob/prob",
        "jquery-ui-css": "/bms/libs/jquery-ui/jquery-ui.min",
        "jquery-ui-theme-css": "/bms/libs/jquery-ui/jquery-ui.theme.min",
        "tooltipster": "/bms/libs/tooltipster/jquery.tooltipster.min",
        "tooltipster-css": "/bms/libs/tooltipster/tooltipster",
        "tooltipster-shadow-css": "/bms/libs/tooltipster/themes/tooltipster-shadow",
        "jquery-ui": "/bms/libs/jquery-ui/jquery-ui.min",
        "jquery-cookie": "/bms/libs/jquery-cookie/jquery.cookie",
        "cytoscape": "/bms/libs/cytoscape/cytoscape.min",
        "cola": "/bms/libs/cytoscape/cola.v3.min",
        "xeditable": "/bms/libs/xeditable/xeditable.min",
        "xeditable-css": "/bms/libs/xeditable/xeditable"
    },
    shim: {
        'jquery-ui': ['jquery'],
        'tooltipster': ["jquery"],
        'prob': ['probFunctions', 'angularAMD', "ngBMotion", 'jquery-cookie', 'jquery-ui', 'cytoscape'],
        'probFunctions': ['bms', 'tooltipster'],
        'cytoscape': {exports: 'cy', deps: ['jquery', 'cola']}
    }
    //,deps: ['/bms/libs/bmotion/config.js']
});
define(['/bms/libs/bmotion/config.js'], function (config) {
    return config
});
