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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newComment;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.BridgeCommentManager;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 11.1
 */
@Features({ PropertyCommentFeature.class, BridgeCommentFeature.class })
public class TestBridgeFromPropertyToTreeCommentManager extends AbstractTestCommentManager {

    @Inject
    @Named("first")
    protected CommentManager first;

    public TestBridgeFromPropertyToTreeCommentManager() {
        super(BridgeCommentManager.class);
    }

    @Test
    public void testDeleteCommentAsProperty() {
        Comment comment = first.createComment(session, newComment(commentedDocModel.getId()));

        // check bridge can delete it
        commentManager.deleteComment(session, comment.getId());
        assertFalse(session.exists(new IdRef(comment.getId())));
    }

    @Test
    public void testGetTopLevelDocumentRefAsProperty() {
        Comment comment = first.createComment(session, newComment(commentedDocModel.getId()));

        DocumentRef commentedDocRef = commentedDocModel.getRef();
        DocumentRef commentRef = new IdRef(comment.getId());
        assertEquals(commentedDocRef, commentManager.getTopLevelDocumentRef(session, commentRef));
    }
}
