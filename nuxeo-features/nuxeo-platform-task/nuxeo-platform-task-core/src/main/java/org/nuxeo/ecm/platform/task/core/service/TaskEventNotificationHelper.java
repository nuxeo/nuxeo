/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.platform.task.core.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper for notifying task related events.
 *
 * @since 5.6
 */
public final class TaskEventNotificationHelper {

    private final static Log log = LogFactory.getLog(TaskEventNotificationHelper.class);

    public static void notifyEvent(CoreSession coreSession, DocumentModel document, NuxeoPrincipal principal,
            Task task, String eventId, Map<String, Serializable> properties, String comment, String category)
            {
        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }
        if (properties == null) {
            properties = new HashMap<>();
        }

        EventContext eventContext;
        if (document != null) {
            properties.put(CoreEventConstants.REPOSITORY_NAME, document.getRepositoryName());
            properties.put(CoreEventConstants.SESSION_ID, coreSession.getSessionId());
            properties.put(CoreEventConstants.DOC_LIFE_CYCLE, document.getCurrentLifeCycleState());
            eventContext = new DocumentEventContext(coreSession, principal, document);
        } else {
            eventContext = new EventContextImpl(coreSession, principal);
        }
        properties.put(DocumentEventContext.COMMENT_PROPERTY_KEY, comment);
        properties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY, category);
        properties.put(TaskService.TASK_INSTANCE_EVENT_PROPERTIES_KEY, task);
        String disableNotif = task.getVariable(TaskEventNames.DISABLE_NOTIFICATION_SERVICE);
        if (disableNotif != null && Boolean.TRUE.equals(Boolean.valueOf(disableNotif))) {
            properties.put(TaskEventNames.DISABLE_NOTIFICATION_SERVICE, Boolean.TRUE);
        }
        eventContext.setProperties(properties);

        Event event = eventContext.newEvent(eventId);
        getEventProducer().fireEvent(event);
    }

    /**
     * @since 7.2
     */
    public static void notifyTaskEnded(CoreSession coreSession, NuxeoPrincipal principal, Task task, String comment,
            String eventName, Map<String, Serializable> extraEventProperties) {

        // try to resolve document when notifying
        DocumentModel document;

        List<String> docIds = new ArrayList<>();
        docIds.addAll(task.getTargetDocumentsIds());
        // also handle compatibility with deprecated jbpm tasks
        String docIdVar = task.getVariable(TaskService.VariableName.documentId.name());
        if (!docIds.contains(docIdVar)) {
            docIds.add(docIdVar);
        }
        String docRepo = task.getVariable(TaskService.VariableName.documentRepositoryName.name());
        List<DocumentModel> documents = new ArrayList<>();
        if (coreSession.getRepositoryName().equals(docRepo)) {
            try {
                for (String id : docIds) {
                    document = coreSession.getDocument(new IdRef(id));
                    documents.add(document);
                }
            } catch (DocumentNotFoundException e) {
                log.error(String.format("Could not fetch document with id '%s:(%s)' for notification", docRepo, docIds), e);
            }
        } else {
            log.error(String.format("Could not resolve document for notification: "
                    + "document is on repository '%s' and given session is on " + "repository '%s'", docRepo,
                    coreSession.getRepositoryName()));
        }

        final Map<String, Serializable> eventProperties = new HashMap<>();
        ArrayList<String> notificationRecipients = new ArrayList<>();
        notificationRecipients.add(task.getInitiator());
        notificationRecipients.addAll(task.getActors());
        eventProperties.put(NotificationConstants.RECIPIENTS_KEY, notificationRecipients);
        if (extraEventProperties != null) {
            eventProperties.putAll(extraEventProperties);
        }
        boolean taskEndedByDelegatedActor = task.getDelegatedActors() != null
                && task.getDelegatedActors().contains(principal.getName());
        for (DocumentModel doc : documents) {
            notifyEvent(coreSession, doc, principal, task, eventName, eventProperties,
                    comment, null);
            if (taskEndedByDelegatedActor) {
                notifyEvent(
                        coreSession,
                        doc,
                        principal,
                        task,
                        eventName,
                        eventProperties,
                        String.format("Task ended by an delegated actor '%s' ", principal.getName())
                                + (!StringUtils.isEmpty(comment) ? " with the following comment: " + comment : ""),
                        null);
            }
        }
    }

    public static EventProducer getEventProducer() {
        return Framework.getLocalService(EventProducer.class);
    }
}
