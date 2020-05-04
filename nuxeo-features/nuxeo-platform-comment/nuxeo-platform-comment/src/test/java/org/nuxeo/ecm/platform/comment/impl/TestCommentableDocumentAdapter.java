/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.platform.comment.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_CREATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT;

import java.util.Calendar;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.comment.CommentFeature;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Features(CommentFeature.class)
public class TestCommentableDocumentAdapter {

    @Inject
    protected CoreSession session;

    @Inject
    protected CommentManager commentManager;

    @Test
    public void testCommentableDocumentAdapter() {
        DocumentModel file = session.createDocumentModel("/", "file001", "File");
        file = session.createDocument(file);

        CommentableDocument commentableDocument = file.getAdapter(CommentableDocument.class);
        DocumentModel comment = session.createDocumentModel(COMMENT_DOC_TYPE);
        comment.setPropertyValue(COMMENT_TEXT, "Test");
        comment.setPropertyValue(COMMENT_AUTHOR, "james");
        comment.setPropertyValue(COMMENT_CREATION_DATE, Calendar.getInstance());

        // Create a comment
        commentableDocument.addComment(comment);
        session.save();

        // Creation check
        assertEquals(1, commentableDocument.getComments().size());
        DocumentModel newComment = commentableDocument.getComments().get(0);
        assertThat(newComment.getPropertyValue(COMMENT_TEXT)).isEqualTo("Test");

        // Deletion check
        commentableDocument.removeComment(newComment);
        assertTrue(commentableDocument.getComments().isEmpty());
    }
}
