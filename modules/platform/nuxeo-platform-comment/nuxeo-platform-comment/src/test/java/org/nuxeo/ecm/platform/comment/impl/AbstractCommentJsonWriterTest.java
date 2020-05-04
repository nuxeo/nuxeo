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
 *     Nuno Cunha <ncunha@nuxeo.com>
 */

package org.nuxeo.ecm.platform.comment.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newComment;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.platform.comment.CommentFeature;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(CommentFeature.class)
public abstract class AbstractCommentJsonWriterTest
        extends AbstractJsonWriterTest.External<CommentJsonWriter, Comment> {

    @Inject
    protected CommentManager commentManager;

    @Inject
    protected CoreSession session;

    protected DocumentModel docModel;

    protected Comment comment;

    protected List<Comment> replies;

    protected abstract Class<? extends CommentManager> getCommentManager();

    public AbstractCommentJsonWriterTest() {
        super(CommentJsonWriter.class, Comment.class);
    }

    @Before
    public void setUp() {

        DocumentModel doc = session.createDocumentModel("/", "myDoc", "File");
        docModel = session.createDocument(doc);
        session.save();

        comment = commentManager.createComment(session, newComment(docModel.getId(), "main comment"));

        replies = new ArrayList<>();
        Instant date = Instant.now();
        Comment firstReply = newComment(comment.getId(), "first reply");
        firstReply.setCreationDate(date);

        Comment secondReply = newComment(comment.getId(), "second reply");
        secondReply.setCreationDate(date.plusSeconds(1));

        Comment thirdReply = newComment(comment.getId(), "third reply");
        thirdReply.setCreationDate(date.plusSeconds(2));
        ((CommentImpl) thirdReply).setOrigin("origin");
        ((CommentImpl) thirdReply).setEntity("entity");
        ((CommentImpl) thirdReply).setEntityId("entityId");

        replies.add(commentManager.createComment(session, firstReply));
        replies.add(commentManager.createComment(session, secondReply));
        replies.add(commentManager.createComment(session, thirdReply));

        ACP acp = doc.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("james", SecurityConstants.READ, true));
        session.setACP(doc.getRef(), acp, false);

        session.save();

    }

    @Test
    public void shouldWriteDefaultPropertiesWhenNoFetchersAreProvided() throws IOException {
        RenderingContext ctx = RenderingContext.CtxBuilder.session(session).get();
        JsonAssert json = jsonAssert(comment, ctx);
        assertCommentProperties(json);
    }

    @Test
    public void shouldWriteCompleteRepliesSummaryWhenRepliesFetcherIsProvided() throws IOException {
        RenderingContext ctx = RenderingContext.CtxBuilder.session(session).fetch("comment", "repliesSummary").get();
        JsonAssert json = jsonAssert(comment, ctx);
        assertCommentProperties(json);
        assertRepliesSummary(json, true);
    }

    @Test
    public void shouldWriteRepliesSummaryWithoutLastReplyWhenRepliesFetcherIsProvidedButThereAreNoReplies()
            throws IOException {
        RenderingContext ctx = RenderingContext.CtxBuilder.session(session).fetch("comment", "repliesSummary").get();
        JsonAssert json = jsonAssert(replies.get(0), ctx);
        assertCommentProperties(json);
        assertRepliesSummary(json, false);
    }

    @Test
    public void shouldWriteCorrectInfoWhenCommentHasRepliesAndFetcherIsProvided() throws IOException {
        RenderingContext ctx = RenderingContext.CtxBuilder.session(session).fetch("comment", "repliesSummary").get();
        JsonAssert json = jsonAssert(comment, ctx);
        assertCommentProperties(json);
        assertRepliesSummary(json, true);

        json.has("id").isEquals(comment.getId());
        json.has("parentId").isEquals(comment.getParentId());
        json.has("ancestorIds").length(1);
        json.has("author").isEquals(comment.getAuthor());
        json.has("text").isEquals(comment.getText());
        json.has("creationDate").isEquals(comment.getCreationDate().toString());
        json.has("modificationDate").isEquals(comment.getModificationDate().toString());
        json.has("entity").isEmptyStringOrNull();
        json.has("entityId").isEmptyStringOrNull();
        json.has("origin").isEmptyStringOrNull();
        json.has("numberOfReplies").isEquals(3);
        json.has("lastReplyDate").isEquals(replies.get(replies.size() - 1).getCreationDate().toString());
    }

    @Test
    public void shouldWriteCorrectInfoWhenNoFetcherAndCommentIsEntityWithoutReplies() throws IOException {
        Comment lastReply = replies.get(replies.size() - 1);
        lastReply.setModificationDate(Instant.now());

        RenderingContext ctx = RenderingContext.CtxBuilder.session(session).get();
        JsonAssert json = jsonAssert(lastReply, ctx);
        assertCommentProperties(json);

        json.has("id").isEquals(lastReply.getId());
        json.has("parentId").isEquals(lastReply.getParentId());
        json.has("ancestorIds").length(2);
        json.has("author").isEquals(lastReply.getAuthor());
        json.has("text").isEquals(lastReply.getText());
        json.has("creationDate").isEquals(lastReply.getCreationDate().toString());
        json.has("modificationDate").isEquals(lastReply.getModificationDate().toString());
        json.has("entity").isEquals(((CommentImpl) lastReply).getEntity());
        json.has("entityId").isEquals(((CommentImpl) lastReply).getEntityId());
        json.has("origin").isEquals(((CommentImpl) lastReply).getOrigin());
        json.hasNot("numberOfReplies");
        json.hasNot("lastReplyDate");
    }

    protected void assertCommentProperties(JsonAssert json) throws IOException {
        json.isObject();
        assertThat(json.getNode().size(), greaterThanOrEqualTo(11));
        json.has("entity-type").isEquals("comment");
        json.has("id").isText();
        json.has("parentId").isText();
        json.has("ancestorIds").isArray();
        json.has("author").isText();
        json.has("text").isText();
        json.has("creationDate").isText();
        json.has("modificationDate");
        json.has("entity");
        json.has("entityId");
        json.has("origin");
        json.has("permissions").isArray();
    }

    protected void assertRepliesSummary(JsonAssert json, boolean isLastReplyAvailable) throws IOException {
        json.has("numberOfReplies").isInt();
        if (isLastReplyAvailable) {
            json.has("lastReplyDate").isText();
        } else {
            json.hasNot("lasReplyDate");
        }
    }

    @Test
    public void testUsedCommentManager() {
        assertEquals(getCommentManager(), commentManager.getClass());
    }

    @Test
    public void shouldGetCommentsWhenIHaveRightPermissions() throws IOException {
        CoreSession jamesSession = CoreInstance.getCoreSession(docModel.getRepositoryName(), "james");
        Comment lastReply = replies.get(replies.size() - 1);
        lastReply.setModificationDate(Instant.now());

        RenderingContext ctx = RenderingContext.CtxBuilder.session(jamesSession).get();
        JsonAssert json = jsonAssert(lastReply, ctx);
        assertCommentProperties(json);

        json.has("id").isEquals(lastReply.getId());
        json.has("parentId").isEquals(lastReply.getParentId());
        json.has("ancestorIds").length(2);
        json.has("author").isEquals(lastReply.getAuthor());
        json.has("text").isEquals(lastReply.getText());
        json.has("creationDate").isEquals(lastReply.getCreationDate().toString());
        json.has("modificationDate").isEquals(lastReply.getModificationDate().toString());
        json.has("entity").isEquals(((CommentImpl) lastReply).getEntity());
        json.has("entityId").isEquals(((CommentImpl) lastReply).getEntityId());
        json.has("origin").isEquals(((CommentImpl) lastReply).getOrigin());
        json.hasNot("numberOfReplies");
        json.hasNot("lastReplyDate");
    }

}