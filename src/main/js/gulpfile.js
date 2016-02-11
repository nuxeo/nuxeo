'use strict';

var gulp = require('gulp'),
    path = require('path'),
    del = require('del'),
    runSequence = require('run-sequence'),
    merge = require('merge-stream');

// load plugins
var $ = require('gulp-load-plugins')();

var SOURCES = ['app/app.js', 'app/nuxeo/**/*.js', 'app/ui/**/*.js'],
    DEST = '../../../target/classes/web/nuxeo.war/spreadsheet',
    JQUERY_UI_THEME = 'app/bower_components/jquery-ui/themes/smoothness';

gulp.task('transpile', function () {
  var traceur = path.join('node_modules', 'traceur', 'traceur'),
      out = path.join('app', 'app-build.js');
  return gulp.src('app/app.js', {read:false})
    .pipe($.shell(['node ' + traceur + ' --modules=instantiate --experimental --out '+ out + ' <%= file.path %>']));
});

gulp.task('jshint', function () {
  return gulp.src(SOURCES)
      .pipe($.jshint('.jshintrc'))
      .pipe($.jshint.reporter('jshint-stylish'));
});

gulp.task('clean', function (cb) {
  del.sync([
    'app/app-build.js',
    DEST + '/**',
  ], {force: true}, cb);
});

gulp.task('images', function () {
  var styles = gulp.src([JQUERY_UI_THEME + '/**/*.{png,gif}',
    'app/bower_components/select2/*.{png,gif}'])
      .pipe(gulp.dest(DEST + '/styles'));
  var app = gulp.src(['app/images/**/*'])
      .pipe(gulp.dest(DEST + '/images'));

  return merge(styles, app);
});

gulp.task('html', function () {
  var jsFilter = $.filter('**/*.js');
  var cssFilter = $.filter('**/*.css');

  return gulp.src('app/*.jsp')
    .pipe($.useref.assets({searchPath: '{.tmp,app}'}))
    .pipe(jsFilter)
    .pipe($.uglify())
    .pipe(jsFilter.restore())
    .pipe(cssFilter)
    .pipe($.csso())
    .pipe(cssFilter.restore())
    .pipe($.useref.restore())
    .pipe($.useref())
    .pipe(gulp.dest(DEST))
    .pipe($.size());
});

gulp.task('build', function() {
  runSequence('transpile', 'html', 'images');
});

gulp.task('browser-sync', function () {
  try {
    require('browser-sync')({
      server: {
        baseDir: ['app']
      }
    });
  } catch (e) {
   console.log('Failed to load browser-sync. Please run `npm install browser-sync`.');
   process.exit(-1);
  }
});

gulp.task('watch', ['browser-sync'], function () {
  gulp.watch(SOURCES, ['jshint', 'transpile', require('browser-sync').reload]);
});

gulp.task('default', ['clean'], function () {
  gulp.start('build');
});
