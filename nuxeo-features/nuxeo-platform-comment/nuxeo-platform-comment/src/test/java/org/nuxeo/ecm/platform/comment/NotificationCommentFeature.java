/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.platform.comment;

import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * Feature for the comment notification stack.
 * 
 * @since 11.1
 */
@Features(CommentFeature.class)
@Deploy("org.nuxeo.ecm.platform.notification.core")
@Deploy("org.nuxeo.ecm.platform.notification.api")
@Deploy("org.nuxeo.ecm.platform.url.api")
@Deploy("org.nuxeo.ecm.platform.url.core")
public class NotificationCommentFeature implements RunnerFeature {
}
