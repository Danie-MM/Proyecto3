// Solo para pruebas dentro de Docker. El servidor de Angular usa su configuración normal.
module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    reporters: ['progress'],
    browsers: ['ChromeHeadlessDocker'],
    customLaunchers: {
      ChromeHeadlessDocker: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox', '--disable-dev-shm-usage']
      }
    },
    singleRun: true,
    restartOnFileChange: false,
    browserNoActivityTimeout: 60000,
    client: { jasmine: { random: false } }
  });
};
