/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.publishing.jbpm;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.ecm.platform.publishing.AbstractPublisher;
import org.nuxeo.ecm.platform.publishing.ChangePermissionUnrestricted;
import org.nuxeo.ecm.platform.publishing.api.DocumentWaitingValidationException;
import org.nuxeo.ecm.platform.publishing.api.Publisher;
import org.nuxeo.ecm.platform.publishing.api.PublishingException;
import org.nuxeo.ecm.platform.publishing.api.PublishingService;
import org.nuxeo.ecm.platform.publishing.api.PublishingValidatorException;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 *
 */
public class JbpmPublisher extends AbstractPublisher implements Publisher {
    public static String TASK_NAME = "org.nuxeo.ecm.platform.publishing.jbpm.JbpmPublisher";

    public static String ACL_NAME = "org.nuxeo.ecm.platform.publishing.jbpm.JbpmPublisher";

    public static enum PublishingEvent {
        documentPublished, documentSubmittedForPublication, documentPublicationRejected, documentPublicationApproved, documentWaitingPublication
    };

    private final JbpmService jbpmService;

    private PublishingService publishingService;

    public PublishingService getPublishingService() {
        if (publishingService == null) {
            try {
                publishingService = Framework.getService(PublishingService.class);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return publishingService;
    }

    public JbpmPublisher() {
        try {
            jbpmService = Framework.getService(JbpmService.class);
        } catch (Exception e) {
            throw new IllegalStateException("Jbpm service is not deployed.", e);
        }
    }

    public boolean canManagePublishing(DocumentModel currentDocument,
            NuxeoPrincipal currentUser) throws PublishingException {
        if (currentDocument.isProxy()
                && hasValidationTask(currentDocument, currentUser)) {
            return true;
        }
        return false;

    }

    public boolean hasValidationTask(DocumentModel proxy,
            NuxeoPrincipal currentUser) throws PublishingException {
        assert currentUser != null;
        try {
            List<TaskInstance> tis = jbpmService.getTaskInstances(proxy,
                    currentUser, null);
            for (TaskInstance ti : tis) {
                if (ti.getName().equals(TASK_NAME)
                        && ti.getPooledActors().contains(currentUser.getName())) {
                    return true;
                }
            }
        } catch (NuxeoJbpmException e) {
            throw new PublishingException(e);
        }
        return false;
    }

    public boolean isPublished(DocumentModel proxy) throws PublishingException {
        try {
            List<TaskInstance> tis = jbpmService.getTaskInstances(proxy, null,
                    null);
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

    public void submitToPublication(DocumentModel document,
            DocumentModel placeToPublishTo, NuxeoPrincipal principal)
            throws PublishingException, DocumentWaitingValidationException {
        DocumentModel newProxy;
        CoreSession coreSession;
        try {
            coreSession = getCoreSession(document.getRepositoryName(),
                    principal);
        } catch (ClientException e2) {
            throw new IllegalStateException("No core session available.", e2);
        }
        try {
            newProxy = publish(document, placeToPublishTo, principal,
                    coreSession);
            notifyEvent(PublishingEvent.documentSubmittedForPublication,
                    newProxy, coreSession);
        } catch (ClientException e1) {
            throw new PublishingException(e1);
        }
        if (!isValidator(newProxy, principal)) {
            try {
                notifyEvent(PublishingEvent.documentWaitingPublication,
                        newProxy, coreSession);
                restrictPermission(newProxy, principal, coreSession);
                createTask(newProxy);
                throw new DocumentWaitingValidationException();
            } catch (PublishingValidatorException e) {
                throw new PublishingException(e);
            } catch (NuxeoJbpmException e) {
                throw new PublishingException(e);
            }
        } else {
            notifyEvent(PublishingEvent.documentPublished, newProxy,
                    coreSession);
        }
    }

    protected void createTask(DocumentModel document)
            throws PublishingValidatorException, NuxeoJbpmException {
        TaskInstance ti = new TaskInstance();
        String[] actorIds = getPublishingService().getValidatorsFor(document);
        ti.setPooledActors(actorIds);
        Map<String, Serializable> variables = new HashMap<String, Serializable>();
        variables.put(JbpmService.VariableName.documentId.name(),
                document.getId());
        variables.put(JbpmService.VariableName.documentRepositoryName.name(),
                document.getRepositoryName());
        ti.setVariables(variables);
        ti.setName(TASK_NAME);
        ti.setCreate(new Date());
        jbpmService.saveTaskInstances(Collections.singletonList(ti));
    }

    protected void restrictPermission(DocumentModel newProxy,
            NuxeoPrincipal principal, CoreSession coreSession)
            throws PublishingValidatorException, PublishingException {
        ChangePermissionUnrestricted permissionChanger = new ChangePermissionUnrestricted(
                coreSession, newProxy, getPublishingService().getValidatorsFor(
                        newProxy), principal, ACL_NAME);
        try {
            permissionChanger.runUnrestricted();
        } catch (ClientException e) {
            throw new PublishingException(e);
        }
    }

    public boolean isValidator(DocumentModel document, NuxeoPrincipal principal)
            throws PublishingException {
        try {
            String[] validators = getPublishingService().getValidatorsFor(
                    document);
            for (String s : validators) {
                if (principal.getName().equals(s)) {
                    return true;
                }
            }
        } catch (PublishingValidatorException e) {
            throw new PublishingException(e);
        }
        return false;
    }

    public void notifyEvent(PublishingEvent event, DocumentModel doc,
            CoreSession coreSession) throws PublishingException {
        try {
            notifyEvent(event.name(), null, null, null, doc, coreSession);
        } catch (ClientException e) {
            throw new PublishingException(e);
        }
    }

    public void validatorPublishDocument(DocumentModel currentDocument,
            NuxeoPrincipal currentUser) throws PublishingException {
        removeACL(currentDocument);
        endTask(currentDocument, currentUser);
        notifyEvent(PublishingEvent.documentPublicationApproved,
                currentDocument, null);
        notifyEvent(PublishingEvent.documentPublished, currentDocument, null);
    }

    private void endTask(DocumentModel document, NuxeoPrincipal currentUser)
            throws PublishingException {
        try {
            List<TaskInstance> tis = jbpmService.getTaskInstances(document,
                    currentUser, null);
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

    private void removeACL(DocumentModel document) throws PublishingException {
        try {
            document.getACP().removeACL(ACL_NAME);
            CoreSession session = getCoreSession(document.getRepositoryName(),
                    null);
            session.saveDocument(document);
            session.save();
        } catch (ClientException e) {
            throw new PublishingException(e);
        }

    }

    public void validatorRejectPublication(DocumentModel doc,
            NuxeoPrincipal principal, String comment)
            throws PublishingException {
        notifyEvent(PublishingEvent.documentPublicationRejected, doc, null);
        removeProxy(doc);
        endTask(doc, principal);

    }

    private void removeProxy(DocumentModel doc) throws PublishingException {
        try {
            CoreSession session = getCoreSession(doc.getRepositoryName(), null);
            session.removeDocument(doc.getRef());
            session.save();
        } catch (ClientException e) {
            throw new PublishingException(e);
        }
    }

}
