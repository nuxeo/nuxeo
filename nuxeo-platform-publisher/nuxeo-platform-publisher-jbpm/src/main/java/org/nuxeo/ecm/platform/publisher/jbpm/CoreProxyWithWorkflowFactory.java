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

import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.jbpm.JbpmEventNames;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.ecm.platform.publisher.api.*;
import org.nuxeo.ecm.platform.publisher.impl.core.CoreFolderPublicationNode;
import org.nuxeo.ecm.platform.publisher.impl.core.CoreProxyFactory;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.publisher.rules.PublishingValidatorException;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.util.*;

/**
 *
 * Implementation of the {@link PublishedDocumentFactory} for core
 * implementation using native proxy system with validation workflow.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class CoreProxyWithWorkflowFactory extends CoreProxyFactory implements
        PublishedDocumentFactory {

    public static final String TASK_NAME = "org.nuxeo.ecm.platform.publisher.jbpm.CoreProxyWithWorkflowFactory";

    public static final String ACL_NAME = "org.nuxeo.ecm.platform.publisher.jbpm.CoreProxyWithWorkflowFactory";

    protected JbpmService jbpmService;

    protected EventProducer eventProducer;

    @Override
    public PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {
        DocumentModel proxy = publish(doc, targetNode, params);
        SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(
                proxy);
        NuxeoPrincipal principal = (NuxeoPrincipal) coreSession.getPrincipal();

        if (!isValidator(proxy, principal)) {
            try {
                notifyEvent(PublishingEvent.documentWaitingPublication, proxy,
                        coreSession);
                restrictPermission(proxy, principal, coreSession);
                createTask(proxy, coreSession, principal);
                publishedDocument.setPending(true);
            } catch (PublishingValidatorException e) {
                throw new PublishingException(e);
            } catch (NuxeoJbpmException e) {
                throw new PublishingException(e);
            }
        } else {
            notifyEvent(PublishingEvent.documentPublished, proxy, coreSession);
        }

        return publishedDocument;
    }

    protected DocumentModel publish(DocumentModel doc,
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
        boolean overwriteProxy = (!((params != null) && (params.containsKey("overwriteExistingProxy"))))
                || Boolean.parseBoolean(params.get("overwriteExistingProxy"));

        PublishUnrestricted publisher = new PublishUnrestricted(coreSession,
                doc, targetDocModel, overwriteProxy);
        publisher.runUnrestricted();
        return publisher.getModel();
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
            NuxeoPrincipal principal, CoreSession coreSession)
            throws PublishingValidatorException, PublishingException {
        ChangePermissionUnrestricted permissionChanger = new ChangePermissionUnrestricted(
                coreSession, newProxy, getValidatorsFor(newProxy), principal,
                ACL_NAME);
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
                prefixedActorIds.add(NuxeoPrincipal.PREFIX + s);
            }
        }
        ti.setPooledActors(prefixedActorIds.toArray(new String[prefixedActorIds.size()]));
        Map<String, Serializable> variables = new HashMap<String, Serializable>();
        variables.put(JbpmService.VariableName.documentId.name(),
                document.getId());
        variables.put(JbpmService.VariableName.documentRepositoryName.name(),
                document.getRepositoryName());
        ti.setVariables(variables);
        ti.setName(TASK_NAME);
        ti.setCreate(new Date());
        getJbpmService().saveTaskInstances(Collections.singletonList(ti));
        DocumentEventContext ctx = new DocumentEventContext(session, principal,
                document);
        ctx.setProperty("recipients", actorIds);
        try {
            getEventProducer().fireEvent(
                    ctx.newEvent(JbpmEventNames.WORKFLOW_TASK_ASSIGNED));
            getEventProducer().fireEvent(
                    ctx.newEvent(JbpmEventNames.WORKFLOW_TASK_START));
        } catch (ClientException e) {
            throw new PublishingException(e);
        }

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
    public void validatorPublishDocument(PublishedDocument publishedDocument)
            throws PublishingException {
        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
        NuxeoPrincipal principal = (NuxeoPrincipal) coreSession.getPrincipal();
        try {
            removeACL(proxy, coreSession);
            endTask(proxy, principal);
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

    protected void endTask(DocumentModel document, NuxeoPrincipal currentUser)
            throws PublishingException {
        try {
            List<TaskInstance> tis = getJbpmService().getTaskInstances(
                    document, currentUser, null);
            for (TaskInstance ti : tis) {
                if (ti.getName().equals(TASK_NAME)) {
                    ti.end();
                    jbpmService.saveTaskInstances(Collections.singletonList(ti));
                    return;
                }
            }
        } catch (NuxeoJbpmException e) {
            throw new PublishingException(e);
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
            endTask(proxy, principal);
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

    public boolean hasValidationTask(PublishedDocument publishedDocument) throws ClientException {
        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
        NuxeoPrincipal currentUser = (NuxeoPrincipal) coreSession.getPrincipal();
        return hasValidationTask(proxy, currentUser);
    }

}
