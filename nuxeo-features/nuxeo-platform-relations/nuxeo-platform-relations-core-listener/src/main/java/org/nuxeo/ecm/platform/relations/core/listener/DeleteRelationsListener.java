/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.relations.core.listener;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Core Event listener that cleans relation on deleted documents; it should be
 * executed after PublishRelationsListener so as to be able to copy relations
 * from the deleted proxies.
 *
 * @author mcedica
 */
public class DeleteRelationsListener implements EventListener {

    private RelationManager relationManager;

    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docEventContext = (DocumentEventContext) ctx;
            DocumentModel doc = docEventContext.getSourceDocument();
            relationManager = getRelationManager();

            // create resource from the document being deleted
            Resource sourceResource = relationManager.getResource(
                    RelationConstants.DOCUMENT_NAMESPACE, doc, null);

            // remove all the relations from the default graf in which this
            // document is an object in the statement
            Statement patternIncoming = new StatementImpl(null, null,
                    sourceResource);
            List<Statement> statementList = relationManager.getStatements(
                    RelationConstants.GRAPH_NAME, patternIncoming);
            relationManager.remove(RelationConstants.GRAPH_NAME, statementList);

            // remove all the relations in which this document is a subject in
            // the statement
            Statement patternOutcoming = new StatementImpl(sourceResource,
                    null, null);
            statementList = relationManager.getStatements(
                    RelationConstants.GRAPH_NAME, patternOutcoming);
            relationManager.remove(RelationConstants.GRAPH_NAME, statementList);
        }
    }

    public RelationManager getRelationManager() throws ClientException {
        if (relationManager == null) {
            try {
                relationManager = Framework.getService(RelationManager.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return relationManager;
    }

}
