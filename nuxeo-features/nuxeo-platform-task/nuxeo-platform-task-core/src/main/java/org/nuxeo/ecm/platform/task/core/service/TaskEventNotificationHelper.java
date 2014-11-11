/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.platform.task.core.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
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

    public static void notifyEvent(CoreSession coreSession,
            DocumentModel document, NuxeoPrincipal principal, Task task,
            String eventId, Map<String, Serializable> properties,
            String comment, String category) throws ClientException {
        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }
        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }

        EventContext eventContext = null;
        if (document != null) {
            properties.put(CoreEventConstants.REPOSITORY_NAME,
                    document.getRepositoryName());
            properties.put(CoreEventConstants.SESSION_ID,
                    coreSession.getSessionId());
            properties.put(CoreEventConstants.DOC_LIFE_CYCLE,
                    document.getCurrentLifeCycleState());
            eventContext = new DocumentEventContext(coreSession, principal,
                    document);
        } else {
            eventContext = new EventContextImpl(coreSession, principal);
        }
        properties.put(DocumentEventContext.COMMENT_PROPERTY_KEY, comment);
        properties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY, category);
        properties.put(TaskService.TASK_INSTANCE_EVENT_PROPERTIES_KEY, task);
        String disableNotif = task.getVariable(TaskEventNames.DISABLE_NOTIFICATION_SERVICE);
        if (disableNotif != null
                && Boolean.TRUE.equals(Boolean.valueOf(disableNotif))) {
            properties.put(TaskEventNames.DISABLE_NOTIFICATION_SERVICE,
                    Boolean.TRUE);
        }
        eventContext.setProperties(properties);

        Event event = eventContext.newEvent(eventId);
        getEventProducer().fireEvent(event);
    }

    public static EventProducer getEventProducer() {
        return Framework.getLocalService(EventProducer.class);
    }
}
