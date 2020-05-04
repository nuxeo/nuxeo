/*
 * (C) Copyright 2019-2020 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.platform.comment.CommentUtils.addNotificationSubscriptions;

import org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 11.1
 * @deprecated since 11.1, in order to follow service depreciation
 */
@Deprecated(since = "11.1")
@Features(PropertyCommentFeature.class)
public class TestPropertyCommentNotification extends AbstractTestCommentNotification {

    public TestPropertyCommentNotification() {
        super(PropertyCommentManager.class);
    }

    @Override
    public void before() {
        super.before();
        // abstract supposes auto subscription is enabled
        // property implementation doesn't have this feature, so enable it for each tests
        addNotificationSubscriptions(session.getPrincipal(), commentedDocModel, "CommentAdded", "CommentUpdated");
    }
}
