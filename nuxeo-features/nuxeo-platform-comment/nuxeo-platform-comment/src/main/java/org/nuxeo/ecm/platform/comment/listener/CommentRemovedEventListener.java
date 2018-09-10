/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.listener;

import static org.nuxeo.ecm.platform.comment.api.CommentManager.Feature.COMMENTS_LINKED_WITH_PROPERTY;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.service.CommentServiceConfig;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.runtime.api.Framework;

public class CommentRemovedEventListener extends AbstractCommentListener implements EventListener {

    private static final Log log = LogFactory.getLog(CommentRemovedEventListener.class);

    @Override
    protected void doProcess(CoreSession coreSession, RelationManager relationManager, CommentServiceConfig config,
            DocumentModel docMessage) {
        log.debug("Processing relations cleanup on Comment removal");
        String typeName = docMessage.getType();
        if (CommentsConstants.COMMENT_DOC_TYPE.equals(typeName) || "Post".equals(typeName)) {
            CommentManager commentManager = Framework.getService(CommentManager.class);
            if (commentManager.hasFeature(COMMENTS_LINKED_WITH_PROPERTY)) {
                deleteCommentChildren(coreSession, commentManager, docMessage);
                coreSession.save();
            } else {
                onCommentRemoved(relationManager, config, docMessage);
            }
        }
    }

    private static void onCommentRemoved(RelationManager relationManager, CommentServiceConfig config,
            DocumentModel docModel) {
        Resource commentRes = relationManager.getResource(config.commentNamespace, docModel, null);
        if (commentRes == null) {
            log.warn("Could not adapt document model to relation resource; "
                    + "check the service relation adapters configuration");
            return;
        }
        Graph graph = relationManager.getGraph(config.graphName, docModel.getCoreSession());
        List<Statement> statementList = graph.getStatements(commentRes, null, null);
        graph.remove(statementList);
    }
}
