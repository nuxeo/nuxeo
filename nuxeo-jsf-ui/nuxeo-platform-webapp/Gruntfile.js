'use strict';

module.exports = function (grunt) {
  // show elapsed time at the end
  require('time-grunt')(grunt);
  // load all grunt tasks
  require('load-grunt-tasks')(grunt);

  grunt.initConfig({
    config: {
      source: 'src/main/resources/web/nuxeo.war',
      target: 'target/classes/web/nuxeo.war'
    },
    vulcanize: {
      permissions: {
        options: {
          inlineScripts: true,
          inlineCss: true
        },
        files: {
          '<%= config.target %>/permissions/components/elements.vulcanized.html': [
            '<%= config.target %>/bower_components/nuxeo-document-permissions/nuxeo-document-permissions.html'
          ]
        },
      },
    },
    clean: {
      bower_components: {
        files: [{
          src: [
            '<%= config.target %>/bower_components/*',
            '!<%= config.target %>/bower_components/es6-promise',
            '!<%= config.target %>/bower_components/moment',
            '!<%= config.target %>/bower_components/nuxeo',
            '!<%= config.target %>/bower_components/webcomponentsjs'
          ]
        }]
      }
    }
  });

  grunt.registerTask('default', [
    'vulcanize:permissions',
    'clean:bower_components'
  ]);
};
