/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.multi.tenant;

import static org.nuxeo.ecm.core.api.LifeCycleConstants.DELETE_TRANSITION;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSITION_EVENT;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.UNDELETE_TRANSITION;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED_BY_COPY;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_REMOVED;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class MultiTenantListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        MultiTenantService multiTenantService = Framework.getService(MultiTenantService.class);
        String tenantDocumentType = multiTenantService.getTenantDocumentType();
        DocumentModel doc = ((DocumentEventContext) ctx).getSourceDocument();
        if (!doc.getType().equals(tenantDocumentType)) {
            return;
        }

        @SuppressWarnings("resource")
        CoreSession session = ctx.getCoreSession();
        if (!multiTenantService.isTenantIsolationEnabled(session)) {
            return;
        }

        String eventName = event.getName();
        if (DOCUMENT_CREATED.equals(eventName) || DOCUMENT_CREATED_BY_COPY.equals(eventName)) {
            multiTenantService.enableTenantIsolationFor(session, doc);
            session.save();
        } else if (DOCUMENT_REMOVED.equals(eventName)) {
            multiTenantService.disableTenantIsolationFor(session, doc);
            session.save();
        } else if (TRANSITION_EVENT.equals(eventName)) {
            String transition = (String) ctx.getProperty(TRANSTION_EVENT_OPTION_TRANSITION);
            if (DELETE_TRANSITION.equals(transition)) {
                multiTenantService.disableTenantIsolationFor(session, doc);
                session.save();
            } else if (UNDELETE_TRANSITION.equals(transition)) {
                multiTenantService.enableTenantIsolationFor(session, doc);
                session.save();
            }
        }
    }

}
