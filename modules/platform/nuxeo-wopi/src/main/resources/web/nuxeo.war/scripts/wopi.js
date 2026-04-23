/*
 * (C) Copyright 2024-2026 Nuxeo (http://nuxeo.com/) and others.
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
var frameholder = document.getElementById('frameholder');
var office_frame = document.createElement('iframe');
office_frame.name = 'office_frame';
office_frame.id ='office_frame';
// The title should be set for accessibility
office_frame.title = 'Office Online Frame';
// This attribute allows true fullscreen mode in slideshow view
// when using PowerPoint Online's 'view' action.
office_frame.setAttribute('allowfullscreen', 'true');

// The sandbox attribute is needed to allow automatic redirection to the O365 sign-in page in the business user flow
office_frame.setAttribute('sandbox', 'allow-scripts allow-same-origin allow-forms allow-popups allow-top-navigation allow-popups-to-escape-sandbox allow-downloads');

frameholder.appendChild(office_frame);
document.getElementById('office_form').submit();