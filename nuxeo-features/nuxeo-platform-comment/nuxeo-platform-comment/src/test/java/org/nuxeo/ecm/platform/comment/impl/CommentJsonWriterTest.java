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
 *     Nuno Cunha <ncunha@nuxeo.com>
 */

package org.nuxeo.ecm.platform.comment.impl;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, MockitoFeature.class })
public class CommentJsonWriterTest extends AbstractJsonWriterTest.External<CommentJsonWriter, Comment> {

    @Mock
    @RuntimeService
    protected CommentManager commentManager;

    @Inject
    protected CoreSession session;

    protected RenderingContext context;

    protected Comment comment;

    protected List<Comment> replies;

    public CommentJsonWriterTest() {
        super(CommentJsonWriter.class, Comment.class);
    }

    @Before
    public void setUp() {
        comment = getComment("commentId", "parentFileId", "author", "main comment", false, emptyList());
        comment.addAncestorId("parentFileId");

        replies = new ArrayList<>();
        replies.add(getComment("firstReplyId", comment.getId(), "chronicler", "first reply", false,
                singletonList("parentFileId")));
        replies.add(getComment("secondReplyId", comment.getId(), "author", "second reply", false,
                singletonList("parentFileId")));
        replies.add(getComment("thirdReplyId", comment.getId(), "chronicler", "third reply", true,
                singletonList("parentFileId")));

        context = RenderingContext.CtxBuilder.session(session).fetch("comment", "repliesSummary").get();
    }

    @Test
    public void shouldWriteDefaultPropertiesWhenNoFetchersAreProvided() throws IOException {
        JsonAssert json = jsonAssert(comment);
        verify(commentManager, never()).getComments(any(CoreSession.class), anyString(), eq(1L), eq(0L), eq(false));
        assertCommentProperties(json);
    }

    @Test
    public void shouldWriteCompleteRepliesSummaryWhenRepliesFetcherIsProvided() throws IOException {
        Collections.reverse(replies);
        when(commentManager.getComments(any(CoreSession.class), eq(comment.getId()), eq(1L), eq(0L),
                eq(false))).thenReturn(new PartialList<>(replies, replies.size()));

        JsonAssert json = jsonAssert(comment, context);
        verify(commentManager).getComments(any(CoreSession.class), eq(comment.getId()), eq(1L), eq(0L), eq(false));
        assertCommentProperties(json);
        assertRepliesSummary(json, true);
    }

    @Test
    public void shouldWriteRepliesSummaryWithoutLastReplyWhenRepliesFetcherIsProvidedButThereAreNoReplies()
            throws IOException {
        PartialList<Comment> returnedComments = new PartialList<>(emptyList(), 0);

        when(commentManager.getComments(any(), eq(replies.get(0).getId()), eq(1L), eq(0L), eq(false))).thenReturn(
                returnedComments);

        JsonAssert json = jsonAssert(replies.get(0), context);
        verify(commentManager).getComments(any(CoreSession.class), eq(replies.get(0).getId()), eq(1L), eq(0L),
                eq(false));
        assertCommentProperties(json);
        assertRepliesSummary(json, false);
    }

    @Test
    public void shouldWriteCorrectInfoWhenCommentHasRepliesAndFetcherIsProvided() throws IOException {
        Collections.reverse(replies);
        when(commentManager.getComments(any(CoreSession.class), eq(comment.getId()), eq(1L), eq(0L),
                eq(false))).thenReturn(new PartialList<>(replies, replies.size()));

        JsonAssert json = jsonAssert(comment, context);
        verify(commentManager).getComments(any(CoreSession.class), eq(comment.getId()), eq(1L), eq(0L), eq(false));
        assertCommentProperties(json);
        assertRepliesSummary(json, true);

        json.has("id").isEquals(comment.getId());
        json.has("parentId").isEquals(comment.getParentId());
        json.has("ancestorIds").length(1);
        json.has("author").isEquals(comment.getAuthor());
        json.has("text").isEquals(comment.getText());
        json.has("creationDate").isEquals(comment.getCreationDate().toString());
        json.has("modificationDate").isEmptyStringOrNull();
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

        JsonAssert json = jsonAssert(lastReply);
        verify(commentManager, never()).getComments(any(CoreSession.class), anyString(), eq(1L), eq(0L), eq(false));
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
    }

    protected void assertRepliesSummary(JsonAssert json, boolean isLastReplyAvailable) throws IOException {
        json.has("numberOfReplies").isInt();
        if (isLastReplyAvailable) {
            json.has("lastReplyDate").isText();
        } else {
            json.hasNot("lasReplyDate");
        }
    }

    protected Comment getComment(String id, String parentId, String author, String text, boolean isEntity,
            List<String> ancestorIds) {
        Comment comment = new CommentImpl();
        comment.setId(id);
        comment.setParentId(parentId);
        comment.addAncestorId(parentId);
        comment.setAuthor(author);
        comment.setText(text);
        comment.setCreationDate(Instant.now());
        if (isEntity) {
            ((CommentImpl) comment).setEntity("entity");
            ((CommentImpl) comment).setEntityId("entityId");
            ((CommentImpl) comment).setOrigin("entityOrigin");
        }
        ancestorIds.forEach(comment::addAncestorId);
        return comment;
    }

}