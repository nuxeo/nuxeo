'use strict';

var gulp = require('gulp');
var path = require('path');
var browserSync = require('browser-sync');
var del = require('del');
var runSequence = require('run-sequence');

// load plugins
var $ = require('gulp-load-plugins')();

var sources = ['app/app.js', 'app/nuxeo/**/*.js', 'app/ui/**/*.js'];

gulp.task('transpile', function () {
  var traceur = path.join('node_modules', 'traceur', 'traceur'),
      out = path.join('app', 'app-build.js');
  return gulp.src('app/app.js', {read:false})
    .pipe($.shell(['node ' + traceur + ' --modules=instantiate --experimental --out '+ out + ' <%= file.path %>']));
});

gulp.task('jshint', function () {
  return gulp.src(sources)
      .pipe($.jshint('.jshintrc'))
      .pipe($.jshint.reporter('jshint-stylish'));
});

gulp.task('clean', function (cb) {
  del.sync([
    'app/app-build.js',
    '../../../target/classes/web/nuxeo.war/spreadsheet/**',
  ], {force: true}, cb);
});

gulp.task('images', function () {
  return gulp.src(['app/images/**/*'])
      .pipe(gulp.dest('../../../target/classes/web/nuxeo.war/spreadsheet/images'))
      .pipe($.size());
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

gulp.task('build', function() {
  runSequence('transpile', 'html', 'images');
});

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
