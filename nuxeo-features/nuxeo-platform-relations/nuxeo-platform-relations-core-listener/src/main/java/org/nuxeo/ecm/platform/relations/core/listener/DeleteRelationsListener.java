/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.relations.core.listener;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Core Event listener that cleans relation on deleted documents; it should be executed after PublishRelationsListener
 * so as to be able to copy relations from the deleted proxies.
 *
 * @author mcedica
 */
public class DeleteRelationsListener implements EventListener {

    private RelationManager relationManager;

    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docEventContext = (DocumentEventContext) ctx;
            DocumentModel doc = docEventContext.getSourceDocument();
            relationManager = getRelationManager();

            // create resource from the document being deleted
            Resource sourceResource = relationManager.getResource(RelationConstants.DOCUMENT_NAMESPACE, doc, null);

            // remove all the relations from the default graf in which this
            // document is an object in the statement
            Graph graph = relationManager.getGraphByName(RelationConstants.GRAPH_NAME);
            List<Statement> statementList = graph.getStatements(null, null, sourceResource);
            graph.remove(statementList);

            // remove all the relations in which this document is a subject in
            // the statement
            statementList = graph.getStatements(sourceResource, null, null);
            graph.remove(statementList);
        }
    }

    public RelationManager getRelationManager() {
        if (relationManager == null) {
            relationManager = Framework.getService(RelationManager.class);
        }
        return relationManager;
    }

}
