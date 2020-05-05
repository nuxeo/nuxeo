/*
 * (C) Copyright 2018-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuno Cunha <ncunha@nuxeo.com>
 */

package org.nuxeo.ecm.platform.comment;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.platform.comment.CommentUtils.emptyComment;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newComment;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newExternalComment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.ExternalEntity;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentNotFoundException;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentSecurityException;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Features(CommentFeature.class)
public abstract class AbstractTestCommentManager {

    protected static final String JAMES = "james";

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected CommentManager commentManager;

    protected final Class<?> commentManagerUnderTest;

    protected DocumentModel commentedDocModel;

    protected AbstractTestCommentManager(Class<?> commentManagerUnderTest) {
        this.commentManagerUnderTest = commentManagerUnderTest;
    }

    @Before
    public void init() {
        assertEquals(commentManagerUnderTest, commentManager.getClass());

        // create a Domain and a File to comment
        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        session.createDocument(domain);
        commentedDocModel = session.createDocumentModel("/domain", "test", "File");
        commentedDocModel = session.createDocument(commentedDocModel);

        // give permission to comment to james
        ACP acp = commentedDocModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE(JAMES, SecurityConstants.READ, true));
        session.setACP(commentedDocModel.getRef(), acp, false);
        session.save();
    }

    // ------------
    // CRUD tests
    // ------------

    @Test
    public void testCreateComment() {
        String text = "I am a comment!";

        // check that parent id must be valid
        try {
            commentManager.createComment(session, newComment("fakeId", text));
            fail("Creating a comment should have failed !");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        // check regular creation
        Comment comment = newComment(commentedDocModel.getId(), text);
        comment = commentManager.createComment(session, comment);
        assertEquals("Administrator", comment.getAuthor());
        assertEquals(text, comment.getText());
        assertEquals(commentedDocModel.getId(), comment.getParentId());
        assertNotNull(comment.getCreationDate());
        assertNotNull(comment.getModificationDate());

        // check james can create (because he has READ on commentedDocModel)
        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);
        comment = newComment(commentedDocModel.getId(), text);
        comment = commentManager.createComment(jamesSession, comment);
        assertEquals(JAMES, comment.getAuthor());

        try {
            // check jane can not create
            CoreSession janeSession = coreFeature.getCoreSession("jane");
            comment = newComment(commentedDocModel.getId(), text);
            commentManager.createComment(janeSession, comment);
            fail("jane should not be able to create comment");
        } catch (CommentSecurityException | CommentNotFoundException cse) {
            // TODO check maybe create a method to override NotFound seems to come from 1st impl
            // ok
        }
    }

    @Test
    public void testCreateCommentWithCreationDate() {
        String text = "I am a comment!";
        Instant creationDate = Instant.parse("2020-04-25T10:35:10.00Z");

        Comment comment = newComment(commentedDocModel.getId(), text);
        comment.setCreationDate(creationDate);
        comment = commentManager.createComment(session, comment);
        assertEquals("Administrator", comment.getAuthor());
        assertEquals(text, comment.getText());
        assertEquals(commentedDocModel.getId(), comment.getParentId());
        assertEquals(creationDate, comment.getCreationDate());
        assertNotNull(comment.getModificationDate());
    }

    @Test
    public void testCreateReply() {
        // give permission to comment to jdoe
        ACP acp = commentedDocModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("jdoe", SecurityConstants.READ, true));
        session.setACP(commentedDocModel.getRef(), acp, false);
        session.save();

        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);
        CoreSession jdoeSession = coreFeature.getCoreSession("jdoe");

        Comment comment = newComment(commentedDocModel.getId(), "I am a comment!");
        comment = commentManager.createComment(jamesSession, comment);

        // check reply creation
        Comment reply = newComment(comment.getId(), "I am a reply!");
        reply = commentManager.createComment(jdoeSession, reply);
        assertEquals("jdoe", reply.getAuthor());
        assertEquals("I am a reply!", reply.getText());
        assertEquals(comment.getId(), reply.getParentId());
        var replyAncestorIds = new HashSet<>(reply.getAncestorIds());
        assertTrue("Commented document not found in ancestors", replyAncestorIds.remove(commentedDocModel.getId()));
        assertTrue("Comment not found in ancestors", replyAncestorIds.remove(comment.getId()));
        assertTrue("Reply should have 2 ancestorIds", replyAncestorIds.isEmpty());

        // check sub reply creation
        Comment subReply = newComment(reply.getId(), "I am a sub reply!");
        subReply = commentManager.createComment(jamesSession, subReply);
        assertEquals(JAMES, subReply.getAuthor());
        assertEquals("I am a sub reply!", subReply.getText());
        assertEquals(reply.getId(), subReply.getParentId());
        var subReplyAncestorIds = new HashSet<>(subReply.getAncestorIds());
        assertTrue("Commented document not found in ancestors", subReplyAncestorIds.remove(commentedDocModel.getId()));
        assertTrue("Comment not found in ancestors", subReplyAncestorIds.remove(comment.getId()));
        assertTrue("Reply not found in ancestors", subReplyAncestorIds.remove(reply.getId()));
        assertTrue("Sub reply should have 3 ancestorIds", subReplyAncestorIds.isEmpty());
    }

    @Test
    public void testGetComment() {
        String text = "I am a comment!";
        Comment comment = newComment(commentedDocModel.getId(), text);
        comment = commentManager.createComment(session, comment);

        try {
            commentManager.getComment(session, "fakeId");
            fail("Getting a comment should have failed !");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        // check regular get
        comment = commentManager.getComment(session, comment.getId());
        assertEquals("Administrator", comment.getAuthor());
        assertEquals(text, comment.getText());
        assertEquals(commentedDocModel.getId(), comment.getParentId());
    }

    // exist to be overridden by implementation having a different permissions check
    @Test
    public void testGetCommentPermissions() {
        Comment comment = newComment(commentedDocModel.getId(), "I am a comment!");
        comment = commentManager.createComment(session, comment);
        assertEquals("Administrator", comment.getAuthor());

        // check james can get (because he has READ on commentedDocModel)
        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);
        commentManager.getComment(jamesSession, comment.getId());

        try {
            // check jane can not get
            CoreSession janeSession = coreFeature.getCoreSession("jane");
            commentManager.getComment(janeSession, comment.getId());
            fail("jane should not be able to get comment");
        } catch (CommentSecurityException cse) {
            // ok
        }
    }

    @Test
    public void testGetReply() {
        Comment c1 = commentManager.createComment(session, newComment(commentedDocModel.getId(), "I am a comment!"));
        Comment c2 = commentManager.createComment(session, newComment(c1.getId(), "I am a reply!"));
        Comment c3 = commentManager.createComment(session, newComment(c2.getId(), "Me too!"));

        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);

        // check james can get replies (because he has READ on commentedDocModel)
        c2 = commentManager.getComment(jamesSession, c2.getId());
        assertEquals("Administrator", c2.getAuthor());
        assertEquals("I am a reply!", c2.getText());
        assertEquals(c1.getId(), c2.getParentId());

        c3 = commentManager.getComment(jamesSession, c3.getId());
        assertEquals("Administrator", c3.getAuthor());
        assertEquals("Me too!", c3.getText());
        assertEquals(c2.getId(), c3.getParentId());

        CoreSession janeSession = coreFeature.getCoreSession("jane");

        // check jane can not get replies
        try {
            commentManager.getComment(janeSession, c2.getId());
            fail("jane should not be able to get comment");
        } catch (CommentSecurityException cse) {
            // ok
        }
        try {
            commentManager.getComment(janeSession, c3.getId());
            fail("jane should not be able to get comment");
        } catch (CommentSecurityException cse) {
            // ok
        }
    }

    @Test
    public void testGetComments() {
        Comment c1 = commentManager.createComment(session, newComment(commentedDocModel.getId(), "I am a comment!"));
        Comment c2 = commentManager.createComment(session, newComment(commentedDocModel.getId(), "Me too!"));
        Comment c3 = commentManager.createComment(session,
                newComment(commentedDocModel.getId(), "I am the last comment!"));
        Comment c4 = commentManager.createComment(session, newComment(commentedDocModel.getId(), "No sorry, it's me!"));
        // getComments uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();

        assertEquals(List.of(c1, c2, c3, c4), commentManager.getComments(session, commentedDocModel.getId()));
    }

    @Test
    public void testGetCommentsOrdering() {
        Comment c1 = commentManager.createComment(session, newComment(commentedDocModel.getId(), "I am a comment!"));
        Comment c2 = commentManager.createComment(session, newComment(commentedDocModel.getId(), "Me too!"));
        Comment c3 = commentManager.createComment(session,
                newComment(commentedDocModel.getId(), "I am the last comment!"));
        Comment c4 = commentManager.createComment(session, newComment(commentedDocModel.getId(), "No sorry, it's me!"));
        // getComments uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();

        assertEquals(List.of(c1, c2, c3, c4), commentManager.getComments(session, commentedDocModel.getId()));
        assertEquals(List.of(c1, c2, c3, c4), commentManager.getComments(session, commentedDocModel.getId(), true));
        assertEquals(List.of(c4, c3, c2, c1), commentManager.getComments(session, commentedDocModel.getId(), false));
    }

    @Test
    public void testGetCommentsPagination() {
        List<Comment> comments = IntStream.rangeClosed(1, 10)
                                          // type inference issue
                                          .mapToObj(i -> (Comment) newComment(commentedDocModel.getId(),
                                                  "I am the " + i + "  comment!"))
                                          .map(c -> commentManager.createComment(session, c))
                                          .collect(toList());
        // getComments uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();

        assertEquals(comments, commentManager.getComments(session, commentedDocModel.getId()));
        assertEquals(comments.subList(0, 3), commentManager.getComments(session, commentedDocModel.getId(), 3L, 0L));
        assertEquals(comments.subList(3, 6), commentManager.getComments(session, commentedDocModel.getId(), 3L, 1L));
    }

    @Test
    public void testGetCommentsPaginationOrdering() {
        List<Comment> comments = IntStream.rangeClosed(1, 10)
                                          // type inference issue
                                          .mapToObj(i -> (Comment) newComment(commentedDocModel.getId(),
                                                  "I am the " + i + "  comment!"))
                                          .map(c -> commentManager.createComment(session, c))
                                          .collect(toList());
        // getComments uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();

        assertEquals(comments.subList(3, 6),
                commentManager.getComments(session, commentedDocModel.getId(), 3L, 1L, true));
        List<Comment> reversedComments = new ArrayList<>(comments);
        Collections.reverse(reversedComments);
        assertEquals(reversedComments.subList(3, 6),
                commentManager.getComments(session, commentedDocModel.getId(), 3L, 1L, false));
    }

    @Test
    public void testGetCommentsWithReply() {
        // give permission to comment to jdoe
        ACP acp = commentedDocModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("jdoe", SecurityConstants.READ, true));
        session.setACP(commentedDocModel.getRef(), acp, false);
        session.save();

        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);
        CoreSession jdoeSession = coreFeature.getCoreSession("jdoe");

        assertTrue(commentManager.getComments(jamesSession, commentedDocModel.getId()).isEmpty());
        assertTrue(commentManager.getComments(jdoeSession, commentedDocModel.getId()).isEmpty());

        Comment c1 = commentManager.createComment(jamesSession,
                newComment(commentedDocModel.getId(), "I am a comment!"));
        Comment c2 = commentManager.createComment(jdoeSession, newComment(c1.getId(), "And me a reply!"));
        Comment c3 = commentManager.createComment(jamesSession,
                newComment(commentedDocModel.getId(), "I am the last comment!"));
        Comment c4 = commentManager.createComment(jdoeSession, newComment(c1.getId(), "I am a sibling reply!"));
        // getComments uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();

        // now if we get comments, we should only get those of the first level (that means without replies)
        List<Comment> comments = commentManager.getComments(jamesSession, commentedDocModel.getId());
        assertEquals(2, comments.size());
        List<String> commentIds = comments.stream().map(Comment::getId).collect(toList());
        assertTrue(commentIds.remove(c1.getId()));
        assertTrue(commentIds.remove(c3.getId()));
        // check jdoe can do the same
        comments = commentManager.getComments(jdoeSession, commentedDocModel.getId());
        assertEquals(2, comments.size());

        // check we can get replies
        List<Comment> replies = commentManager.getComments(jamesSession, c1.getId());
        assertEquals(2, replies.size());
        List<String> repliesIds = replies.stream().map(Comment::getId).collect(toList());
        assertTrue(repliesIds.remove(c2.getId()));
        assertTrue(repliesIds.remove(c4.getId()));
        // check jdoe can do the same
        replies = commentManager.getComments(jdoeSession, c1.getId());
        assertEquals(2, replies.size());

        CoreSession janeSession = coreFeature.getCoreSession("jane");
        // check jane can not get comments
        try {
            commentManager.getComment(janeSession, commentedDocModel.getId());
            fail("jane should not be able to get comment");
        } catch (CommentSecurityException cse) {
            // ok
        }
        try {
            commentManager.getComment(janeSession, c1.getId());
            fail("jane should not be able to get comment");
        } catch (CommentSecurityException cse) {
            // ok
        }
    }

    @Test
    public void testUpdateComment() {
        Comment comment = newComment(commentedDocModel.getId(), "I am a comment!");
        comment = commentManager.createComment(session, comment);

        try {
            commentManager.updateComment(session, "fakeId", emptyComment());
            fail("Updating a comment should have failed !");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        Comment updatedComment = emptyComment();
        updatedComment.setText("This a new text on this comment");
        updatedComment = commentManager.updateComment(session, comment.getId(), updatedComment);
        assertEquals(comment.getId(), updatedComment.getId());
        assertEquals("Administrator", updatedComment.getAuthor());
        assertEquals("This a new text on this comment", updatedComment.getText());
        assertEquals(commentedDocModel.getId(), updatedComment.getParentId());
        assertEquals(comment.getCreationDate(), updatedComment.getCreationDate());
        assertNotNull(updatedComment.getModificationDate());
        assertTrue(comment.getModificationDate().isBefore(updatedComment.getModificationDate()));
    }

    @Test
    public void testUpdateCommentWithModificationDate() {
        Comment comment = newComment(commentedDocModel.getId(), "I am a comment!");
        comment = commentManager.createComment(session, comment);

        Instant modificationDate = Instant.parse("2020-04-25T10:35:10.00Z");
        Comment updatedComment = emptyComment();
        updatedComment.setModificationDate(modificationDate);
        updatedComment = commentManager.updateComment(session, comment.getId(), updatedComment);
        assertEquals(comment.getId(), updatedComment.getId());
        assertEquals("Administrator", updatedComment.getAuthor());
        assertEquals("I am a comment!", updatedComment.getText());
        assertEquals(commentedDocModel.getId(), updatedComment.getParentId());
        assertEquals(comment.getCreationDate(), updatedComment.getCreationDate());
        assertEquals(modificationDate, updatedComment.getModificationDate());
    }

    @Test
    public void testUpdateCommentByItsAuthor() {
        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);
        Comment comment = newComment(commentedDocModel.getId(), "I am a comment!");
        comment = commentManager.createComment(jamesSession, comment);

        // give permission to comment to jdoe
        ACP acp = commentedDocModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("jdoe", SecurityConstants.READ, true));
        session.setACP(commentedDocModel.getRef(), acp, false);
        session.save();

        try {
            // check only author can update its comment
            CoreSession jdoeSession = coreFeature.getCoreSession("jdoe");
            commentManager.updateComment(jdoeSession, comment.getId(), emptyComment());
        } catch (CommentSecurityException e) {
            // ok
        }

        // check regular update
        Comment updatedComment = emptyComment();
        updatedComment.setText("This a new text on this comment");
        updatedComment = commentManager.updateComment(jamesSession, comment.getId(), updatedComment);
        assertEquals(comment.getId(), updatedComment.getId());
        assertEquals(JAMES, updatedComment.getAuthor());
        assertEquals("This a new text on this comment", updatedComment.getText());
        assertEquals(commentedDocModel.getId(), updatedComment.getParentId());
        assertEquals(comment.getCreationDate(), updatedComment.getCreationDate());
        assertNotNull(updatedComment.getModificationDate());
        assertTrue(comment.getModificationDate().isBefore(updatedComment.getModificationDate()));
    }

    @Test
    public void testUpdateCommentByPowerfulUser() {
        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);
        Comment comment = newComment(commentedDocModel.getId(), "I am a comment!");
        comment = commentManager.createComment(jamesSession, comment);

        // give permission to do everything to julia
        ACP acp = commentedDocModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("julia", SecurityConstants.EVERYTHING, true));
        session.setACP(commentedDocModel.getRef(), acp, false);
        session.save();

        CoreSession juliaSession = coreFeature.getCoreSession("julia");
        Comment updatedComment = emptyComment();
        updatedComment.setText("This a new text on this comment");
        updatedComment = commentManager.updateComment(juliaSession, comment.getId(), updatedComment);
        assertEquals(comment.getId(), updatedComment.getId());
        // check author doesn't change
        assertEquals(JAMES, updatedComment.getAuthor());
        assertEquals("This a new text on this comment", updatedComment.getText());
        assertEquals(commentedDocModel.getId(), updatedComment.getParentId());
        assertEquals(comment.getCreationDate(), updatedComment.getCreationDate());
        assertNotNull(updatedComment.getModificationDate());
        assertTrue(comment.getModificationDate().isBefore(updatedComment.getModificationDate()));
    }

    @Test
    public void testUpdateReply() {
        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);

        Comment c1 = commentManager.createComment(jamesSession,
                newComment(commentedDocModel.getId(), "I am a comment!"));
        Comment c2 = commentManager.createComment(jamesSession, newComment(c1.getId(), "I am a reply!"));
        Comment c3 = commentManager.createComment(jamesSession, newComment(c2.getId(), "Me too!"));

        // check james can update its replies
        Comment updatedComment = emptyComment();
        updatedComment.setText("This a new text on this reply");
        updatedComment = commentManager.updateComment(jamesSession, c2.getId(), updatedComment);
        assertEquals(c2.getId(), updatedComment.getId());
        assertEquals(JAMES, updatedComment.getAuthor());
        assertEquals("This a new text on this reply", updatedComment.getText());
        assertEquals(c1.getId(), updatedComment.getParentId());
        assertEquals(c2.getCreationDate(), updatedComment.getCreationDate());
        assertNotNull(updatedComment.getModificationDate());
        assertTrue(c2.getModificationDate().isBefore(updatedComment.getModificationDate()));

        updatedComment = emptyComment();
        updatedComment.setText("I am just a reply!");
        updatedComment = commentManager.updateComment(jamesSession, c3.getId(), updatedComment);
        assertEquals(c3.getId(), updatedComment.getId());
        assertEquals(JAMES, updatedComment.getAuthor());
        assertEquals("I am just a reply!", updatedComment.getText());
        assertEquals(c2.getId(), updatedComment.getParentId());
        assertEquals(c3.getCreationDate(), updatedComment.getCreationDate());
        assertNotNull(updatedComment.getModificationDate());
        assertTrue(c3.getModificationDate().isBefore(updatedComment.getModificationDate()));

        CoreSession janeSession = coreFeature.getCoreSession("jane");

        // check jane can not update james replies
        try {
            commentManager.updateComment(janeSession, c2.getId(), emptyComment());
            fail("jane should not be able to update comment");
            // TODO CommentNotFoundException came from bridge, it should be that
        } catch (CommentSecurityException | CommentNotFoundException cse) {
            // ok
        }
        try {
            commentManager.updateComment(janeSession, c3.getId(), emptyComment());
            fail("jane should not be able to update comment");
            // TODO CommentNotFoundException came from bridge, it should be that
        } catch (CommentSecurityException | CommentNotFoundException cse) {
            // ok
        }
    }

    @Test
    public void testDeleteComment() {
        Comment comment = newComment(commentedDocModel.getId(), "I am a comment!");
        comment = commentManager.createComment(session, comment);
        assertTrue(session.exists(new IdRef(comment.getId())));

        try {
            commentManager.deleteComment(session, "fakeId");
            fail("Deleting a comment should have failed !");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        commentManager.deleteComment(session, comment.getId());
        assertFalse(session.exists(new IdRef(comment.getId())));
    }

    @Test
    public void testDeleteCommentByItsAuthor() {
        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);
        Comment comment = newComment(commentedDocModel.getId(), "I am a comment!");
        comment = commentManager.createComment(jamesSession, comment);
        assertTrue(session.exists(new IdRef(comment.getId())));

        // give permission to comment to jdoe
        ACP acp = commentedDocModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("jdoe", SecurityConstants.READ, true));
        session.setACP(commentedDocModel.getRef(), acp, false);
        session.save();

        try {
            // check only author can delete its comment
            CoreSession jdoeSession = coreFeature.getCoreSession("jdoe");
            commentManager.deleteComment(jdoeSession, comment.getId());
        } catch (CommentSecurityException e) {
            // ok
        }

        // check regular delete
        commentManager.deleteComment(jamesSession, comment.getId());
        assertFalse(session.exists(new IdRef(comment.getId())));
    }

    @Test
    public void testDeleteCommentByPowerfulUser() {
        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);
        Comment comment = newComment(commentedDocModel.getId(), "I am a comment!");
        comment = commentManager.createComment(jamesSession, comment);
        assertTrue(session.exists(new IdRef(comment.getId())));

        // give permission to do everything to julia
        ACP acp = commentedDocModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("julia", SecurityConstants.EVERYTHING, true));
        session.setACP(commentedDocModel.getRef(), acp, false);
        session.save();

        CoreSession juliaSession = coreFeature.getCoreSession("julia");
        commentManager.deleteComment(juliaSession, comment.getId());
        assertFalse(session.exists(new IdRef(comment.getId())));
    }

    @Test
    public void testDeleteReply() {
        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);
        CoreSession janeSession = coreFeature.getCoreSession("jane");

        Comment c1 = commentManager.createComment(jamesSession,
                newComment(commentedDocModel.getId(), "I am a comment!"));
        Comment c2 = commentManager.createComment(jamesSession, newComment(c1.getId(), "I am a reply!"));
        Comment c3 = commentManager.createComment(jamesSession, newComment(c2.getId(), "Me too!"));

        assertTrue(session.exists(new IdRef(c1.getId())));
        assertTrue(session.exists(new IdRef(c2.getId())));
        assertTrue(session.exists(new IdRef(c3.getId())));

        // check jane can not delete james replies
        try {
            commentManager.deleteComment(janeSession, c2.getId());
            fail("jane should not be able to delete comment");
            // TODO CommentNotFoundException came from bridge, it should be that
        } catch (CommentSecurityException | CommentNotFoundException cse) {
            // ok
        }
        try {
            commentManager.deleteComment(janeSession, c3.getId());
            fail("jane should not be able to delete comment");
            // TODO CommentNotFoundException came from bridge, it should be that
        } catch (CommentSecurityException | CommentNotFoundException cse) {
            // ok
        }

        // check james can delete its first reply
        commentManager.deleteComment(session, c2.getId());
        assertFalse(session.exists(new IdRef(c2.getId())));
        // wait for replies cleanup
        transactionalFeature.nextTransaction();
        assertFalse(session.exists(new IdRef(c3.getId())));
    }

    // ------------------------------
    // CRUD tests on external entity
    // ------------------------------

    @Test
    public void testCreateExternalComment() {
        String text = "I am an external comment!";
        String entity = "<entity><content>Some content</content></entity>";
        // check regular creation
        Comment comment = newExternalComment(commentedDocModel.getId(), "entityId", entity, text);
        comment = commentManager.createComment(session, comment);
        assertEquals("Administrator", comment.getAuthor());
        assertEquals(text, comment.getText());
        assertEquals(commentedDocModel.getId(), comment.getParentId());
        assertNotNull(comment.getCreationDate());
        assertNotNull(comment.getModificationDate());
        var externalEntity = (ExternalEntity) comment;
        assertEquals("entityId", externalEntity.getEntityId());
        assertEquals(entity, externalEntity.getEntity());
        assertEquals("Test", externalEntity.getOrigin());
    }

    @Test
    public void testGetExternalComment() {
        String text = "I am an external comment!";
        String entity = "<entity><content>Some content</content></entity>";
        commentManager.createComment(session, newExternalComment(commentedDocModel.getId(), "entityId", entity, text));
        // external comment uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();

        try {
            commentManager.getExternalComment(session, "fakeId", "entityId");
            fail("Getting an external comment should have failed !");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }
        try {
            commentManager.getExternalComment(session, commentedDocModel.getId(), "fakeId");
            fail("Getting an external comment should have failed !");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        // check regular get
        Comment comment = commentManager.getExternalComment(session, commentedDocModel.getId(), "entityId");
        assertEquals("Administrator", comment.getAuthor());
        assertEquals(text, comment.getText());
        assertEquals(commentedDocModel.getId(), comment.getParentId());
        var externalEntity = (ExternalEntity) comment;
        assertEquals("entityId", externalEntity.getEntityId());
        assertEquals(entity, externalEntity.getEntity());
        assertEquals("Test", externalEntity.getOrigin());
    }

    // exist to be overridden by implementation having a different permissions check
    @Test
    public void testGetExternalCommentPermissions() {
        String text = "I am an external comment!";
        String entity = "<entity><content>Some content</content></entity>";
        var comment = commentManager.createComment(session,
                newExternalComment(commentedDocModel.getId(), "entityId", entity, text));
        // external comment uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();
        assertEquals("Administrator", comment.getAuthor());

        // check james can get (because he has READ on commentedDocModel)
        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);
        commentManager.getExternalComment(jamesSession, commentedDocModel.getId(), "entityId");

        try {
            // check jane can not get
            CoreSession janeSession = coreFeature.getCoreSession("jane");
            commentManager.getExternalComment(janeSession, commentedDocModel.getId(), "entityId");
            fail("jane should not be able to get comment");
        } catch (CommentSecurityException | CommentNotFoundException cse) {
            // ok
        }
    }

    @Test
    public void testUpdateExternalComment() {
        String entity = "<entity><content>Some content</content></entity>";
        // check regular creation
        Comment comment = newExternalComment(commentedDocModel.getId(), "entityId", entity,
                "I am an external comment!");
        comment = commentManager.createComment(session, comment);
        // external comment uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();

        try {
            commentManager.updateExternalComment(session, "fakeId", "entityId", emptyComment());
            fail("Updating a comment should have failed !");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }
        try {
            commentManager.updateExternalComment(session, commentedDocModel.getId(), "fakeId", emptyComment());
            fail("Updating a comment should have failed !");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        Comment updatedComment = emptyComment();
        updatedComment.setText("This a new text on this comment");
        ((ExternalEntity) updatedComment).setEntity("<entity/>");
        updatedComment = commentManager.updateExternalComment(session, commentedDocModel.getId(), "entityId",
                updatedComment);
        assertEquals(comment.getId(), updatedComment.getId());
        assertEquals("Administrator", updatedComment.getAuthor());
        assertEquals("This a new text on this comment", updatedComment.getText());
        assertEquals(commentedDocModel.getId(), updatedComment.getParentId());
        assertEquals(comment.getCreationDate(), updatedComment.getCreationDate());
        assertNotNull(updatedComment.getModificationDate());
        assertTrue(comment.getModificationDate().isBefore(updatedComment.getModificationDate()));
        var externalEntity = (ExternalEntity) comment;
        var updatedExternalEntity = (ExternalEntity) updatedComment;
        assertEquals(externalEntity.getEntityId(), updatedExternalEntity.getEntityId());
        assertEquals(externalEntity.getOrigin(), updatedExternalEntity.getOrigin());
        assertEquals("<entity/>", updatedExternalEntity.getEntity());
    }

    @Test
    public void testUpdateExternalCommentByItsAuthor() {
        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);
        String entity = "<entity><content>Some content</content></entity>";
        Comment comment = newExternalComment(commentedDocModel.getId(), "entityId", entity,
                "I am an external comment!");
        comment = commentManager.createComment(jamesSession, comment);
        // external comment uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();

        // give permission to comment to jdoe
        ACP acp = commentedDocModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("jdoe", SecurityConstants.READ, true));
        session.setACP(commentedDocModel.getRef(), acp, false);
        session.save();

        try {
            // check only author can update its comment
            CoreSession jdoeSession = coreFeature.getCoreSession("jdoe");
            commentManager.updateExternalComment(jdoeSession, commentedDocModel.getId(), "entityId", emptyComment());
        } catch (CommentSecurityException e) {
            // ok
        }

        // check regular update
        Comment updatedComment = emptyComment();
        updatedComment.setText("This a new text on this comment");
        ((ExternalEntity) updatedComment).setEntity("<entity/>");
        updatedComment = commentManager.updateExternalComment(jamesSession, commentedDocModel.getId(), "entityId",
                updatedComment);
        assertEquals(comment.getId(), updatedComment.getId());
        assertEquals(JAMES, updatedComment.getAuthor());
        assertEquals("This a new text on this comment", updatedComment.getText());
        assertEquals(commentedDocModel.getId(), updatedComment.getParentId());
        assertEquals(comment.getCreationDate(), updatedComment.getCreationDate());
        assertNotNull(updatedComment.getModificationDate());
        assertTrue(comment.getModificationDate().isBefore(updatedComment.getModificationDate()));
        var externalEntity = (ExternalEntity) comment;
        var updatedExternalEntity = (ExternalEntity) updatedComment;
        assertEquals(externalEntity.getEntityId(), updatedExternalEntity.getEntityId());
        assertEquals(externalEntity.getOrigin(), updatedExternalEntity.getOrigin());
        assertEquals("<entity/>", updatedExternalEntity.getEntity());
    }

    @Test
    public void testUpdateExternalCommentByPowerfulUser() {
        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);
        String entity = "<entity><content>Some content</content></entity>";
        Comment comment = newExternalComment(commentedDocModel.getId(), "entityId", entity,
                "I am an external comment!");
        comment = commentManager.createComment(jamesSession, comment);
        // external comment uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();

        // give permission to do everything to julia
        ACP acp = commentedDocModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("julia", SecurityConstants.EVERYTHING, true));
        session.setACP(commentedDocModel.getRef(), acp, false);
        session.save();

        CoreSession juliaSession = coreFeature.getCoreSession("julia");
        Comment updatedComment = emptyComment();
        updatedComment.setText("This a new text on this comment");
        ((ExternalEntity) updatedComment).setEntity("<entity/>");
        updatedComment = commentManager.updateExternalComment(juliaSession, commentedDocModel.getId(), "entityId",
                updatedComment);
        assertEquals(comment.getId(), updatedComment.getId());
        assertEquals(JAMES, updatedComment.getAuthor());
        assertEquals("This a new text on this comment", updatedComment.getText());
        assertEquals(commentedDocModel.getId(), updatedComment.getParentId());
        assertEquals(comment.getCreationDate(), updatedComment.getCreationDate());
        assertNotNull(updatedComment.getModificationDate());
        assertTrue(comment.getModificationDate().isBefore(updatedComment.getModificationDate()));
        var externalEntity = (ExternalEntity) comment;
        var updatedExternalEntity = (ExternalEntity) updatedComment;
        assertEquals(externalEntity.getEntityId(), updatedExternalEntity.getEntityId());
        assertEquals(externalEntity.getOrigin(), updatedExternalEntity.getOrigin());
        assertEquals("<entity/>", updatedExternalEntity.getEntity());
    }

    @Test
    public void testDeleteExternalComment() {
        var comment = commentManager.createComment(session, newExternalComment(commentedDocModel.getId(), "entityId"));
        assertTrue(session.exists(new IdRef(comment.getId())));
        // external comment uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();

        try {
            commentManager.deleteExternalComment(session, "fakeId", "entityId");
            fail("Deleting a comment should have failed !");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        try {
            commentManager.deleteExternalComment(session, commentedDocModel.getId(), "fakeId");
            fail("Deleting a comment should have failed !");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        commentManager.deleteExternalComment(session, commentedDocModel.getId(), "entityId");
        assertFalse(session.exists(new IdRef(comment.getId())));
    }

    /*
     * NXP-28964
     */
    @Test
    public void testExternalCommentOnVersion() {
        // first create a version
        DocumentRef versionRef = commentedDocModel.checkIn(VersioningOption.MINOR, "checkin comment");
        String versionId = versionRef.reference().toString();
        // then create an external comment
        commentManager.createComment(session, newExternalComment(versionId, "foo", "<entity/>", "I am a comment!"));
        // external comment uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();

        var externalComment = commentManager.getExternalComment(session, versionId, "foo");
        assertEquals(versionId, externalComment.getParentId());

        externalComment.setText("Updated text");
        externalComment = commentManager.updateExternalComment(session, versionId, "foo", externalComment);
        assertEquals("Updated text", externalComment.getText());

        // external comment uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();
        externalComment = commentManager.getExternalComment(session, versionId, "foo");
        assertEquals("Updated text", externalComment.getText());

        commentManager.deleteExternalComment(session, versionId, "foo");
        assertFalse(session.exists(new IdRef(externalComment.getId())));
    }

    @Test
    public void testDeleteExternalCommentByItsAuthor() {
        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);
        Comment comment = newExternalComment(commentedDocModel.getId(), "entityId");
        comment = commentManager.createComment(jamesSession, comment);
        assertTrue(session.exists(new IdRef(comment.getId())));
        // external comment uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();

        // give permission to comment to jdoe
        ACP acp = commentedDocModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("jdoe", SecurityConstants.READ, true));
        session.setACP(commentedDocModel.getRef(), acp, false);
        session.save();

        try {
            // check only author can delete its comment
            CoreSession jdoeSession = coreFeature.getCoreSession("jdoe");
            commentManager.deleteExternalComment(jdoeSession, commentedDocModel.getId(), "entityId");
        } catch (CommentSecurityException e) {
            // ok
        }

        // check regular delete
        commentManager.deleteExternalComment(jamesSession, commentedDocModel.getId(), "entityId");
        assertFalse(session.exists(new IdRef(comment.getId())));
    }

    @Test
    public void testDeleteExternalCommentByPowerfulUser() {
        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);
        Comment comment = newExternalComment(commentedDocModel.getId(), "entityId");
        comment = commentManager.createComment(jamesSession, comment);
        assertTrue(session.exists(new IdRef(comment.getId())));
        // external comment uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();

        // give permission to do everything to julia
        ACP acp = commentedDocModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("julia", SecurityConstants.EVERYTHING, true));
        session.setACP(commentedDocModel.getRef(), acp, false);
        session.save();

        CoreSession juliaSession = coreFeature.getCoreSession("julia");
        commentManager.deleteExternalComment(juliaSession, commentedDocModel.getId(), "entityId");
        assertFalse(session.exists(new IdRef(comment.getId())));
    }

    @Test
    public void testCommentsAncestorIds() {
        Comment c1 = commentManager.createComment(session, newComment(commentedDocModel.getId(), "I am a comment!"));
        Comment c2 = commentManager.createComment(session, newComment(c1.getId(), "I am a reply!"));
        Comment c3 = commentManager.createComment(session, newComment(c2.getId(), "Me too!"));

        assertEquals(Set.of(commentedDocModel.getId()), new HashSet<>(c1.getAncestorIds()));
        assertEquals(Set.of(commentedDocModel.getId(), c1.getId()), new HashSet<>(c2.getAncestorIds()));
        assertEquals(Set.of(commentedDocModel.getId(), c1.getId(), c2.getId()), new HashSet<>(c3.getAncestorIds()));
    }

    @Test
    public void testGetTopLevelDocumentRef() {
        Comment comment = newComment(commentedDocModel.getId(), "I am a comment!");
        comment = commentManager.createComment(session, comment);

        DocumentRef commentedDocRef = commentedDocModel.getRef();
        DocumentRef commentRef = new IdRef(comment.getId());
        // check creator can get top level document ref
        assertEquals(commentedDocRef, commentManager.getTopLevelDocumentRef(session, commentRef));

        // check james can get top level document ref (because he has READ on commentedDocModel)
        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);
        assertEquals(commentedDocRef, commentManager.getTopLevelDocumentRef(jamesSession, commentRef));

        try {
            // check jane can not get top level document ref
            CoreSession janeSession = coreFeature.getCoreSession("jane");
            commentManager.getTopLevelDocumentRef(janeSession, commentRef);
            fail("jane should not be able to get the top level comment ancestor");
        } catch (CommentSecurityException cse) {
            assertNotNull(cse);
            assertEquals(
                    String.format("The user jane does not have access to the comments of document %s", commentedDocRef),
                    cse.getMessage());
        }
    }

    @Test
    public void testGetTopLevelDocumentRefFromSubReply() {
        String text = "I am a comment!";
        Comment comment = newComment(commentedDocModel.getId(), text);
        comment = commentManager.createComment(session, comment);

        // Add a reply
        Comment reply = newComment(comment.getId(), "I am a reply");
        reply = commentManager.createComment(session, reply);

        // Another reply
        Comment anotherReply = newComment(comment.getId(), "I am a 2nd reply");
        anotherReply = commentManager.createComment(session, anotherReply);

        DocumentRef commentedDocRef = commentedDocModel.getRef();
        DocumentRef commentRef = new IdRef(anotherReply.getId());
        // check creator can get top level document ref
        assertEquals(commentedDocRef, commentManager.getTopLevelDocumentRef(session, commentRef));

        // check james can get top level document ref (because he has READ on commentedDocModel)
        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);
        assertEquals(commentedDocRef, commentManager.getTopLevelDocumentRef(jamesSession, commentRef));

        try {
            // check jane can not get top level document ref
            CoreSession janeSession = coreFeature.getCoreSession("jane");
            commentManager.getTopLevelDocumentRef(janeSession, commentRef);
            fail("jane should not be able to get the top level comment ancestor");
        } catch (CommentSecurityException cse) {
            assertNotNull(cse);
            assertEquals(
                    String.format("The user jane does not have access to the comments of document %s", commentedDocRef),
                    cse.getMessage());
        }
    }

    /*
     * NXP-28719
     */
    @Test
    public void testCreateCommentUnderPlacelessDocument() {
        DocumentModel placeless = session.createDocumentModel(null, "placeless", "File");
        placeless = session.createDocument(placeless);
        transactionalFeature.nextTransaction();

        // first comment
        String text = "I am a comment!";
        Comment comment = newComment(placeless.getId(), text);
        comment = commentManager.createComment(session, comment);
        assertEquals("Administrator", comment.getAuthor());
        assertEquals(text, comment.getText());
        assertEquals(placeless.getId(), comment.getParentId());
    }

    /*
     * NXP-28719
     */
    @Test
    public void testCreateRepliesUnderPlacelessDocument() {
        DocumentModel placeless = session.createDocumentModel(null, "placeless", "File");
        placeless = session.createDocument(placeless);
        transactionalFeature.nextTransaction();

        // first comment
        Comment comment = newComment(placeless.getId(), "I am a comment!");
        comment = commentManager.createComment(session, comment);

        // a reply
        Comment reply = newComment(comment.getId(), "I am a reply!");
        reply = commentManager.createComment(session, reply);
        assertEquals("Administrator", reply.getAuthor());
        assertEquals("I am a reply!", reply.getText());
        assertEquals(comment.getId(), reply.getParentId());

        // another reply
        Comment reply2 = newComment(reply.getId(), "I am a sub reply !");
        reply2 = commentManager.createComment(session, reply2);
        assertEquals("Administrator", reply2.getAuthor());
        assertEquals("I am a sub reply !", reply2.getText());
        assertEquals(reply.getId(), reply2.getParentId());
    }

}
