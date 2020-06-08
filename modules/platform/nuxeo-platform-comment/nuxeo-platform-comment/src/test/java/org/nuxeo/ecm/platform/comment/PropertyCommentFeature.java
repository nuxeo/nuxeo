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

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * Feature for the comment stack using the {@link org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager}
 * implementation.
 *
 * @since 11.1
 * @deprecated since 11.1, use {@link CommentFeature} instead
 */
@Deprecated(since = "11.1")
@Features(CommentFeature.class)
@Deploy("org.nuxeo.ecm.platform.comment.tests:OSGI-INF/property-comment-manager-override.xml")
public class PropertyCommentFeature implements RunnerFeature {

    @RunWith(FeaturesRunner.class)
    @Features(PropertyCommentFeature.class)
    public static class TestPropertyCommentFeature {

        @Inject
        protected CommentManager service;

        @Test
        public void testCommentManager() {
            assertTrue(service instanceof PropertyCommentManager);
        }
    }

}
