/*
 (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 Contributors:
 Tiago Cardoso <tcardoso@nuxeo.com>
 */

'use strict';

// Include Gulp & tools we'll use
var gulp = require('gulp');
var $ = require('gulp-load-plugins')();
var del = require('del');
var runSequence = require('run-sequence');
var path = require('path');

var DIST = 'target/classes/web/nuxeo.war/platform-3d';

var dist = function (subpath) {
    return !subpath ? DIST : path.join(DIST, subpath);
};

// Vulcanize granular configuration
gulp.task('vulcanize', function () {
    return gulp.src(dist('bower_components/nuxeo-3d-elements/nuxeo-3d-viewer.html'))
        .pipe($.vulcanize({
            stripComments: true,
            inlineCss: true,
            inlineScripts: true
        }))
        .pipe(gulp.dest(dist()))
        .pipe($.size({title: 'vulcanize'}));
});

gulp.task('copy', function () {
    return gulp.src([dist('bower_components/nuxeo-3d-elements/nuxeo-3d-viewer.html'),
        dist('bower_components/nuxeo-3d-elements/nuxeo-3d-elements.html')])
        .pipe(gulp.dest(dist('platform-3d')));
});

// Strip unnecessary stuff
gulp.task('strip', function () {
    return del([
        dist('bower_components/**'),
        '!' + dist('bower_components'),
        '!' + dist('bower_components/webcomponentsjs'),
        '!' + dist('bower_components/webcomponentsjs/**/*')
    ]);
});

// Clean output directory
gulp.task('clean', function () {
    return del(['.tmp']);
});

// Build production files, the default task
gulp.task('default', ['clean'], function (cb) {
    runSequence(
        'vulcanize',
        'copy',
        'strip',
        cb);
});