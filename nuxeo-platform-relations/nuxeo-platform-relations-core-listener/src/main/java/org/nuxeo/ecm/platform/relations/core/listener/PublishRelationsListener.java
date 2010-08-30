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
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Core Event listener to copy relations affecting the source document to the
 * proxy upon publication events and the relations that were present on the
 * replaced proxies if any.
 *
 * If this core event listener is used in combination with another core event
 * listener that cleans relation on deleted documents, it should be executed
 * before the cleaning listener so as to be able to copy relations from the
 * deleted proxies.
 *
 * This core event listener cannot work in asynchronous or post commit mode.
 *
 * @author ogrisel
 */
public class PublishRelationsListener implements EventListener {

    private static final Log log = LogFactory.getLog(PublishRelationsListener.class);

    protected RelationManager rmanager;

    // Override to change the list of graphs to copy relations when a document
    // is published, set to null to copy relations from all graphs

    protected List<String> graphNamesForCopyFromWork = Arrays.asList(RelationConstants.GRAPH_NAME);

    protected List<String> graphNamesForCopyFromReplacedProxy = Arrays.asList(RelationConstants.GRAPH_NAME, "documentComments");

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

    public List<String> getGraphNamesForCopyFromWork() throws ClientException {
        if (graphNamesForCopyFromWork == null) {
            return getRelationManager().getGraphNames();
        }
        return graphNamesForCopyFromWork;
    }

    public List<String> getGraphNamesForCopyFromReplacedProxy() throws ClientException {
        if (graphNamesForCopyFromReplacedProxy == null) {
            return getRelationManager().getGraphNames();
        }
        return graphNamesForCopyFromReplacedProxy;
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

            // copy the relations from the work version (the source document
            // getting published)
            copyRelationsFromWorkVersion(rmanager, sourceResource,
                    publishedResource);

            List<String> replacedProxyIds = (List<String>) ctx.getProperties().get(
                    CoreEventConstants.REPLACED_PROXY_IDS);
            if (replacedProxyIds != null) {
                for (String replacedProxyId : replacedProxyIds) {
                    DocumentLocationImpl docLoc = new DocumentLocationImpl(
                            ctx.getRepositoryName(),
                            new IdRef(replacedProxyId), null);
                    Resource replacedResource = rmanager.getResource(
                            RelationConstants.DOCUMENT_NAMESPACE, docLoc, null);
                    copyRelationsFromReplacedProxy(rmanager, replacedResource,
                            publishedResource, sourceResource);
                }
            }
        }
    }

    protected void copyRelationsFromReplacedProxy(RelationManager rmanager,
            Resource replacedResource, Resource publishedResource,
            Resource sourceResource) throws ClientException {

        Statement sourcePattern = new StatementImpl(replacedResource, null,
                null);
        Statement targetPattern = new StatementImpl(null, null,
                replacedResource);

        for (String graphName : getGraphNamesForCopyFromReplacedProxy()) {

            // collect existing relations to or from the source resource
            List<Statement> newStatements = new ArrayList<Statement>();
            for (Statement stmt : rmanager.getStatements(graphName,
                    sourcePattern)) {
                if (!isCopyFromSource(stmt, sourceResource)) {
                    // do not copy previous relations that come from a source
                    // copy
                    stmt.setSubject(publishedResource);
                    newStatements.add(stmt);
                }
            }
            for (Statement stmt : rmanager.getStatements(graphName,
                    targetPattern)) {
                if (!isCopyFromSource(stmt, sourceResource)) {
                    // do not copy previous relations that come from a source
                    // copy
                    stmt.setObject(publishedResource);
                    newStatements.add(stmt);
                }
            }
            if (!newStatements.isEmpty()) {
                // add the rewritten statements on the proxy
                rmanager.add(graphName, newStatements);
            }
        }
    }

    protected boolean isCopyFromSource(Statement stmt, Resource sourceResource) {
        Node[] values = stmt.getProperties(RelationConstants.COPY_FROM_WORK_VERSION);
        if (values == null) {
            return false;
        } else {
            return Arrays.asList(values).contains(sourceResource);
        }
    }

    protected void copyRelationsFromWorkVersion(RelationManager rmanager,
            Resource sourceResource, Resource publishedResource)
            throws ClientException {
        Statement sourcePattern = new StatementImpl(sourceResource, null, null);
        Statement targetPattern = new StatementImpl(null, null, sourceResource);

        for (String graphName : getGraphNamesForCopyFromWork()) {

            // collect existing relations to or from the source document
            List<Statement> newStatements = new ArrayList<Statement>();
            for (Statement stmt : rmanager.getStatements(graphName,
                    sourcePattern)) {
                stmt.setSubject(publishedResource);
                stmt.addProperty(RelationConstants.COPY_FROM_WORK_VERSION,
                        sourceResource);
                newStatements.add(stmt);
            }
            for (Statement stmt : rmanager.getStatements(graphName,
                    targetPattern)) {
                stmt.setObject(publishedResource);
                stmt.addProperty(RelationConstants.COPY_FROM_WORK_VERSION,
                        sourceResource);
                newStatements.add(stmt);
            }

            if (!newStatements.isEmpty()) {
                // add the rewritten statements on the proxy
                rmanager.add(graphName, newStatements);
            }
        }
    }
}
