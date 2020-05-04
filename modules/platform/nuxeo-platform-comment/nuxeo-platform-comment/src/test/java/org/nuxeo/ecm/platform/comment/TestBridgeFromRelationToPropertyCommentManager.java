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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.BridgeCommentManager;
import org.nuxeo.ecm.platform.comment.service.CommentService;
import org.nuxeo.ecm.platform.comment.service.CommentServiceConfig;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 11.1
 * @deprecated since 10.3, in order to follow the service deprecation
 *             {@link org.nuxeo.ecm.platform.comment.impl.CommentManagerImpl}.
 */
@Deprecated
@Features({ RelationCommentFeature.class, BridgeCommentFeature.class })
public class TestBridgeFromRelationToPropertyCommentManager extends AbstractTestCommentManager {

    @Inject
    @Named("first")
    protected CommentManager first;

    public TestBridgeFromRelationToPropertyCommentManager() {
        super(BridgeCommentManager.class);
    }

    @Test
    @Override
    @Ignore("PropertyCommentManager doesn't support update by powerful user")
    public void testUpdateCommentByPowerfulUser() {
    }

    @Test
    @Override
    @Ignore("BridgeCommentFeature + PropertyCommentManager don't clean [sub] reply on comment delete")
    public void testDeleteReply() {
    }

    @Test
    @Override
    @Ignore("PropertyCommentManager doesn't support update by powerful user")
    public void testUpdateExternalCommentByPowerfulUser() {
    }

    @Test
    public void testDeleteCommentAsRelation() {
        // use a deprecated API to create comment as it was
        DocumentModel commentDocModel = first.createComment(commentedDocModel, "I am a comment!");
        // check binding in new API
        Comment comment = first.getComment(session, commentDocModel.getId());
        assertNotNull(comment);

        // Ensure that this comment is correctly created as relation
        CommentService commentComponent = (CommentService) Framework.getRuntime().getComponent(CommentService.NAME);
        RelationManager relationManager = Framework.getService(RelationManager.class);
        CommentServiceConfig config = commentComponent.getConfig();
        assertNotNull(config);
        Resource commentRes = relationManager.getResource(config.commentNamespace, commentDocModel, null);
        assertNotNull(commentRes);
        Graph graph = relationManager.getGraph(config.graphName, commentDocModel.getCoreSession());
        Resource predicateRes = new ResourceImpl(config.predicateNamespace);
        assertTrue(graph.getObjects(commentRes, predicateRes).stream().findAny().isPresent());

        // check bridge can delete it
        commentManager.deleteComment(session, comment.getId());
        assertFalse(session.exists(new IdRef(comment.getId())));
    }
}
