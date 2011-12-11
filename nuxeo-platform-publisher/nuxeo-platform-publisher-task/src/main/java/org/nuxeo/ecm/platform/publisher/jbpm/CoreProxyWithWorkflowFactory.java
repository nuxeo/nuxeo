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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublishingEvent;
import org.nuxeo.ecm.platform.publisher.api.PublishingException;
import org.nuxeo.ecm.platform.publisher.impl.core.CoreFolderPublicationNode;
import org.nuxeo.ecm.platform.publisher.impl.core.CoreProxyFactory;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.publisher.rules.PublishingValidatorException;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of the {@link PublishedDocumentFactory} for core
 * implementation using native proxy system with validation workflow.
 * 
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class CoreProxyWithWorkflowFactory extends CoreProxyFactory implements
        PublishedDocumentFactory {

    public static final String TASK_NAME = "org.nuxeo.ecm.platform.publisher.task.CoreProxyWithWorkflowFactory";

    public static final String ACL_NAME = "org.nuxeo.ecm.platform.publisher.task.CoreProxyWithWorkflowFactory";

    // TODO: remove when refactoring
    public static final String JBPM_ACL_NAME = "org.nuxeo.ecm.platform.publisher.jbpm.CoreProxyWithWorkflowFactory";

    public static final String PUBLISH_TASK_TYPE = "publish_moderate";

    protected TaskService taskService;

    protected UserManager userManager;

    @Override
    public PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {
        DocumentModel targetDocModel;
        if (targetNode instanceof CoreFolderPublicationNode) {
            CoreFolderPublicationNode coreNode = (CoreFolderPublicationNode) targetNode;
            targetDocModel = coreNode.getTargetDocumentModel();
        } else {
            targetDocModel = coreSession.getDocument(new PathRef(
                    targetNode.getPath()));
        }
        NuxeoPrincipal principal = (NuxeoPrincipal) coreSession.getPrincipal();
        DocumentPublisherUnrestricted runner = new DocumentPublisherUnrestricted(
                coreSession, doc.getRef(), targetDocModel.getRef(), principal,
                null);
        runner.runUnrestricted();
        return runner.getPublishedDocument();
    }

    protected boolean isPublishedDocWaitingForPublication(
            DocumentModel documentModel) throws ClientException {
        return documentModel.getACP().getACL(ACL_NAME) != null;
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
            ClientException, PublishingException {
        String[] actorIds = getValidatorsFor(document);
        Map<String, String> variables = new HashMap<String, String>();
        variables.put(Task.TaskVariableName.needi18n.name(), "true");
        variables.put(Task.TaskVariableName.taskType.name(), PUBLISH_TASK_TYPE);
        variables.put(TaskService.VariableName.documentId.name(),
                document.getId());
        variables.put(TaskService.VariableName.documentRepositoryName.name(),
                document.getRepositoryName());
        variables.put(TaskService.VariableName.initiator.name(),
                principal.getName());

        getTaskService().createTask(session, principal, document, TASK_NAME,
                Arrays.asList(actorIds), false, TASK_NAME, null, null,
                variables, null);
        DocumentEventContext ctx = new DocumentEventContext(session, principal,
                document);
        ctx.setProperty(NotificationConstants.RECIPIENTS_KEY, actorIds);
        try {
            getEventProducer().fireEvent(
                    ctx.newEvent(TaskEventNames.WORKFLOW_TASK_ASSIGNED));
            getEventProducer().fireEvent(
                    ctx.newEvent(TaskEventNames.WORKFLOW_TASK_START));
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

    protected TaskService getTaskService() {
        if (taskService == null) {
            try {
                taskService = Framework.getService(TaskService.class);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Task service is not deployed.", e);
            }
        }
        return taskService;
    }

    @Override
    public void validatorPublishDocument(PublishedDocument publishedDocument,
            String comment) throws PublishingException {
        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
        NuxeoPrincipal principal = (NuxeoPrincipal) coreSession.getPrincipal();
        try {

            DocumentModel sourceVersion = coreSession.getSourceDocument(proxy.getRef());
            DocumentModel dm = coreSession.getSourceDocument(sourceVersion.getRef());
            DocumentModelList brothers = coreSession.getProxies(dm.getRef(),
                    proxy.getParentRef());
            if (brothers != null && brothers.size() > 1) {
                // we remove the brothers of the published document if any
                // the use case is:
                // v1 is published, v2 is waiting for publication and was just
                // validated
                // v1 is removed and v2 is now being published
                for (DocumentModel doc : brothers) {
                    if (!doc.getId().equals(proxy.getId())) {
                        coreSession.removeDocument(doc.getRef());
                    }
                }
            }
        } catch (ClientException e1) {
            throw new PublishingException(e1.getMessage(), e1);
        }
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
                    coreSession, document, ACL_NAME, JBPM_ACL_NAME);
            remover.runUnrestricted();
        } catch (ClientException e) {
            throw new PublishingException(e);
        }
    }

    protected void endTask(DocumentModel document, NuxeoPrincipal currentUser,
            CoreSession session, String comment, PublishingEvent event)
            throws PublishingException {
        try {
            List<Task> tis = getTaskService().getTaskInstances(document,
                    currentUser, session);
            String initiator = null;
            for (Task task : tis) {
                if (task.getName().equals(TASK_NAME)) {
                    initiator = (String) task.getVariable(TaskService.VariableName.initiator.name());
                    task.end(session);
                    session.saveDocument(task.getDocument());
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
        // FIXME: should be cached
        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
        try {
            List<Task> tasks = getTaskService().getTaskInstances(proxy,
                    (NuxeoPrincipal) null, coreSession);
            for (Task task : tasks) {
                if (task.getName().equals(TASK_NAME)) {
                    // if there is a task on this doc, then it is not yet
                    // published
                    return false;
                }
            }
        } catch (ClientException e) {
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
            List<Task> tasks = getTaskService().getTaskInstances(proxy,
                    currentUser, coreSession);
            for (Task task : tasks) {
                if (task.getName().equals(TASK_NAME)) {
                    return true;
                }
            }
        } catch (ClientException e) {
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

    /**
     * @author arussel
     */
    protected class DocumentPublisherUnrestricted extends
            UnrestrictedSessionRunner {

        protected PublishedDocument result;

        protected DocumentRef docRef;

        protected DocumentRef targetRef;

        protected NuxeoPrincipal principal;

        protected String comment = "";

        public DocumentPublisherUnrestricted(CoreSession session,
                DocumentRef docRef, DocumentRef targetRef,
                NuxeoPrincipal principal, String comment) {
            super(session);
            this.docRef = docRef;
            this.targetRef = targetRef;
            this.principal = principal;
            this.comment = comment;
        }

        public PublishedDocument getPublishedDocument() {
            return result;
        }

        @Override
        public void run() throws ClientException {
            DocumentModelList list = session.getProxies(docRef, targetRef);
            DocumentModel proxy = null;
            if (list.isEmpty()) {// first publication
                proxy = session.publishDocument(session.getDocument(docRef),
                        session.getDocument(targetRef));
                SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(
                        proxy);
                session.save();
                if (!isValidator(proxy, principal)) {
                    notifyEvent(PublishingEvent.documentWaitingPublication,
                            coreSession.getDocument(proxy.getRef()),
                            coreSession);
                    restrictPermission(proxy, principal, session, null);
                    createTask(proxy, coreSession, principal);
                    publishedDocument.setPending(true);
                } else {
                    notifyEvent(PublishingEvent.documentPublished, proxy,
                            coreSession);
                }
                result = publishedDocument;
            } else if (list.size() == 1) {
                // one doc is already published or waiting for publication
                if (isPublishedDocWaitingForPublication(list.get(0))) {
                    proxy = session.publishDocument(
                            session.getDocument(docRef),
                            session.getDocument(targetRef));
                    if (!isValidator(proxy, principal)) {
                        // we're getting the old proxy acl
                        ACL acl = list.get(0).getACP().getACL(ACL_NAME);
                        acl.add(0, new ACE(principal.getName(),
                                SecurityConstants.READ, true));
                        ACP acp = proxy.getACP();
                        acp.addACL(acl);
                        session.setACP(proxy.getRef(), acp, true);
                        session.save();
                        SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(
                                proxy);
                        publishedDocument.setPending(true);
                        result = publishedDocument;
                    } else {
                        endTask(proxy, principal, coreSession, "",
                                PublishingEvent.documentPublicationApproved);
                        notifyEvent(PublishingEvent.documentPublished, proxy,
                                coreSession);
                        ACP acp = proxy.getACP();
                        acp.removeACL(ACL_NAME);
                        session.setACP(proxy.getRef(), acp, true);
                        session.save();
                        result = new SimpleCorePublishedDocument(proxy);
                    }
                } else {
                    if (!isValidator(list.get(0), principal)) {
                        proxy = session.publishDocument(
                                session.getDocument(docRef),
                                session.getDocument(targetRef), false);
                        // save needed to have the proxy visible from other
                        // sessions in non-JCA mode
                        session.save();
                        SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(
                                proxy);
                        notifyEvent(PublishingEvent.documentWaitingPublication,
                                proxy, coreSession);
                        restrictPermission(proxy, principal, coreSession, null);
                        session.save(); // process invalidations (non-JCA)
                        createTask(proxy, coreSession, principal);
                        publishedDocument.setPending(true);
                        result = publishedDocument;
                    } else {
                        proxy = session.publishDocument(
                                session.getDocument(docRef),
                                session.getDocument(targetRef));
                        // save needed to have the proxy visible from other
                        // sessions in non-JCA mode
                        session.save();
                        notifyEvent(PublishingEvent.documentPublished, proxy,
                                coreSession);
                        SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(
                                proxy);
                        result = publishedDocument;
                    }
                }
            } else if (list.size() == 2) {
                DocumentModel waitingForPublicationDoc = null;
                session.save(); // process invalidations (non-JCA)
                for (DocumentModel dm : list) {
                    if (session.getACP(dm.getRef()).getACL(ACL_NAME) != null) {
                        waitingForPublicationDoc = dm;
                    }
                }
                if (!isValidator(waitingForPublicationDoc, principal)) {
                    // we're getting the old proxy acl
                    ACL acl = waitingForPublicationDoc.getACP().getACL(ACL_NAME);
                    acl.add(0, new ACE(principal.getName(),
                            SecurityConstants.READ, true));
                    // remove publishedDoc
                    ACP acp = session.getACP(waitingForPublicationDoc.getRef());
                    acp.addACL(acl);
                    session.setACP(waitingForPublicationDoc.getRef(), acp, true);
                    SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(
                            waitingForPublicationDoc);
                    publishedDocument.setPending(true);
                    result = publishedDocument;
                } else {
                    endTask(waitingForPublicationDoc, principal, coreSession,
                            comment,
                            PublishingEvent.documentPublicationApproved);
                    session.removeDocument(waitingForPublicationDoc.getRef());
                    proxy = session.publishDocument(
                            session.getDocument(docRef),
                            session.getDocument(targetRef));
                    notifyEvent(PublishingEvent.documentPublished, proxy,
                            coreSession);
                    SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(
                            proxy);
                    result = publishedDocument;
                }
            }
            if (proxy != null) {
                detachDocument(proxy);
            }
        }

        private void detachDocument(DocumentModel doc) throws ClientException {
            if (doc instanceof DocumentModelImpl) {
                ((DocumentModelImpl) doc).detach(true);
            }
        }

    }

}
