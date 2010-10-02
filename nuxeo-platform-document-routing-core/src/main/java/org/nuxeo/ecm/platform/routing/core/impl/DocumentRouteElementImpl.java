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

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationContext;
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
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 *
 */
public class DocumentRouteElementImpl implements DocumentRouteElement {

    protected DocumentModel document;

    public DocumentRouteElementImpl(DocumentModel doc) {
        this.document = doc;
    }

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
    public DocumentRoute getDocumentRoute(CoreSession session) {
        DocumentModel parent = document;
        while (true) {
            try {
                if (DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE.equals(parent.getType())) {
                    break;
                }
                parent = session.getParentDocument(parent.getRef());
            } catch (ClientException e) {
                throw new RuntimeException(e);
            }
        }
        return parent.getAdapter(DocumentRoute.class);
    }

    public AutomationService getAutomationService() {
        try {
            return Framework.getService(AutomationService.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DocumentRoutingService getDocumentRoutingService() {
        try {
            return Framework.getService(DocumentRoutingService.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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
    public void run(CoreSession session) {
        if (isRunning()) {
            return;
        } else {
            setRunning(session);
        }
        if (!(this instanceof DocumentRouteStep)) {
            throw new RuntimeException(
                    "Method run should be overriden in parent class.");
        }
        OperationContext context = new OperationContext(session);
        context.put(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY, this);
        context.setInput(getAttachedDocuments(session));
        try {
            String chainId = getDocumentRoutingService().getOperationChainId(
                    document.getType());
            getAutomationService().run(context, chainId);
        } catch (InvalidChainException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isRunning() {
        return checkLifeCycleState(ElementLifeCycleState.running);
    }

    @Override
    public void setRunning(CoreSession session) {
        followTransition(ElementLifeCycleTransistion.toRunning, session);
    }

    @Override
    public void followTransition(ElementLifeCycleTransistion transition,
            CoreSession session) {
        try {
            session.saveDocument(document);
            document.followTransition(transition.name());
            session.saveDocument(document);
            document = session.getDocument(document.getRef());
            session.save();
            if (Framework.isTestModeSet()) {
                Framework.getLocalService(EventService.class).waitForAsyncCompletion();
            }
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public void save(CoreSession session) {
        try {
            session.saveDocument(document);
            session.save();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public void setDone(CoreSession session) {
        followTransition(ElementLifeCycleTransistion.toDone, session);
    }

    @Override
    public void setValidated(CoreSession session) {
        followTransition(ElementLifeCycleTransistion.toValidated, session);
    }

    @Override
    public void setReady(CoreSession session) {
        followTransition(ElementLifeCycleTransistion.toReady, session);
    }

    public void validate(CoreSession session) throws ClientException {
        setValidated(session);
        setReadOnly(session);
    }

    @Override
    public void setReadOnly(CoreSession session) throws ClientException {
        SetDocumentOnReadOnlyUnrestrictedSessionRunner readOnlySetter = new SetDocumentOnReadOnlyUnrestrictedSessionRunner(
                session, document);
        readOnlySetter.runUnrestricted();
    }

    protected class SetDocumentOnReadOnlyUnrestrictedSessionRunner extends
            UnrestrictedSessionRunner {

        public SetDocumentOnReadOnlyUnrestrictedSessionRunner(
                CoreSession session, DocumentModel document) {
            super(session);
            this.document = document;
        }

        protected DocumentModel document;

        @Override
        public void run() throws ClientException {
            ACP acp = new ACPImpl();
            // add new ACL to set READ permission to everyone
            ACL routingACL = acp.getOrCreateACL(DocumentRoutingConstants.DOCUMENT_ROUTING_ACL);
            routingACL.add(new ACE(SecurityConstants.EVERYONE,
                    SecurityConstants.READ, true));

            // block rights inheritance
            ACL inheritedACL = acp.getOrCreateACL(ACL.INHERITED_ACL);
            inheritedACL.add(new ACE(SecurityConstants.EVERYONE,
                    SecurityConstants.EVERYTHING, false));

            ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
            localACL.add(new ACE(SecurityConstants.EVERYONE,
                    SecurityConstants.EVERYTHING, false));

            document.setACP(acp, true);
            session.saveDocument(document);
            session.save();
        }
    }

    @Override
    public String getTypeDescription() {
        return document.getType();
    }

}