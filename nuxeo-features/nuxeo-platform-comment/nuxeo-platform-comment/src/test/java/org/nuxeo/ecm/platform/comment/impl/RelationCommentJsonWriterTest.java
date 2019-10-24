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

package org.nuxeo.ecm.platform.comment.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;

import java.io.IOException;
import java.time.Instant;

import org.junit.Test;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 11.1
 */
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.relations.api")
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.ecm.platform.comment.tests:OSGI-INF/comment-jena-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.comment.tests:OSGI-INF/relation-comment-manager-override.xml")
public class RelationCommentJsonWriterTest extends AbstractCommentJsonWriterTest {

    @Test
    @Override
    public void shouldWriteCorrectInfoWhenCommentHasRepliesAndFetcherIsProvided() throws IOException {
        RenderingContext ctx = RenderingContext.CtxBuilder.session(session).fetch("comment", "repliesSummary").get();
        JsonAssert json = jsonAssert(comment, ctx);
        assertCommentProperties(json);
        assertRepliesSummary(json, true);

        json.has("id").isEquals(comment.getId());
        json.has("parentId").isEquals(comment.getParentId());
        json.has("ancestorIds").length(0);
        json.has("author").isEquals(comment.getAuthor());
        json.has("text").isEquals(comment.getText());
        json.has("entity").isEmptyStringOrNull();
        json.has("entityId").isEmptyStringOrNull();
        json.has("origin").isEmptyStringOrNull();
        json.has("numberOfReplies").isEquals(3);
    }

    @Test
    @Override
    public void shouldWriteCorrectInfoWhenNoFetcherAndCommentIsEntityWithoutReplies() throws IOException {
        Comment lastReply = replies.get(replies.size() - 1);
        lastReply.setModificationDate(Instant.now());

        RenderingContext ctx = RenderingContext.CtxBuilder.session(session).get();
        JsonAssert json = jsonAssert(lastReply, ctx);
        assertCommentProperties(json);

        json.has("id").isEquals(lastReply.getId());
        json.has("parentId").isEquals(lastReply.getParentId());
        json.has("ancestorIds").length(0);
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

    @Test
    @Override
    public void shouldGetCommentsWhenIHaveRightPermissions() throws IOException {
        try (CloseableCoreSession jamesSession = CoreInstance.openCoreSession(docModel.getRepositoryName(), "james")) {
            Comment lastReply = replies.get(replies.size() - 1);
            lastReply.setModificationDate(Instant.now());

            RenderingContext ctx = RenderingContext.CtxBuilder.session(jamesSession).get();
            JsonAssert json = jsonAssert(lastReply, ctx);
            assertCommentProperties(json);

            json.has("id").isEquals(lastReply.getId());
            json.has("parentId").isEquals(lastReply.getParentId());
            json.has("ancestorIds").length(0);
            json.has("author").isEquals(lastReply.getAuthor());
            json.has("text").isEquals(lastReply.getText());
            json.has("entity").isEquals(((CommentImpl) lastReply).getEntity());
            json.has("entityId").isEquals(((CommentImpl) lastReply).getEntityId());
            json.has("origin").isEquals(((CommentImpl) lastReply).getOrigin());
            json.hasNot("numberOfReplies");
            json.hasNot("lastReplyDate");
        }
    }

    @Override
    protected void assertCommentProperties(JsonAssert json) throws IOException {
        json.isObject();
        assertThat(json.getNode().size(), greaterThanOrEqualTo(11));
        json.has("entity-type").isEquals("comment");
        json.has("id").isText();
        json.has("parentId").isText();
        json.has("ancestorIds").isArray();
        json.has("author").isText();
        json.has("text").isText();
        json.has("entity");
        json.has("entityId");
        json.has("origin");
        json.has("permissions").isArray();
    }

    @Override
    protected Class<? extends CommentManager> getCommentManager() {
        return CommentManagerImpl.class;
    }
}
