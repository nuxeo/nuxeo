'use strict';

module.exports = function (grunt) {
  // show elapsed time at the end
  require('time-grunt')(grunt);
  // load all grunt tasks
  require('load-grunt-tasks')(grunt);

  // configurable paths
  var yeomanConfig = {
    elements: 'src/main/elements',
    dist: 'target/classes/web/nuxeo.war/analytics/elements'
  };

  grunt.initConfig({
    yeoman: yeomanConfig,
    clean: {
      dist: ['.tmp', '<%= yeoman.dist %>/*'],
      server: '.tmp'
    },
    vulcanize: {
      default: {
        options: {
          inlineScripts: true,
          inlineCss: true
        },
        files: {
          '<%= yeoman.dist %>/elements.vulcanized.html': [
            '<%= yeoman.elements %>/elements.html'
          ]
        }
      }
    },
    copy: {
      dist: {
        files: [{
          expand: true,
          dot: true,
          cwd: '<%= yeoman.elements %>',
          dest: '<%= yeoman.dist %>',
          src: [
            '**',
            '!**/*.css',
            'images/{,*/}*.{webp,gif}'
          ]
        }]
      }
    }
  });

  grunt.registerTask('build', [
    'clean:dist',
    'copy',
    'vulcanize'
  ]);

  grunt.registerTask('default', [
    'build'
  ]);
};
