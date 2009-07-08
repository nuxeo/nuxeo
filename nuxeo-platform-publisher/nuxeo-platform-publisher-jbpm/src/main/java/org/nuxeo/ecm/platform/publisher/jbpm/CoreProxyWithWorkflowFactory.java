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

import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishingEvent;
import org.nuxeo.ecm.platform.publisher.impl.core.CoreProxyFactory;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.publisher.rules.PublishingValidatorException;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.JbpmEventNames;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import java.util.*;
import java.io.Serializable;

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

    public static final String TASK_NAME = "org.nuxeo.ecm.platform.publishing.jbpm.JbpmPublisher";
    public static final String ACL_NAME = "org.nuxeo.ecm.platform.publishing.jbpm.JbpmPublisher";

    private JbpmService jbpmService;

    private EventProducer eventProducer;

    @Override
    public PublishedDocument publishDocument(DocumentModel doc, PublicationNode targetNode, Map<String, String> params) throws ClientException {
        SimpleCorePublishedDocument publishedDocument = (SimpleCorePublishedDocument) super.publishDocument(doc, targetNode, params);

        NuxeoPrincipal principal = (NuxeoPrincipal) coreSession.getPrincipal();
        DocumentModel proxy = publishedDocument.getProxy();

        if (!isValidator(proxy, principal)) {
            try {
                notifyEvent(PublishingEvent.documentWaitingPublication,
                        proxy, coreSession);
                restrictPermission(proxy, principal, coreSession);
                createTask(proxy, coreSession, principal);
                publishedDocument.setPending(true);
            } catch (PublishingValidatorException e) {
                throw new PublishingException(e);
            } catch (NuxeoJbpmException e) {
                throw new PublishingException(e);
            }
        } else {
            notifyEvent(PublishingEvent.documentPublished, proxy,
                    coreSession);
        }

        return publishedDocument;
    }

    public boolean isValidator(DocumentModel document, NuxeoPrincipal principal)
            throws PublishingException {
        try {
            String[] validators = publicationTree.getValidatorsFor(
                    document);
            for (String s : validators) {
                if (principal.getName().equals(s)
                        || principal.isMemberOf(s)) {
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
                coreSession, newProxy, publicationTree.getValidatorsFor(
                        newProxy), principal, ACL_NAME);
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
        String[] actorIds = getPublishingService().getValidatorsFor(document);
        List<String> prefixedActorIds = new ArrayList<String>();
        for (String s : actorIds) {
            if (s.contains(":")) {
                prefixedActorIds.add(s);
            } else {
                prefixedActorIds.add(NuxeoPrincipal.PREFIX + s);
            }
        }
        ti.setPooledActors(prefixedActorIds.toArray(new String[] {}));
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
            getEventProducer().fireEvent(ctx.newEvent(JbpmEventNames.WORKFLOW_TASK_START));
        } catch (ClientException e) {
            throw new PublishingException(e);
        }

    }


}
