require.config({
    paths: {
        "prob": "/bms/libs/prob/prob",
        "ngProB": "/bms/libs/prob/ngProB",
        'prob-css': "/bms/libs/prob/prob"
    },
    shim: {
        'ngProB': ["ngBMotion", 'jquery-cookie', 'jquery-ui'],
        'prob': ['ngProB', 'bms', 'angularAMD', 'tooltipster']
    },
    deps: ['/bms/libs/bmotion/config.js']
})