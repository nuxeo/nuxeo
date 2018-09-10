/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.platform.comment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.platform.comment.api.CommentManager.Feature.COMMENTS_LINKED_WITH_PROPERTY;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 10.3
 */
@Deploy("org.nuxeo.ecm.platform.query.api")
public class TestPropertyCommentManager extends AbstractTestCommentManager {

    @Test
    public void testUpdateComment() {

        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        domain = session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel("/domain", "test", "File");
        doc = session.createDocument(doc);
        session.save();

        String author = "toto";
        String text = "I am a comment !";
        Comment comment = new CommentImpl();
        comment.setAuthor(author);
        comment.setText(text);
        comment.setParentId(doc.getId());

        comment = commentManager.createComment(session, comment);

        try {
            commentManager.updateComment(session, "fakeId", comment);
            fail("Getting a comment should have failed !");
        } catch (IllegalArgumentException e) {
            // ok
        }

        comment.setAuthor("titi");
        commentManager.updateComment(session, comment.getId(), comment);
        comment = commentManager.getComment(session, comment.getId());

        assertEquals("titi", comment.getAuthor());
        assertEquals(text, comment.getText());

    }

}
