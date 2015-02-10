require.config({
    paths: {
        "prob": "/bms/libs/prob/prob",
        "ngProB": "/bms/libs/prob/ngProB",
        'prob-css': "/bms/libs/prob/prob",
        "jquery-ui-css": "/bms/libs/jquery-ui/jquery-ui.min",
        "jquery-ui-theme-css": "/bms/libs/jquery-ui/jquery-ui.theme.min",
        "tooltipster": "/bms/libs/tooltipster/jquery.tooltipster.min",
        "tooltipster-css": "/bms/libs/tooltipster/tooltipster",
        "tooltipster-shadow-css": "/bms/libs/tooltipster/themes/tooltipster-shadow",
        "jquery-ui": "/bms/libs/jquery-ui/jquery-ui.min",
        "jquery-cookie": "/bms/libs/jquery-cookie/jquery.cookie",
        "cytoscape": "/bms/libs/cytoscape/cytoscape.min",
        "cola": "/bms/libs/cytoscape/cola.v3.min"
    },
    shim: {
        'jquery-ui': ['jquery'],
        'tooltipster': ["jquery"],
        'ngProB': ["ngBMotion", 'jquery-cookie', 'jquery-ui'],
        'prob': ['ngProB', 'bms', 'angularAMD', 'tooltipster'],
        'cytoscape': {exports: 'cy', deps: ['jquery', 'cola']}
    },
    deps: ['/bms/libs/bmotion/config.js']
});