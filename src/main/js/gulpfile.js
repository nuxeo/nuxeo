'use strict';

var gulp = require('gulp');
var path = require('path');
var browserSync = require('browser-sync');

// load plugins
var $ = require('gulp-load-plugins')();

var sources = ['app/app.js', 'app/nuxeo/**/*.js', 'app/ui/**/*.js'];

gulp.task('transpile', function () {
  var traceur = path.join('node_modules', 'traceur', 'traceur'),
      out = path.join('app', 'app-build.js'),
      app = path.join('app', 'app.js');
  return $.shell.task(traceur + ' --modules=instantiate --experimental --out '+ out + ' ' + app);
});

gulp.task('jshint', function () {
  return gulp.src(sources)
      .pipe($.jshint('.jshintrc'))
      .pipe($.jshint.reporter('jshint-stylish'));
});

gulp.task('clean', function () {
  return gulp.src(['app-build.js'], { read: false }).pipe($.clean());
});

gulp.task('html', function () {
  var jsFilter = $.filter('**/*.js');
  var cssFilter = $.filter('**/*.css');

  return gulp.src('app/*.html')
    .pipe($.useref.assets({searchPath: '{.tmp,app}'}))
    .pipe(jsFilter)
    .pipe($.uglify())
    .pipe(jsFilter.restore())
    .pipe(cssFilter)
    .pipe($.csso())
    .pipe(cssFilter.restore())
    .pipe($.useref.restore())
    .pipe($.useref())
    .pipe(gulp.dest('../../../target/classes/web/nuxeo.war/spreadsheet'))
    .pipe($.size());
});

gulp.task('build', ['transpile', 'html']);

gulp.task('browser-sync', function () {
  browserSync({
    server: {
      baseDir: ['app']
    }
  });
});

gulp.task('watch', ['browser-sync'], function () {
  gulp.watch(sources, ['jshint', 'transpile', browserSync.reload]);
});

gulp.task('default', ['clean'], function () {
  gulp.start('build');
});
