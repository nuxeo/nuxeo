/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.core.management.events;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.ReconnectedEventBundleImpl;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.management.counters.CounterHelper;

/**
 * AsyncEventListener that collects events to update the Simon counters
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class EventCounterListener implements PostCommitEventListener {

    // Counters used
    public static final String EVENT_CREATE_COUNTER = "org.nuxeo.event.create";

    public static final String EVENT_UPDATE_COUNTER = "org.nuxeo.event.update";

    public static final String EVENT_REMOVE_COUNTER = "org.nuxeo.event.remove";

    // Event tracked
    protected static final List<String> createEvents = Arrays.asList(new String[] {
            DocumentEventTypes.DOCUMENT_CREATED, DocumentEventTypes.DOCUMENT_CREATED_BY_COPY,
            DocumentEventTypes.DOCUMENT_IMPORTED, DocumentEventTypes.DOCUMENT_PROXY_PUBLISHED,
            DocumentEventTypes.DOCUMENT_PROXY_UPDATED });

    protected static final List<String> updateEvents = Arrays.asList(
            new String[] { DocumentEventTypes.DOCUMENT_CHECKEDIN, DocumentEventTypes.DOCUMENT_CHECKEDOUT,
                    DocumentEventTypes.DOCUMENT_CHILDREN_ORDER_CHANGED, DocumentEventTypes.DOCUMENT_LOCKED,
                    DocumentEventTypes.DOCUMENT_MOVED, DocumentEventTypes.DOCUMENT_PUBLISHED,
                    DocumentEventTypes.DOCUMENT_SECURITY_UPDATED, DocumentEventTypes.DOCUMENT_UNLOCKED,
                    DocumentEventTypes.DOCUMENT_UPDATED, LifeCycleConstants.TRANSITION_EVENT,
                    TrashService.DOCUMENT_TRASHED, TrashService.DOCUMENT_UNTRASHED });

    protected static final List<String> removeEvents = Arrays.asList(new String[] {
            DocumentEventTypes.DOCUMENT_REMOVED, DocumentEventTypes.VERSION_REMOVED });

    @Override
    public void handleEvent(EventBundle events) {
        if (events instanceof ReconnectedEventBundleImpl) {
            ReconnectedEventBundleImpl bundle = (ReconnectedEventBundleImpl) events;
            updateCounters(bundle.getEventNames());
        }
    }

    protected void updateCounters(List<String> eventNames) {

        int created = 0;
        int updated = 0;
        int removed = 0;

        for (String eventName : eventNames) {
            if (createEvents.contains(eventName)) {
                created += 1;
            } else if (updateEvents.contains(eventName)) {
                updated += 1;
            } else if (removeEvents.contains(eventName)) {
                removed += 1;
            }
        }

        if (created > 0) {
            CounterHelper.increaseCounter(EVENT_CREATE_COUNTER, created);
        }
        if (updated > 0) {
            CounterHelper.increaseCounter(EVENT_UPDATE_COUNTER, updated);
        }
        if (removed > 0) {
            CounterHelper.increaseCounter(EVENT_REMOVE_COUNTER, removed);
        }
    }

}
