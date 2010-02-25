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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.jbpm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.jbpm.JbpmEventNames;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublishingEvent;
import org.nuxeo.ecm.platform.publisher.api.PublishingException;
import org.nuxeo.ecm.platform.publisher.impl.core.CoreFolderPublicationNode;
import org.nuxeo.ecm.platform.publisher.impl.core.CoreProxyFactory;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.publisher.rules.PublishingValidatorException;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * Implementation of the {@link PublishedDocumentFactory} for core
 * implementation using native proxy system with validation workflow.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 *
 */
public class CoreProxyWithWorkflowFactory extends CoreProxyFactory implements
        PublishedDocumentFactory {

    public static final String TASK_NAME = "org.nuxeo.ecm.platform.publisher.jbpm.CoreProxyWithWorkflowFactory";

    public static final String ACL_NAME = "org.nuxeo.ecm.platform.publisher.jbpm.CoreProxyWithWorkflowFactory";

    protected JbpmService jbpmService;

    protected UserManager userManager;

    protected EventProducer eventProducer;

    @Override
    public PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {
        NuxeoPrincipal principal = (NuxeoPrincipal) coreSession.getPrincipal();
        DocumentModel targetDocModel;
        if (targetNode instanceof CoreFolderPublicationNode) {
            CoreFolderPublicationNode coreNode = (CoreFolderPublicationNode) targetNode;
            targetDocModel = coreNode.getTargetDocumentModel();
        } else {
            targetDocModel = coreSession.getDocument(new PathRef(
                    targetNode.getPath()));
        }
        DocumentModelList list = getProxiesUnrestricted(coreSession, doc,
                targetDocModel);
        if (list.isEmpty()) {// first publication
            DocumentModel proxy = publishUnrestricted(doc, targetDocModel);
            SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(
                    proxy);
            if (!isValidator(proxy, principal)) {
                notifyEvent(PublishingEvent.documentWaitingPublication, proxy,
                        coreSession);
                restrictPermission(proxy, principal, coreSession, null);
                createTask(proxy, coreSession, principal);
                publishedDocument.setPending(true);
            } else {
                notifyEvent(PublishingEvent.documentPublished, proxy,
                        coreSession);
            }
            return publishedDocument;
        } else if (list.size() == 1) {
            // one doc is already published or waiting for publication
            if (isPublishedDocWaitingForPublication(list.get(0))) {
                DocumentModel proxy = publishUnrestricted(doc, targetDocModel);
                if (!isValidator(proxy, principal)) {
                    // we're getting the old proxy acl
                    ACL acl = list.get(0).getACP().getACL(ACL_NAME);
                    acl.add(new ACE(principal.getName(),
                            SecurityConstants.READ, true));
                    proxy.getACP().addACL(acl);
                    SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(
                            proxy);
                    publishedDocument.setPending(true);
                    return publishedDocument;
                } else {
                    endTask(proxy, principal, coreSession, null,
                            PublishingEvent.documentPublicationApproved);
                    notifyEvent(PublishingEvent.documentPublished, proxy,
                            coreSession);
                }
            } else {
                if (!isValidator(list.get(0), principal)) {
                    DocumentModel proxy = publishUnrestricted(doc,
                            targetDocModel, false);
                    SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(
                            proxy);
                    notifyEvent(PublishingEvent.documentWaitingPublication,
                            proxy, coreSession);
                    restrictPermission(proxy, principal, coreSession, null);
                    createTask(proxy, coreSession, principal);
                    publishedDocument.setPending(true);
                    return publishedDocument;
                } else {
                    DocumentModel proxy = publishUnrestricted(doc, targetDocModel);
                    notifyEvent(PublishingEvent.documentPublished, proxy,
                            coreSession);
                    SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(
                            proxy);
                    return publishedDocument;
                }
            }
        } else if (list.size() == 2) {
            DocumentModel publishedDoc = null;
            for(DocumentModel dm : list) {
                if(dm.getACP().getACL(ACL_NAME) != null) {
                    publishedDoc = dm;
                }
            }
            if(!isValidator(publishedDoc, principal)) {
                // we're getting the old proxy acl
                ACL acl = list.get(0).getACP().getACL(ACL_NAME);
                acl.add(new ACE(principal.getName(),
                        SecurityConstants.READ, true));
                // remove publishedDoc
                publishedDoc.getACP().addACL(acl);
                SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(
                        publishedDoc);
                publishedDocument.setPending(true);
                return publishedDocument;
            } else {
                coreSession.removeDocument(publishedDoc.getRef());
                DocumentModel proxy = publishUnrestricted(doc, targetDocModel);
                notifyEvent(PublishingEvent.documentPublished, proxy,
                        coreSession);
                SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(
                        proxy);
                return publishedDocument;
            }
        }
        throw new IllegalStateException(
        "No more than 2 doc can be published");
    }

    private DocumentModelList getProxiesUnrestricted(CoreSession session,
            DocumentModel doc, DocumentModel targetDocModel)
            throws ClientException {
        GetProxiesUnrestricted runner = new GetProxiesUnrestricted(session,
                targetDocModel, doc);
        runner.runUnrestricted();
        return runner.getDocumentModelList();
    }

    protected boolean isPublishedDocWaitingForPublication(
            DocumentModel documentModel) throws ClientException {
        return documentModel.getACP().getACL(ACL_NAME) != null;
    }

    protected DocumentModel publishUnrestricted(DocumentModel doc,
            DocumentModel targetDocModel, boolean overwrite)
            throws ClientException {
        PublishUnrestricted publisher = new PublishUnrestricted(coreSession,
                doc, targetDocModel, overwrite);
        publisher.runUnrestricted();
        DocumentModel proxy = publisher.getModel();
        return proxy;
    }

    protected DocumentModel publishUnrestricted(DocumentModel doc,
            DocumentModel targetDocModel) throws ClientException {
        return publishUnrestricted(doc, targetDocModel, true);
    }

    protected boolean isValidator(DocumentModel document,
            NuxeoPrincipal principal) throws PublishingException {
        try {
            String[] validators = getValidatorsFor(document);
            for (String s : validators) {
                if (principal.getName().equals(s) || principal.isMemberOf(s)) {
                    return true;
                }
            }
        } catch (PublishingValidatorException e) {
            throw new PublishingException(e);
        }
        return false;
    }

    protected void restrictPermission(DocumentModel newProxy,
            NuxeoPrincipal principal, CoreSession coreSession, ACL acl)
            throws PublishingValidatorException, PublishingException {
        ChangePermissionUnrestricted permissionChanger = new ChangePermissionUnrestricted(
                coreSession, newProxy, getValidatorsFor(newProxy), principal,
                ACL_NAME, acl);
        try {
            permissionChanger.runUnrestricted();
        } catch (ClientException e) {
            throw new PublishingException(e);
        }
    }

    protected void createTask(DocumentModel document, CoreSession session,
            NuxeoPrincipal principal) throws PublishingValidatorException,
            NuxeoJbpmException, PublishingException {
        TaskInstance ti = new TaskInstance();
        String[] actorIds = getValidatorsFor(document);
        List<String> prefixedActorIds = new ArrayList<String>();
        for (String s : actorIds) {
            if (s.contains(":")) {
                prefixedActorIds.add(s);
            } else {
                UserManager userManager = getUserManager();
                String prefix;
                try {
                    prefix = userManager.getPrincipal(s) == null ? NuxeoGroup.PREFIX
                            : NuxeoPrincipal.PREFIX;
                } catch (ClientException e) {
                    throw new ClientRuntimeException(e);
                }
                prefixedActorIds.add(prefix + s);
            }
        }
        ti.setPooledActors(prefixedActorIds.toArray(new String[prefixedActorIds.size()]));
        Map<String, Serializable> variables = new HashMap<String, Serializable>();
        variables.put(JbpmService.VariableName.documentId.name(),
                document.getId());
        variables.put(JbpmService.VariableName.documentRepositoryName.name(),
                document.getRepositoryName());
        variables.put(JbpmService.VariableName.initiator.name(),
                principal.getName());
        ti.setVariables(variables);
        ti.setName(TASK_NAME);
        ti.setCreate(new Date());
        getJbpmService().saveTaskInstances(Collections.singletonList(ti));
        DocumentEventContext ctx = new DocumentEventContext(session, principal,
                document);
        ctx.setProperty(NotificationConstants.RECIPIENTS_KEY, actorIds);
        try {
            getEventProducer().fireEvent(
                    ctx.newEvent(JbpmEventNames.WORKFLOW_TASK_ASSIGNED));
            getEventProducer().fireEvent(
                    ctx.newEvent(JbpmEventNames.WORKFLOW_TASK_START));
        } catch (ClientException e) {
            throw new PublishingException(e);
        }

    }

    private UserManager getUserManager() {
        if (userManager == null) {
            try {
                userManager = Framework.getService(UserManager.class);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return userManager;
    }

    protected JbpmService getJbpmService() {
        if (jbpmService == null) {
            try {
                jbpmService = Framework.getService(JbpmService.class);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Jbpm service is not deployed.", e);
            }
        }
        return jbpmService;
    }

    protected EventProducer getEventProducer() throws ClientException {
        if (eventProducer == null) {
            try {
                eventProducer = Framework.getService(EventProducer.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return eventProducer;
    }

    protected void notifyEvent(PublishingEvent event, DocumentModel doc,
            CoreSession coreSession) throws PublishingException {
        try {
            notifyEvent(event.name(), null, null, null, doc, coreSession);
        } catch (ClientException e) {
            throw new PublishingException(e);
        }
    }

    protected void notifyEvent(String eventId,
            Map<String, Serializable> properties, String comment,
            String category, DocumentModel dm, CoreSession coreSession)
            throws ClientException {
        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }
        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }
        properties.put(CoreEventConstants.REPOSITORY_NAME,
                dm.getRepositoryName());
        properties.put(CoreEventConstants.SESSION_ID,
                coreSession.getSessionId());
        properties.put(CoreEventConstants.DOC_LIFE_CYCLE,
                dm.getCurrentLifeCycleState());

        DocumentEventContext ctx = new DocumentEventContext(coreSession,
                coreSession.getPrincipal(), dm);
        ctx.setProperties(properties);
        ctx.setComment(comment);
        ctx.setCategory(category);

        Event event = ctx.newEvent(eventId);

        EventProducer evtProducer;
        try {
            evtProducer = Framework.getService(EventProducer.class);
        } catch (Exception e) {
            throw new ClientException(e);
        }

        try {
            evtProducer.fireEvent(event);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    @Override
    public void validatorPublishDocument(PublishedDocument publishedDocument,
            String comment) throws PublishingException {
        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
        NuxeoPrincipal principal = (NuxeoPrincipal) coreSession.getPrincipal();
        try {
            removeACL(proxy, coreSession);
            endTask(proxy, principal, coreSession, comment,
                    PublishingEvent.documentPublicationApproved);
            notifyEvent(PublishingEvent.documentPublicationApproved, proxy,
                    coreSession);
            notifyEvent(PublishingEvent.documentPublished, proxy, coreSession);
        } catch (ClientException e) {
            throw new PublishingException(e);
        }
        ((SimpleCorePublishedDocument) publishedDocument).setPending(false);
    }

    protected void removeACL(DocumentModel document, CoreSession coreSession)
            throws PublishingException {
        try {
            RemoveACLUnrestricted remover = new RemoveACLUnrestricted(
                    coreSession, document, ACL_NAME);
            remover.runUnrestricted();
        } catch (ClientException e) {
            throw new PublishingException(e);
        }
    }

    protected void endTask(DocumentModel document, NuxeoPrincipal currentUser,
            CoreSession session, String comment, PublishingEvent event)
            throws PublishingException {
        try {
            List<TaskInstance> tis = getJbpmService().getTaskInstances(
                    document, currentUser, null);
            String initiator = null;
            for (TaskInstance ti : tis) {
                if (ti.getName().equals(TASK_NAME)) {
                    initiator = (String) ti.getVariable(JbpmService.VariableName.initiator.name());
                    ti.end();
                    jbpmService.saveTaskInstances(Collections.singletonList(ti));
                    break;
                }
            }
            GetsProxySourceDocumentsUnrestricted runner = new GetsProxySourceDocumentsUnrestricted(
                    session, document);
            runner.runUnrestricted();
            Map<String, Serializable> properties = new HashMap<String, Serializable>();
            if (initiator != null) {
                properties.put(NotificationConstants.RECIPIENTS_KEY,
                        new String[] { initiator });
            }
            notifyEvent(event.name(), properties, comment, null,
                    runner.liveDocument, session);
        } catch (NuxeoJbpmException e) {
            throw new PublishingException(e);
        } catch (ClientException ce) {
            throw new PublishingException(ce);
        }
    }

    @Override
    public void validatorRejectPublication(PublishedDocument publishedDocument,
            String comment) throws PublishingException {
        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
        NuxeoPrincipal principal = (NuxeoPrincipal) coreSession.getPrincipal();
        try {
            notifyEvent(PublishingEvent.documentPublicationRejected, proxy,
                    coreSession);
            removeProxy(proxy, coreSession);
            endTask(proxy, principal, coreSession, comment,
                    PublishingEvent.documentPublicationRejected);
        } catch (ClientException e) {
            throw new PublishingException(e);
        }
    }

    protected void removeProxy(DocumentModel doc, CoreSession coreSession)
            throws PublishingException {
        try {
            DeleteDocumentUnrestricted deleter = new DeleteDocumentUnrestricted(
                    coreSession, doc);
            deleter.runUnrestricted();
        } catch (ClientException e) {
            throw new PublishingException(e);
        }
    }

    @Override
    public PublishedDocument wrapDocumentModel(DocumentModel doc)
            throws ClientException {
        SimpleCorePublishedDocument publishedDocument = (SimpleCorePublishedDocument) super.wrapDocumentModel(doc);
        if (!isPublished(publishedDocument)) {
            publishedDocument.setPending(true);
        }
        return publishedDocument;
    }

    protected boolean isPublished(PublishedDocument publishedDocument)
            throws PublishingException {
        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
        try {
            List<TaskInstance> tis = getJbpmService().getTaskInstances(proxy,
                    (NuxeoPrincipal) null, null);
            for (TaskInstance ti : tis) {
                if (ti.getName().equals(TASK_NAME)) {
                    // if there is a task on this doc, then it is not yet
                    // published
                    return false;
                }
            }
        } catch (NuxeoJbpmException e) {
            throw new PublishingException(e);
        }
        return true;
    }

    @Override
    public boolean canManagePublishing(PublishedDocument publishedDocument)
            throws ClientException {
        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
        NuxeoPrincipal currentUser = (NuxeoPrincipal) coreSession.getPrincipal();
        return proxy.isProxy() && hasValidationTask(proxy, currentUser);
    }

    protected boolean hasValidationTask(DocumentModel proxy,
            NuxeoPrincipal currentUser) throws ClientException {
        assert currentUser != null;
        try {
            List<TaskInstance> tis = getJbpmService().getTaskInstances(proxy,
                    currentUser, null);
            for (TaskInstance ti : tis) {
                if (ti.getName().equals(TASK_NAME)) {
                    return true;
                }
            }
        } catch (NuxeoJbpmException e) {
            throw new PublishingException(e);
        }
        return false;
    }

    @Override
    public boolean hasValidationTask(PublishedDocument publishedDocument)
            throws ClientException {
        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
        NuxeoPrincipal currentUser = (NuxeoPrincipal) coreSession.getPrincipal();
        return hasValidationTask(proxy, currentUser);
    }

    private class GetsProxySourceDocumentsUnrestricted extends
            UnrestrictedSessionRunner {

        public DocumentModel liveDocument;

        private DocumentModel sourceDocument;

        private final DocumentModel document;

        public GetsProxySourceDocumentsUnrestricted(CoreSession session,
                DocumentModel proxy) {
            super(session);
            this.document = proxy;
        }

        @Override
        public void run() throws ClientException {
            sourceDocument = session.getDocument(new IdRef(
                    document.getSourceId()));
            liveDocument = session.getDocument(new IdRef(
                    sourceDocument.getSourceId()));
        }
    }
}
