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
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 *
 */
public class DocumentRouteElementImpl implements DocumentRouteElement,
        DocumentRouteStep {

    private static final long serialVersionUID = 1L;

    protected DocumentModel document;

    protected ElementRunner runner;

    public DocumentRouteElementImpl(DocumentModel doc, ElementRunner runner) {
        this.document = doc;
        this.runner = runner;
    }

    @Override
    public DocumentModelList getAttachedDocuments(CoreSession session) {
        List<String> docIds = getDocumentRoute(session).getAttachedDocuments();
        List<DocumentRef> refs = new ArrayList<DocumentRef>();
        for (String id : docIds) {
            refs.add(new IdRef(id));
        }
        try {
            return session.getDocuments(refs.toArray(new DocumentRef[] {}));
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run(CoreSession session) {
        runner.run(session, this);
    }

    @Override
    public void resume(CoreSession session, String nodeId, String taskId,
            Map<String, Object> data, String status) {
        runner.resume(session, this, nodeId, taskId, data, status);
    }

    @Override
    public DocumentRoute getDocumentRoute(CoreSession session) {
        DocumentModel parent = document;
        while (true) {
            try {
                if (parent.hasFacet(DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_FACET)) {
                    break;
                }
                parent = session.getParentDocument(parent.getRef());
            } catch (ClientException e) {
                throw new RuntimeException(e);
            }
        }
        return parent.getAdapter(DocumentRoute.class);
    }

    @Override
    public DocumentModel getDocument() {
        return document;
    }

    protected Object getProperty(String propertyName) {
        try {
            return document.getPropertyValue(propertyName);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return (String) getProperty(DocumentRoutingConstants.TITLE_PROPERTY_NAME);
    }

    @Override
    public boolean isValidated() {
        return checkLifeCycleState(ElementLifeCycleState.validated);
    }

    @Override
    public boolean isReady() {
        return checkLifeCycleState(ElementLifeCycleState.ready);
    }

    @Override
    public boolean isDone() {
        return checkLifeCycleState(ElementLifeCycleState.done);
    }

    protected boolean checkLifeCycleState(ElementLifeCycleState state) {
        try {
            return document.getCurrentLifeCycleState().equalsIgnoreCase(
                    state.name());
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public String getDescription() {
        return (String) getProperty(DocumentRoutingConstants.DESCRIPTION_PROPERTY_NAME);
    }

    @Override
    public boolean isRunning() {
        return checkLifeCycleState(ElementLifeCycleState.running);
    }

    @Override
    public boolean isCanceled() {
        return checkLifeCycleState(ElementLifeCycleState.canceled);
    }

    @Override
    public boolean isDraft() {
        return checkLifeCycleState(ElementLifeCycleState.draft);
    }

    @Override
    public void setRunning(CoreSession session) {
        followTransition(ElementLifeCycleTransistion.toRunning, session, false);
    }

    @Override
    public void followTransition(ElementLifeCycleTransistion transition,
            CoreSession session, boolean recursive) {
        try {
            if (document.followTransition(transition.name())) {
                if (Framework.isTestModeSet()) {
                    session.save();
                }
                document = session.getDocument(document.getRef());
            }
            if (recursive) {
                DocumentModelList children = session.getChildren(document.getRef());
                for (DocumentModel child : children) {
                    DocumentRouteElement element = child.getAdapter(DocumentRouteElement.class);
                    element.followTransition(transition, session, recursive);
                }

            }

        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public void save(CoreSession session) {
        try {
            session.saveDocument(document);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public void setDone(CoreSession session) {
        followTransition(ElementLifeCycleTransistion.toDone, session, false);
        EventFirer.fireEvent(session, this, null,
                DocumentRoutingConstants.Events.afterStepRunning.name());
    }

    @Override
    public void setValidated(CoreSession session) {
        followTransition(ElementLifeCycleTransistion.toValidated, session, true);
    }

    @Override
    public void setReady(CoreSession session) {
        followTransition(ElementLifeCycleTransistion.toReady, session, true);
    }

    @Override
    public void validate(CoreSession session) throws ClientException {
        setValidated(session);
        setReadOnly(session);
    }

    @Override
    public void setReadOnly(CoreSession session) throws ClientException {
        SetDocumentOnReadOnlyUnrestrictedSessionRunner readOnlySetter = new SetDocumentOnReadOnlyUnrestrictedSessionRunner(
                session, document.getRef());
        readOnlySetter.runUnrestricted();
    }

    protected class SetDocumentOnReadOnlyUnrestrictedSessionRunner extends
            UnrestrictedSessionRunner {

        public SetDocumentOnReadOnlyUnrestrictedSessionRunner(
                CoreSession session, DocumentRef ref) {
            super(session);
            this.ref = ref;
        }

        protected DocumentRef ref;

        @Override
        public void run() throws ClientException {
            DocumentModel doc = session.getDocument(ref);
            ACP acp = new ACPImpl();
            // add new ACL to set READ permission to everyone
            ACL routingACL = acp.getOrCreateACL(DocumentRoutingConstants.DOCUMENT_ROUTING_ACL);
            routingACL.add(new ACE(SecurityConstants.EVERYONE,
                    SecurityConstants.READ, true));
            // block rights inheritance
            ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
            localACL.add(new ACE(SecurityConstants.EVERYONE,
                    SecurityConstants.EVERYTHING, false));
            doc.setACP(acp, true);
            session.saveDocument(doc);
        }
    }

    @Override
    public boolean canValidateStep(CoreSession session) {
        return hasPermissionOnDocument(session,
                SecurityConstants.WRITE_LIFE_CYCLE);
    }

    protected boolean hasPermissionOnDocument(CoreSession session,
            String permission) {
        try {
            return session.hasPermission(document.getRef(), permission);
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setCanValidateStep(CoreSession session, String userOrGroup) {
        setPermissionOnDocument(session, userOrGroup,
                SecurityConstants.WRITE_LIFE_CYCLE);
    }

    protected void setPermissionOnDocument(CoreSession session,
            String userOrGroup, String permission) {
        try {
            ACP acp = document.getACP();
            ACL routingACL = acp.getOrCreateACL(DocumentRoutingConstants.DOCUMENT_ROUTING_ACL);
            routingACL.add(new ACE(userOrGroup, permission, true));
            document.setACP(acp, true);
            session.saveDocument(document);
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean canUpdateStep(CoreSession session) {
        return hasPermissionOnDocument(session,
                SecurityConstants.WRITE_PROPERTIES);
    }

    @Override
    public void setCanUpdateStep(CoreSession session, String userOrGroup) {
        setPermissionOnDocument(session, userOrGroup,
                SecurityConstants.WRITE_PROPERTIES);
    }

    @Override
    public void setCanReadStep(CoreSession session, String userOrGroup) {
        setPermissionOnDocument(session, userOrGroup, SecurityConstants.READ);
    }

    @Override
    public boolean canDeleteStep(CoreSession session) {
        return hasPermissionOnDocument(session, SecurityConstants.REMOVE);
    }

    @Override
    public void setCanDeleteStep(CoreSession session, String userOrGroup) {
        setPermissionOnDocument(session, userOrGroup, SecurityConstants.REMOVE);
    }

    @Override
    public void backToReady(CoreSession session) {
        EventFirer.fireEvent(session, this, null,
                DocumentRoutingConstants.Events.beforeStepBackToReady.name());
        followTransition(ElementLifeCycleTransistion.backToReady, session,
                false);
        EventFirer.fireEvent(session, this, null,
                DocumentRoutingConstants.Events.afterStepBackToReady.name());
    }

    @Override
    public DocumentRouteStep undo(CoreSession session) {
        runner.undo(session, this);
        try {
            document = session.getDocument(document.getRef());
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public boolean canUndoStep(CoreSession session) {
        try {
            GetIsParentRunningUnrestricted runner = new GetIsParentRunningUnrestricted(
                    session);
            runner.runUnrestricted();
            return runner.isRunning();
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }

    protected class GetIsParentRunningUnrestricted extends
            UnrestrictedSessionRunner {
        public GetIsParentRunningUnrestricted(CoreSession session) {
            super(session);
        }

        protected boolean isRunning;

        @Override
        public void run() throws ClientException {
            DocumentModel parent = session.getDocument(document.getParentRef());
            DocumentRouteElement parentElement = parent.getAdapter(DocumentRouteElement.class);
            isRunning = parentElement.isRunning();
        }

        public boolean isRunning() {
            return isRunning;
        }
    }

    @Override
    public void cancel(CoreSession session) {
        runner.cancel(session, this);
    }

    @Override
    public void setCanceled(CoreSession session) {
        followTransition(ElementLifeCycleTransistion.toCanceled, session, false);
    }

    @Override
    public boolean isModifiable() {
        return (isDraft() || isReady() || isRunning());
    }
}
