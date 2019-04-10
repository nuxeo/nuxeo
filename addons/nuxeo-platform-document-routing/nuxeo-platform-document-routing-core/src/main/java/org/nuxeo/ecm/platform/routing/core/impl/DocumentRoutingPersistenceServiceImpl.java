/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingPersistenceService;
import org.nuxeo.ecm.platform.routing.core.persistence.TreeHelper;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author arussel
 *
 */
public class DocumentRoutingPersistenceServiceImpl extends DefaultComponent
        implements DocumentRoutingPersistenceService {

    private static final String DC_TITLE = "dc:title";

    protected static final Log log = LogFactory.getLog(DocumentRoutingPersistenceServiceImpl.class);

    @Override
    public DocumentModel getParentFolderForDocumentRouteInstance(
            DocumentModel document, CoreSession session) {
        try {
            return TreeHelper.getOrCreateDateTreeFolder(session,
                    getOrCreateRootOfDocumentRouteInstanceStructure(session),
                    new Date(), "Folder");
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public DocumentModel createDocumentRouteInstanceFromDocumentRouteModel(
            DocumentModel model, CoreSession session) {
        DocumentModel parent = getParentFolderForDocumentRouteInstance(model,
                session);
        DocumentModel result = null;
        try {
            result = session.copy(model.getRef(), parent.getRef(), null);
            session.save();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        return result;
    }

    @Override
    public DocumentModel saveDocumentRouteInstanceAsNewModel(
            DocumentModel routeInstance, DocumentModel parentFolder,
            CoreSession session) {
        try {
            return session.copy(routeInstance.getRef(), parentFolder.getRef(),
                    null);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public DocumentModel getOrCreateRootOfDocumentRouteInstanceStructure(
            CoreSession session) {
        DocumentModel root;
        try {
            root = getDocumentRouteInstancesStructure(session);
            if (root == null) {
                root = createDocumentRouteInstancesStructure(session);
            }
            return root;
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected DocumentModel createDocumentRouteInstancesStructure(
            CoreSession session) throws ClientException {
        DocumentModel defaultDomain = session.getChildren(
                session.getRootDocument().getRef()).get(0);
        DocumentModel root = session.createDocumentModel(
                defaultDomain.getPathAsString(),
                DocumentRoutingConstants.DOCUMENT_ROUTE_INSTANCES_ROOT_ID,
                DocumentRoutingConstants.DOCUMENT_ROUTE_INSTANCES_ROOT_DOCUMENT_TYPE);
        root.setPropertyValue(
                DC_TITLE,
                DocumentRoutingConstants.DOCUMENT_ROUTE_INSTANCES_ROOT_DOCUMENT_TYPE);
        root = session.createDocument(root);
        session.save();
        return root;
    }

    protected DocumentModel getDocumentRouteInstancesStructure(
            CoreSession session) throws ClientException {
        DocumentModelList res = session.query(String.format(
                "SELECT * from %s",
                DocumentRoutingConstants.DOCUMENT_ROUTE_INSTANCES_ROOT_DOCUMENT_TYPE));
        if (res == null || res.isEmpty()) {
            return null;
        }
        if (res.size() > 1) {
            if (log.isWarnEnabled()) {
                log.warn("More han one DocumentRouteInstanceRoot found:");
                for (DocumentModel model : res) {
                    log.warn(" - " + model.getName() + ", "
                            + model.getPathAsString());
                }
            }
        }
        return res.get(0);
    }
}
