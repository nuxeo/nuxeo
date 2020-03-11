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
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.service.CommentServiceConfig;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.runtime.api.Framework;

public class DocumentRemovedCommentEventListener extends AbstractCommentListener implements PostCommitEventListener {

    private static final Log log = LogFactory.getLog(DocumentRemovedCommentEventListener.class);

    @Override
    protected void doProcess(CoreSession coreSession, RelationManager relationManager, CommentServiceConfig config,
            DocumentModel docMessage) {
        log.debug("Processing relations cleanup on Document removal");
        CommentManager commentManager = Framework.getService(CommentManager.class);
        if (commentManager.hasFeature(COMMENTS_LINKED_WITH_PROPERTY)) {
            deleteCommentChildren(coreSession, commentManager, docMessage);
            coreSession.save();
        } else {
            onDocumentRemoved(coreSession, relationManager, config, docMessage);
        }
    }

    private static void onDocumentRemoved(CoreSession coreSession, RelationManager relationManager,
            CommentServiceConfig config, DocumentModel docMessage) {

        Resource documentRes = relationManager.getResource(config.documentNamespace, docMessage, null);
        if (documentRes == null) {
            log.error("Could not adapt document model to relation resource ; "
                    + "check the service relation adapters configuration");
            return;
        }
        Graph graph = relationManager.getGraph(config.graphName, coreSession);
        List<Statement> statementList = graph.getStatements(null, null, documentRes);

        // remove comments
        for (Statement stmt : statementList) {
            QNameResource resource = (QNameResource) stmt.getSubject();
            String commentId = resource.getLocalName();
            DocumentModel docModel = (DocumentModel) relationManager.getResourceRepresentation(config.commentNamespace,
                    resource, null);

            if (docModel != null) {
                try {
                    coreSession.removeDocument(docModel.getRef());
                    log.debug("comment removal succeded for id: " + commentId);
                } catch (DocumentNotFoundException e) {
                    log.error("comment removal failed", e);
                }
            } else {
                log.warn("comment not found: id=" + commentId);
            }
        }
        coreSession.save();
        // remove relations
        graph.remove(statementList);
    }

}
