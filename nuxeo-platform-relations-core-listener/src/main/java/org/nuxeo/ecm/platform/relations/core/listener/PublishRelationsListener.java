/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
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
 * Core Event listener to copy relations affecting the source document to the
 * proxy upon publication events.
 * 
 * @author ogrisel
 */
public class PublishRelationsListener implements EventListener {

    private static final Log log = LogFactory.getLog(PublishRelationsListener.class);

    protected RelationManager rmanager = null;

    // Override to change the list of graphs to copy relations when a document
    // is published, set to null to copy relations from all graphs
    protected List<String> graphNames = Arrays.asList(RelationConstants.GRAPH_NAME);

    public RelationManager getRelationManager() throws ClientException {
        if (rmanager == null) {
            try {
                rmanager = Framework.getService(RelationManager.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return rmanager;
    }

    public List<String> getGraphNames() throws ClientException {
        if (graphNames == null) {
            return getRelationManager().getGraphNames();
        }
        return graphNames;
    }

    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();

        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel publishedDoc = docCtx.getSourceDocument();
            if (!publishedDoc.isProxy()) {
                // we are only interested in the publication of proxy documents
                return;
            }
            CoreSession session = ctx.getCoreSession();
            RelationManager rmanager = getRelationManager();

            // fetch the archived version the proxy is pointing to
            DocumentModel sourceDoc = session.getSourceDocument(publishedDoc.getRef());

            // fetch the working version the archived version is coming from
            sourceDoc = session.getSourceDocument(sourceDoc.getRef());

            if (sourceDoc == null) {
                log.warn("working version of the proxy is no longer available, cannot copy the relations");
                return;
            }

            Resource sourceResource = rmanager.getResource(
                    RelationConstants.DOCUMENT_NAMESPACE, sourceDoc, null);
            Resource publishedResource = rmanager.getResource(
                    RelationConstants.DOCUMENT_NAMESPACE, publishedDoc, null);

            Statement sourcePattern = new StatementImpl(sourceResource, null,
                    null);
            Statement targetPattern = new StatementImpl(null, null,
                    sourceResource);

            for (String graphName : getGraphNames()) {

                // collect existing relations to or from the source document
                List<Statement> newStatements = new ArrayList<Statement>();
                for (Statement stmt : rmanager.getStatements(graphName,
                        sourcePattern)) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "copying statement (%s, %s, %s)",
                                stmt.getSubject(), stmt.getPredicate(),
                                stmt.getObject()));
                    }
                    stmt.setSubject(publishedResource);
                    newStatements.add(stmt);
                }
                for (Statement stmt : rmanager.getStatements(graphName,
                        targetPattern)) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "copying statement (%s, %s, %s)",
                                stmt.getSubject(), stmt.getPredicate(),
                                stmt.getObject()));
                    }
                    stmt.setObject(publishedResource);
                    newStatements.add(stmt);
                }

                if (!newStatements.isEmpty()) {
                    // add the rewritten statements on the proxy
                    rmanager.add(graphName, newStatements);
                }
            }
        }
    }
}
