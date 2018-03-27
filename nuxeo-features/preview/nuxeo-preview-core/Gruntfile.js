'use strict';

module.exports = function (grunt) {
  // show elapsed time at the end
  require('time-grunt')(grunt);
  // load all grunt tasks
  require('load-grunt-tasks')(grunt);


  grunt.initConfig({
    config: {
      target: 'target/classes/web/nuxeo.war'
    },
    copy: {
      pdfjs: {
        cwd: '<%= config.target %>/bower_components/nuxeo-ui-elements/viewers/pdfjs',
        src: ['**/*'],
        dest: '<%= config.target %>/viewers/pdfjs',
        expand: true
      }
    },
    mkdir: {
        viewers: {
            options: {
                 create: ['<%= config.target %>/viewers']
            }
        }
    },
    vulcanize: {
      pdfViewer: {
        options: {
          inlineScripts: true,
          inlineCss: true
        },
        files: {
          '<%= config.target %>/viewers/nuxeo-pdf-viewer.vulcanized.html': [
            '<%= config.target %>/bower_components/nuxeo-ui-elements/viewers/nuxeo-pdf-viewer.html'
          ]
        }
      },
      imageViewer: {
        options: {
          inlineScripts: true,
          inlineCss: true
        },
        files: {
          '<%= config.target %>/viewers/nuxeo-image-viewer.vulcanized.html': [
            '<%= config.target %>/bower_components/nuxeo-ui-elements/viewers/nuxeo-image-viewer.html'
          ]
        }
      },
      videoViewer: {
        options: {
          inlineScripts: true,
          inlineCss: true
        },
        files: {
          '<%= config.target %>/viewers/nuxeo-video-viewer.vulcanized.html': [
            '<%= config.target %>/bower_components/nuxeo-ui-elements/viewers/nuxeo-video-viewer.html'
          ]
        },
      },
      markdownViewer: {
        options: {
          inlineScripts: true,
          inlineCss: true
        },
        files: {
          '<%= config.target %>/viewers/marked-element.vulcanized.html': [
            '<%= config.target %>/bower_components/marked-element/marked-element.html'
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
    'mkdir:viewers',
    'vulcanize:pdfViewer',
    'copy:pdfjs',
    'vulcanize:imageViewer',
    'vulcanize:videoViewer',
    'vulcanize:markdownViewer',
    'clean:bower_components'
  ]);
};
