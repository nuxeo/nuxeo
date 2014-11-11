/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.listener;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.platform.comment.service.CommentServiceConfig;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;

public class CommentRemovedEventListener extends AbstractCommentListener
        implements EventListener {

    private static final Log log = LogFactory.getLog(CommentRemovedEventListener.class);

    @Override
    protected void doProcess(CoreSession coreSession,
            RelationManager relationManager, CommentServiceConfig config,
            DocumentModel docMessage) throws Exception {
        log.debug("Processing relations cleanup on Comment removal");
        String typeName = docMessage.getType();
        if ("Comment".equals(typeName) || "Post".equals(typeName)) {
            onCommentRemoved(relationManager, config, docMessage);
        }
    }

    private static void onCommentRemoved(RelationManager relationManager,
            CommentServiceConfig config, DocumentModel docModel)
            throws ClientException {
        Resource commentRes = relationManager.getResource(
                config.commentNamespace, docModel, null);
        if (commentRes == null) {
            log.warn("Could not adapt document model to relation resource; "
                    + "check the service relation adapters configuration");
            return;
        }
        Statement pattern = new StatementImpl(commentRes, null, null);
        List<Statement> statementList = relationManager.getStatements(
                config.graphName, pattern);
        relationManager.remove(config.graphName, statementList);
    }

}
