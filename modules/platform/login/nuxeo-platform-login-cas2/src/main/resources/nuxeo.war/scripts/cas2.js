/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Antoine Taillefer
 */
'use strict';
var indexEndServerName = location.href.indexOf('/', 8);
var indexEndBaseURL = location.href.indexOf('/', indexEndServerName + 2);
var logoutURL = location.href.substring(0, indexEndBaseURL) + '/logout';

setTimeout(function() { window.location.replace(logoutURL); }, 0);