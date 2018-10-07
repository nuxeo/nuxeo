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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.task;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
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
import org.nuxeo.ecm.platform.publisher.impl.core.CoreFolderPublicationNode;
import org.nuxeo.ecm.platform.publisher.impl.core.CoreProxyFactory;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.publisher.rules.ValidatorsRule;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of the {@link PublishedDocumentFactory} for core implementation using native proxy system with
 * validation workflow.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class CoreProxyWithWorkflowFactory extends CoreProxyFactory implements PublishedDocumentFactory {

    public static final String TASK_NAME = "org.nuxeo.ecm.platform.publisher.task.CoreProxyWithWorkflowFactory";

    public static final String ACL_NAME = "org.nuxeo.ecm.platform.publisher.task.CoreProxyWithWorkflowFactory";

    // XXX ataillefer: remove if refactor old JBPM ACL name
    public static final String JBPM_ACL_NAME = "org.nuxeo.ecm.platform.publisher.jbpm.CoreProxyWithWorkflowFactory";

    public static final String PUBLISH_TASK_TYPE = "publish_moderate";

    public static final String LOOKUP_STATE_PARAM_KEY = "lookupState";

    public static final String LOOKUP_STATE_PARAM_BYACL = "byACL";

    public static final String LOOKUP_STATE_PARAM_BYTASK = "byTask";

    protected LookupState lookupState = new LookupStateByACL();

    @Override
    public void init(CoreSession coreSession, ValidatorsRule validatorsRule, Map<String, String> parameters) {
        super.init(coreSession, validatorsRule, parameters);
        // setup lookup state strategy if requested
        String lookupState = parameters.get(LOOKUP_STATE_PARAM_KEY);
        if (lookupState != null) {
            if (LOOKUP_STATE_PARAM_BYACL.equals(lookupState)) {
                setLookupByACL();
            } else if (LOOKUP_STATE_PARAM_BYTASK.equals(lookupState)) {
                setLookupByTask();
            }
        }
    }

    public void setLookupByTask() {
        this.lookupState = new LookupStateByTask();
    }

    public void setLookupByACL() {
        this.lookupState = new LookupStateByACL();
    }

    @Override
    public PublishedDocument publishDocument(DocumentModel doc, PublicationNode targetNode,
            Map<String, String> params) {
        DocumentModel targetDocModel;
        if (targetNode instanceof CoreFolderPublicationNode) {
            CoreFolderPublicationNode coreNode = (CoreFolderPublicationNode) targetNode;
            targetDocModel = coreNode.getTargetDocumentModel();
        } else {
            targetDocModel = coreSession.getDocument(new PathRef(targetNode.getPath()));
        }
        NuxeoPrincipal principal = coreSession.getPrincipal();
        DocumentPublisherUnrestricted runner = new DocumentPublisherUnrestricted(coreSession, doc.getRef(),
                targetDocModel.getRef(), principal, null);
        runner.runUnrestricted();
        return runner.getPublishedDocument();
    }

    protected boolean isPublishedDocWaitingForPublication(DocumentModel doc, CoreSession session) {
        return !lookupState.isPublished(doc, session);
    }

    protected boolean isValidator(DocumentModel document, NuxeoPrincipal principal) {
        String[] validators = getValidatorsFor(document);
        for (String s : validators) {
            if (principal.getName().equals(s) || principal.isMemberOf(s)) {
                return true;
            }
        }
        return false;
    }

    protected void restrictPermission(DocumentModel newProxy, NuxeoPrincipal principal, CoreSession coreSession,
            ACL acl) {
        ChangePermissionUnrestricted permissionChanger = new ChangePermissionUnrestricted(coreSession, newProxy,
                getValidatorsFor(newProxy), principal, ACL_NAME, acl);
        permissionChanger.runUnrestricted();
    }

    protected void createTask(DocumentModel document, CoreSession session, NuxeoPrincipal principal) {
        String[] actorIds = getValidatorsFor(document);
        Map<String, String> variables = new HashMap<String, String>();
        variables.put(Task.TaskVariableName.needi18n.name(), "true");
        variables.put(Task.TaskVariableName.taskType.name(), PUBLISH_TASK_TYPE);
        variables.put(TaskService.VariableName.documentId.name(), document.getId());
        variables.put(TaskService.VariableName.documentRepositoryName.name(), document.getRepositoryName());
        variables.put(TaskService.VariableName.initiator.name(), principal.getName());

        getTaskService().createTask(session, principal, document, TASK_NAME, Arrays.asList(actorIds), false, TASK_NAME,
                null, null, variables, null);
        DocumentEventContext ctx = new DocumentEventContext(session, principal, document);
        ctx.setProperty(NotificationConstants.RECIPIENTS_KEY, actorIds);
        getEventProducer().fireEvent(ctx.newEvent(TaskEventNames.WORKFLOW_TASK_START));
    }

    protected TaskService getTaskService() {
        return Framework.getService(TaskService.class);
    }

    protected void removeExistingProxiesOnPreviousVersions(DocumentModel newProxy) {
        new UnrestrictedSessionRunner(coreSession) {
            @Override
            public void run() {
                DocumentModel sourceVersion = session.getSourceDocument(newProxy.getRef());
                DocumentModel dm = session.getSourceDocument(sourceVersion.getRef());
                DocumentModelList brothers = session.getProxies(dm.getRef(), newProxy.getParentRef());
                if (brothers != null && brothers.size() > 1) {
                    // we remove the brothers of the published document if any
                    // the use case is:
                    // v1 is published, v2 is waiting for publication and was just
                    // validated
                    // v1 is removed and v2 is now being published
                    for (DocumentModel doc : brothers) {
                        if (!doc.getId().equals(newProxy.getId())) {
                            session.removeDocument(doc.getRef());
                        }
                    }
                }
            }
        }.runUnrestricted();
    }

    @Override
    public void validatorPublishDocument(PublishedDocument publishedDocument, String comment) {
        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
        NuxeoPrincipal principal = coreSession.getPrincipal();
        removeExistingProxiesOnPreviousVersions(proxy);
        removeACL(proxy, coreSession);
        endTask(proxy, principal, coreSession, comment, PublishingEvent.documentPublicationApproved);
        notifyEvent(PublishingEvent.documentPublicationApproved, proxy, coreSession);
        notifyEvent(PublishingEvent.documentPublished, proxy, coreSession);
        ((SimpleCorePublishedDocument) publishedDocument).setPending(false);
    }

    protected void removeACL(DocumentModel document, CoreSession coreSession) {
        RemoveACLUnrestricted remover = new RemoveACLUnrestricted(coreSession, document, ACL_NAME, JBPM_ACL_NAME);
        remover.runUnrestricted();
    }

    protected void endTask(DocumentModel document, NuxeoPrincipal currentUser, CoreSession session, String comment,
            PublishingEvent event) {
        List<Task> tis = getTaskService().getTaskInstances(document, currentUser, session);
        String initiator = null;
        for (Task task : tis) {
            if (task.getName().equals(TASK_NAME)) {
                initiator = (String) task.getVariable(TaskService.VariableName.initiator.name());
                task.end(session);
                // make sure taskDoc is attached to prevent sending event with null session
                DocumentModel taskDocument = task.getDocument();
                if (taskDocument.getSessionId() == null) {
                    taskDocument.attach(coreSession.getSessionId());
                }
                session.saveDocument(taskDocument);
                break;
            }
        }
        DocumentModel liveDoc = getLiveDocument(session, document);
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        if (initiator != null) {
            properties.put(NotificationConstants.RECIPIENTS_KEY, new String[] { initiator });
        }
        notifyEvent(event.name(), properties, comment, null, liveDoc, session);
    }

    protected DocumentModel getLiveDocument(CoreSession session, DocumentModel proxy) {
        GetsProxySourceDocumentsUnrestricted runner = new GetsProxySourceDocumentsUnrestricted(session, proxy);
        runner.runUnrestricted();
        return runner.liveDocument;
    }

    @Override
    public void validatorRejectPublication(PublishedDocument publishedDocument, String comment) {
        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
        NuxeoPrincipal principal = coreSession.getPrincipal();
        notifyEvent(PublishingEvent.documentPublicationRejected, proxy, coreSession);
        endTask(proxy, principal, coreSession, comment, PublishingEvent.documentPublicationRejected);
        removeProxy(proxy, coreSession);
    }

    protected void removeProxy(DocumentModel doc, CoreSession coreSession) {
        DeleteDocumentUnrestricted deleter = new DeleteDocumentUnrestricted(coreSession, doc);
        deleter.runUnrestricted();
    }

    @Override
    public PublishedDocument wrapDocumentModel(DocumentModel doc) {
        final SimpleCorePublishedDocument publishedDocument = (SimpleCorePublishedDocument) super.wrapDocumentModel(
                doc);

        new UnrestrictedSessionRunner(coreSession) {
            @Override
            public void run() {
                if (!isPublished(publishedDocument, session)) {
                    publishedDocument.setPending(true);
                }

            }
        }.runUnrestricted();

        return publishedDocument;
    }

    protected boolean isPublished(PublishedDocument publishedDocument, CoreSession session) {
        // FIXME: should be cached
        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
        return lookupState.isPublished(proxy, session);
    }

    @Override
    public boolean canManagePublishing(PublishedDocument publishedDocument) {
        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
        NuxeoPrincipal currentUser = coreSession.getPrincipal();
        return proxy.isProxy() && hasValidationTask(proxy, currentUser);
    }

    protected boolean hasValidationTask(DocumentModel proxy, NuxeoPrincipal currentUser) {
        assert currentUser != null;
        List<Task> tasks = getTaskService().getTaskInstances(proxy, currentUser, coreSession);
        for (Task task : tasks) {
            if (task.getName().equals(TASK_NAME)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasValidationTask(PublishedDocument publishedDocument) {
        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
        NuxeoPrincipal currentUser = coreSession.getPrincipal();
        return hasValidationTask(proxy, currentUser);
    }

    private class GetsProxySourceDocumentsUnrestricted extends UnrestrictedSessionRunner {

        public DocumentModel liveDocument;

        private DocumentModel sourceDocument;

        private final DocumentModel document;

        public GetsProxySourceDocumentsUnrestricted(CoreSession session, DocumentModel proxy) {
            super(session);
            this.document = proxy;
        }

        @Override
        public void run() {
            sourceDocument = session.getDocument(new IdRef(document.getSourceId()));
            liveDocument = session.getDocument(new IdRef(sourceDocument.getSourceId()));
        }
    }

    /**
     * @author arussel
     */
    protected class DocumentPublisherUnrestricted extends UnrestrictedSessionRunner {

        protected PublishedDocument result;

        protected DocumentRef docRef;

        protected DocumentRef targetRef;

        protected NuxeoPrincipal principal;

        protected String comment = "";

        public DocumentPublisherUnrestricted(CoreSession session, DocumentRef docRef, DocumentRef targetRef,
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
        public void run() {
            DocumentModelList list = session.getProxies(docRef, targetRef);
            DocumentModel proxy = null;
            if (list.isEmpty()) {// first publication
                proxy = session.publishDocument(session.getDocument(docRef), session.getDocument(targetRef));
                SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(proxy);
                session.save();
                if (!isValidator(proxy, principal)) {
                    notifyEvent(PublishingEvent.documentWaitingPublication, coreSession.getDocument(proxy.getRef()),
                            coreSession);
                    restrictPermission(proxy, principal, session, null);
                    createTask(proxy, coreSession, principal);
                    publishedDocument.setPending(true);
                } else {
                    notifyEvent(PublishingEvent.documentPublished, proxy, coreSession);
                }
                result = publishedDocument;
            } else if (list.size() == 1) {
                // one doc is already published or waiting for publication
                if (isPublishedDocWaitingForPublication(list.get(0), session)) {
                    proxy = session.publishDocument(session.getDocument(docRef), session.getDocument(targetRef));
                    if (!isValidator(proxy, principal)) {
                        // we're getting the old proxy acl
                        ACL acl = list.get(0).getACP().getACL(ACL_NAME);
                        acl.add(0, new ACE(principal.getName(), SecurityConstants.READ, true));
                        ACP acp = proxy.getACP();
                        acp.addACL(acl);
                        session.setACP(proxy.getRef(), acp, true);
                        session.save();
                        SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(proxy);
                        publishedDocument.setPending(true);
                        result = publishedDocument;
                    } else {
                        endTask(proxy, principal, coreSession, "", PublishingEvent.documentPublicationApproved);
                        notifyEvent(PublishingEvent.documentPublished, proxy, coreSession);
                        ACP acp = proxy.getACP();
                        acp.removeACL(ACL_NAME);
                        session.setACP(proxy.getRef(), acp, true);
                        session.save();
                        result = new SimpleCorePublishedDocument(proxy);
                    }
                } else {
                    if (!isValidator(list.get(0), principal)) {
                        proxy = session.publishDocument(session.getDocument(docRef), session.getDocument(targetRef),
                                false);
                        // save needed to have the proxy visible from other
                        // sessions in non-JCA mode
                        session.save();
                        SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(proxy);
                        notifyEvent(PublishingEvent.documentWaitingPublication, proxy, coreSession);
                        restrictPermission(proxy, principal, coreSession, null);
                        session.save(); // process invalidations (non-JCA)
                        createTask(proxy, coreSession, principal);
                        publishedDocument.setPending(true);
                        result = publishedDocument;
                    } else {
                        proxy = session.publishDocument(session.getDocument(docRef), session.getDocument(targetRef));
                        // save needed to have the proxy visible from other
                        // sessions in non-JCA mode
                        session.save();
                        notifyEvent(PublishingEvent.documentPublished, proxy, coreSession);
                        SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(proxy);
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
                    acl.add(0, new ACE(principal.getName(), SecurityConstants.READ, true));
                    // remove publishedDoc
                    ACP acp = session.getACP(waitingForPublicationDoc.getRef());
                    acp.addACL(acl);
                    session.setACP(waitingForPublicationDoc.getRef(), acp, true);
                    SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(
                            waitingForPublicationDoc);
                    publishedDocument.setPending(true);
                    result = publishedDocument;
                } else {
                    endTask(waitingForPublicationDoc, principal, coreSession, comment,
                            PublishingEvent.documentPublicationApproved);
                    session.removeDocument(waitingForPublicationDoc.getRef());
                    proxy = session.publishDocument(session.getDocument(docRef), session.getDocument(targetRef));
                    notifyEvent(PublishingEvent.documentPublished, proxy, coreSession);
                    SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(proxy);
                    result = publishedDocument;
                }
            }
            if (proxy != null) {
                proxy.detach(true);
            }
        }
    }

}
