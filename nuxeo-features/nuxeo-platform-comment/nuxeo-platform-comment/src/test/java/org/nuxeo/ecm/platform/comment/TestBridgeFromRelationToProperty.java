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

package org.nuxeo.ecm.platform.comment;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentNotFoundException;
import org.nuxeo.ecm.platform.comment.impl.BridgeCommentManager;
import org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager;
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
@Features(RelationCommentFeature.class)
public class TestBridgeFromRelationToProperty extends AbstractTestBridgeCommentManager {
    @Test
    public void testDeleteCommentAsRelation() {
        // Use the comment as relation
        CommentManager anotherCommentManager = Framework.getService(CommentManager.class);
        DocumentModel commentDocModel = createComment(anotherCommentManager);
        Comment comment = anotherCommentManager.getComment(session, commentDocModel.getId());
        assertNotNull(comment);

        // Ensure that this comment is correctly created as relation
        CommentService commentComponent = (CommentService) Framework.getRuntime().getComponent(CommentService.NAME);
        RelationManager relationManager = Framework.getService(RelationManager.class);
        CommentServiceConfig config = commentComponent.getConfig();
        if (config != null) {
            Resource commentRes = relationManager.getResource(config.commentNamespace, commentDocModel, null);
            assertNotNull(commentRes);
            Graph graph = relationManager.getGraph(config.graphName, commentDocModel.getCoreSession());
            Resource predicateRes = new ResourceImpl(config.predicateNamespace);
            assertTrue(graph.getObjects(commentRes, predicateRes).stream().findAny().isPresent());
        }
        commentManager.deleteComment(session, commentDocModel.getId());
        try {
            anotherCommentManager.getComment(session, commentDocModel.getId());
            fail();
        } catch (CommentNotFoundException cfe) {
            assertNotNull(cfe);
            assertNotNull(cfe.getMessage());
        }
    }

    @Test
    public void testDeleteCommentAsProperty() {
        // Use the comment as property
        CommentManager anotherCommentManager = new PropertyCommentManager();
        DocumentModel commentDocModel = createComment(anotherCommentManager);
        Comment comment = anotherCommentManager.getComment(session, commentDocModel.getId());
        assertNotNull(comment);
        assertNotNull(comment.getParentId());
        assertNotEquals(0, comment.getParentId().length());

        // Delete this property comment using the Bridge
        commentManager.deleteComment(session, commentDocModel.getId());
        try {
            anotherCommentManager.getComment(session, commentDocModel.getId());
            fail();
        } catch (CommentNotFoundException cfe) {
            assertNotNull(cfe);
            assertNotNull(cfe.getMessage());
        }
    }

    @Override
    protected BridgeCommentManager getBridgeCommentManager() {
        return new BridgeCommentManager(Framework.getService(CommentManager.class), new PropertyCommentManager());
    }
}
