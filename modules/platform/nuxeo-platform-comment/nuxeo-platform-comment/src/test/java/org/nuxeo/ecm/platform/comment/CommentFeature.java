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
import org.nuxeo.ecm.platform.comment.impl.TreeCommentManager;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * Feature for the comment stack, which provides the default implementation of
 * {@link org.nuxeo.ecm.platform.comment.api.CommentManager}, which is
 * {@link org.nuxeo.ecm.platform.comment.impl.TreeCommentManager}
 *
 * @since 11.1
 */
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.platform.comment.api")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.comment")
public class CommentFeature implements RunnerFeature {

    @RunWith(FeaturesRunner.class)
    @Features(CommentFeature.class)
    public static class TestCommentFeature {

        @Inject
        protected CommentManager service;

        @Test
        public void testCommentManager() {
            assertTrue(service instanceof TreeCommentManager);
        }
    }
}
