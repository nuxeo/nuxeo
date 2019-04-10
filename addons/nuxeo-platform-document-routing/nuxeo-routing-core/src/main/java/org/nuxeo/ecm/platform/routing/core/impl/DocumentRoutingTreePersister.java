/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingPersister;
import org.nuxeo.ecm.platform.routing.core.persistence.TreeHelper;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;

/**
 * The default persister. It persists the {@link DocumentRoute} in a tree hierarchy ressembling the current date. New
 * model created from instance are stored in the personal workspace of the user.
 *
 * @author arussel
 */
public class DocumentRoutingTreePersister implements DocumentRoutingPersister {

    private static final String DC_TITLE = "dc:title";

    protected static final Log log = LogFactory.getLog(DocumentRoutingTreePersister.class);

    @Override
    public DocumentModel getParentFolderForDocumentRouteInstance(DocumentModel document, CoreSession session) {
        return TreeHelper.getOrCreateDateTreeFolder(session, getOrCreateRootOfDocumentRouteInstanceStructure(session),
                new Date(), "HiddenFolder");
    }

    @Override
    public DocumentModel createDocumentRouteInstanceFromDocumentRouteModel(DocumentModel model, CoreSession session) {
        DocumentModel parent = getParentFolderForDocumentRouteInstance(model, session);
        DocumentModel result = session.copy(model.getRef(), parent.getRef(), null);
        // copy now copies all the acls, and we don't need the readOnly
        // policy applied on the model
        // on the instance, too => removing acls
        result = undoReadOnlySecurityPolicy(result, session);
        // set initiator
        NuxeoPrincipal principal = session.getPrincipal();
        String initiator = principal.getActingUser();
        result.setPropertyValue(DocumentRoutingConstants.INITIATOR, initiator);
        // using the ref, the value of the attached document might not been
        // saved on the model
        result.setPropertyValue(DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME,
                model.getPropertyValue(DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME));
        // reset creation date, used for workflow start time
        result.setPropertyValue("dc:created", Calendar.getInstance());
        result.setPropertyValue(DocumentRoutingConstants.DOCUMENT_ROUTE_INSTANCE_MODEL_ID, model.getId());
        session.saveDocument(result);
        return result;
    }

    @Override
    public DocumentModel saveDocumentRouteInstanceAsNewModel(DocumentModel routeInstance, DocumentModel parentFolder,
            String newName, CoreSession session) {
        DocumentModel result = session.copy(routeInstance.getRef(), parentFolder.getRef(), newName);
        return undoReadOnlySecurityPolicy(result, session);
    }

    @Override
    public DocumentModel getOrCreateRootOfDocumentRouteInstanceStructure(CoreSession session) {
        DocumentModel root = getDocumentRoutesStructure(
                DocumentRoutingConstants.DOCUMENT_ROUTE_INSTANCES_ROOT_DOCUMENT_TYPE, session);
        if (root == null) {
            root = createDocumentRoutesStructure(DocumentRoutingConstants.DOCUMENT_ROUTE_INSTANCES_ROOT_DOCUMENT_TYPE,
                    DocumentRoutingConstants.DOCUMENT_ROUTE_INSTANCES_ROOT_ID, session);
        }
        return root;
    }

    /**
     * Finds the first domain by name, and creates under it the root container for the structure containing the route
     * instances.
     */
    protected DocumentModel createDocumentRoutesStructure(String routeStructureDocType, String id, CoreSession session)
            {
        DocumentModel root = session.createDocumentModel(session.getRootDocument().getPathAsString(), id,
                routeStructureDocType);
        root.setPropertyValue(DC_TITLE, routeStructureDocType);
        root = session.createDocument(root);
        ACP acp = session.getACP(root.getRef());
        ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
        acl.addAll(getACEs());
        session.setACP(root.getRef(), acp, true);
        return root;
    }

    /**
     * Create the rootModels under to root document. Grant READ to everyone on the root models ; workflow availability
     * is specified on each route
     *
     * @param routeStructureDocType
     * @param id
     * @param session
     * @return
     */
    protected DocumentModel createModelsRoutesStructure(String routeStructureDocType, String id, CoreSession session)
            {
        DocumentModel rootModels = session.createDocumentModel("/", id, routeStructureDocType);
        rootModels.setPropertyValue(DC_TITLE, routeStructureDocType);
        rootModels = session.createDocument(rootModels);
        ACP acp = session.getACP(rootModels.getRef());
        ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
        acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.READ, true));
        session.setACP(rootModels.getRef(), acp, true);
        return rootModels;
    }

    /**
     * @return
     */
    protected List<ACE> getACEs() {
        List<ACE> aces = new ArrayList<ACE>();
        for (String group : getUserManager().getAdministratorsGroups()) {
            aces.add(new ACE(group, SecurityConstants.EVERYTHING, true));
        }
        aces.add(new ACE(DocumentRoutingConstants.ROUTE_MANAGERS_GROUP_NAME, SecurityConstants.READ_WRITE, true));
        aces.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false));
        return aces;
    }

    protected UserManager getUserManager() {
        return Framework.getService(UserManager.class);
    }

    protected DocumentModel getDocumentRoutesStructure(String type, CoreSession session) {
        DocumentModelList res = session.query(String.format("SELECT * from %s", type));
        if (res == null || res.isEmpty()) {
            return null;
        }
        if (res.size() > 1) {
            if (log.isWarnEnabled()) {
                log.warn("More han one DocumentRouteInstanceRoot found:");
                for (DocumentModel model : res) {
                    log.warn(" - " + model.getName() + ", " + model.getPathAsString());
                }
            }
        }
        return res.get(0);
    }

    @Override
    public DocumentModel getParentFolderForNewModel(CoreSession session, DocumentModel instance) {
        UserWorkspaceService service = Framework.getService(UserWorkspaceService.class);
        return service.getCurrentUserPersonalWorkspace(session, instance);
    }

    @Override
    public String getNewModelName(DocumentModel instance) {
        return "(COPY) " + instance.getPropertyValue("dc:title");
    }

    protected DocumentModel undoReadOnlySecurityPolicy(DocumentModel instance, CoreSession session)
            {
        UndoReadOnlySecurityPolicy runner = new UndoReadOnlySecurityPolicy(session, instance.getRef());
        runner.runUnrestricted();
        return session.getDocument(runner.getInstanceRef());
    }

    class UndoReadOnlySecurityPolicy extends UnrestrictedSessionRunner {

        DocumentRef documentRef;

        public UndoReadOnlySecurityPolicy(CoreSession session, DocumentRef documentRef) {
            super(session);
            this.documentRef = documentRef;
        }

        @Override
        public void run() {
            DocumentModel instance = session.getDocument(documentRef);
            if (instance == null) {
                return;
            }
            ACP acp = instance.getACP();
            // remove READ for everyone
            ACL routingACL = acp.getOrCreateACL(DocumentRoutingConstants.DOCUMENT_ROUTING_ACL);
            routingACL.remove(new ACE(SecurityConstants.EVERYONE, SecurityConstants.READ, true));
            // unblock rights inheritance
            ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
            localACL.remove(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false));
            instance.setACP(acp, true);
        }

        DocumentRef getInstanceRef() {
            return documentRef;
        }
    }

    @Override
    public DocumentModel getParentFolderForDocumentRouteModels(CoreSession session) {
        DocumentModel root = getDocumentRoutesStructure(
                DocumentRoutingConstants.DOCUMENT_ROUTE_MODELS_ROOT_DOCUMENT_TYPE, session);
        if (root == null) {
            root = createModelsRoutesStructure(DocumentRoutingConstants.DOCUMENT_ROUTE_MODELS_ROOT_DOCUMENT_TYPE,
                    DocumentRoutingConstants.DOCUMENT_ROUTE_MODELS_ROOT_ID, session);
        }
        return root;
    }
}
