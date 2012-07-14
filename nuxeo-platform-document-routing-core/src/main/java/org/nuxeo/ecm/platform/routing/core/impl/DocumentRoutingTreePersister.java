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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingPersister;
import org.nuxeo.ecm.platform.routing.core.persistence.TreeHelper;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;

/**
 * The default persister. It persists the {@link DocumentRoute} in a tree
 * hierarchy ressembling the current date. New model created from instance are
 * stored in the personal workspace of the user.
 *
 * @author arussel
 *
 */
public class DocumentRoutingTreePersister implements DocumentRoutingPersister {

    private static final String DC_TITLE = "dc:title";

    protected static final Log log = LogFactory.getLog(DocumentRoutingTreePersister.class);

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
            // copy now copies all the acls, and we don't need the readOnly
            // policy applied on the model
            // on the instance, too => removing acls
            result = undoReadOnlySecurityPolicy(result, session);
            // using the ref, the value of the attached document might not been
            // saved on the model
            result.setPropertyValue(
                    DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME,
                    model.getPropertyValue(DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME));
            session.saveDocument(result);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        return result;
    }

    @Override
    public DocumentModel saveDocumentRouteInstanceAsNewModel(
            DocumentModel routeInstance, DocumentModel parentFolder,
            String newName, CoreSession session) {
        DocumentModel result = null;
        try {
            result = session.copy(routeInstance.getRef(),
                    parentFolder.getRef(), newName);
            return undoReadOnlySecurityPolicy(result, session);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public DocumentModel getOrCreateRootOfDocumentRouteInstanceStructure(
            CoreSession session) {
        DocumentModel root;
        try {
            root = getDocumentRoutesStructure(
                    DocumentRoutingConstants.DOCUMENT_ROUTE_INSTANCES_ROOT_DOCUMENT_TYPE,
                    session);
            if (root == null) {
                root = createDocumentRoutesStructure(
                        DocumentRoutingConstants.DOCUMENT_ROUTE_INSTANCES_ROOT_DOCUMENT_TYPE,
                        DocumentRoutingConstants.DOCUMENT_ROUTE_INSTANCES_ROOT_ID,
                        session);
            }
            return root;
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected DocumentModel createDocumentRoutesStructure(
            String routeStructureDocType, String id, CoreSession session)
            throws ClientException {
        String query = "SELECT * FROM Document WHERE " + NXQL.ECM_PARENTID
                + " = '%s' AND " + NXQL.ECM_LIFECYCLESTATE + " <> '"
                + LifeCycleConstants.DELETED_STATE + "' AND "
                + NXQL.ECM_MIXINTYPE + " <> '"
                + FacetNames.HIDDEN_IN_NAVIGATION + "' ORDER BY ecm:name";
        query = String.format(query, session.getRootDocument().getId());
        DocumentModelList docs = session.query(query, 1);
        DocumentModel defaultDomain = docs.get(0);
        DocumentModel root = session.createDocumentModel(
                defaultDomain.getPathAsString(), id, routeStructureDocType);
        root.setPropertyValue(DC_TITLE, routeStructureDocType);
        root = session.createDocument(root);
        ACP acp = session.getACP(root.getRef());
        ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
        acl.addAll(getACEs());
        session.setACP(root.getRef(), acp, true);
        return root;
    }

    /**
     * @return
     */
    protected List<ACE> getACEs() {
        List<ACE> aces = new ArrayList<ACE>();
        for (String group : getUserManager().getAdministratorsGroups()) {
            aces.add(new ACE(group, SecurityConstants.EVERYTHING, true));
        }
        aces.add(new ACE(DocumentRoutingConstants.ROUTE_MANAGERS_GROUP_NAME,
                SecurityConstants.READ_WRITE, true));
        aces.add(new ACE(SecurityConstants.EVERYONE,
                SecurityConstants.EVERYTHING, false));
        return aces;
    }

    protected UserManager getUserManager() {
        try {
            return Framework.getService(UserManager.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected DocumentModel getDocumentRoutesStructure(String type,
            CoreSession session) throws ClientException {
        DocumentModelList res = session.query(String.format("SELECT * from %s",
                type));
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

    @Override
    public DocumentModel getParentFolderForNewModel(CoreSession session,
            DocumentModel instance) {
        try {
            UserWorkspaceService service = Framework.getService(UserWorkspaceService.class);
            return service.getCurrentUserPersonalWorkspace(session, instance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getNewModelName(DocumentModel instance) {
        try {
            return "(COPY) " + instance.getPropertyValue("dc:title");
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }

    protected DocumentModel undoReadOnlySecurityPolicy(DocumentModel instance,
            CoreSession session) throws ClientException {
        UndoReadOnlySecurityPolicy runner = new UndoReadOnlySecurityPolicy(
                session, instance.getRef());
        runner.runUnrestricted();
        return session.getDocument(runner.getInstanceRef());
    }

    class UndoReadOnlySecurityPolicy extends UnrestrictedSessionRunner {

        DocumentRef documentRef;

        public UndoReadOnlySecurityPolicy(CoreSession session,
                DocumentRef documentRef) {
            super(session);
            this.documentRef = documentRef;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel instance = session.getDocument(documentRef);
            if (instance == null) {
                return;
            }
            ACP acp = instance.getACP();
            // remove READ for everyone
            ACL routingACL = acp.getOrCreateACL(DocumentRoutingConstants.DOCUMENT_ROUTING_ACL);
            routingACL.remove(new ACE(SecurityConstants.EVERYONE,
                    SecurityConstants.READ, true));
            // unblock rights inheritance
            ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
            localACL.remove(new ACE(SecurityConstants.EVERYONE,
                    SecurityConstants.EVERYTHING, false));
            instance.setACP(acp, true);
        }

        DocumentRef getInstanceRef() {
            return documentRef;
        }
    }

    @Override
    public DocumentModel getParentFolderForDocumentRouteModels(
            CoreSession session) {
        DocumentModel root;
        try {
            root = getDocumentRoutesStructure(
                    DocumentRoutingConstants.DOCUMENT_ROUTE_MODELS_ROOT_DOCUMENT_TYPE,
                    session);
            if (root == null) {
                root = createDocumentRoutesStructure(
                        DocumentRoutingConstants.DOCUMENT_ROUTE_MODELS_ROOT_DOCUMENT_TYPE,
                        DocumentRoutingConstants.DOCUMENT_ROUTE_MODELS_ROOT_ID,
                        session);
            }
            return root;
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }

    }
}