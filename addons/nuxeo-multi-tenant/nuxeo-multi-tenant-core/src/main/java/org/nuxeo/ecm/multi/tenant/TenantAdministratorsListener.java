/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.multi.tenant;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.usermanager.UserManagerImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.EventService;

/**
 * Listeners invalidating the principals cache when the tenant administrators are changed.
 *
 * @since 5.9.2
 */
public class TenantAdministratorsListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        String eventName = event.getName();
        if (!DocumentEventTypes.BEFORE_DOC_UPDATE.equals(eventName)
                || !(event.getContext() instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc.hasFacet(Constants.TENANT_CONFIG_FACET)) {
            Property property = doc.getProperty(Constants.TENANT_ADMINISTRATORS_PROPERTY);
            if (property.isDirty()) {
                // flush the principals cache
                EventService eventService = Framework.getLocalService(EventService.class);
                eventService.sendEvent(new org.nuxeo.runtime.services.event.Event(UserManagerImpl.USERMANAGER_TOPIC,
                        "invalidateAllPrincipals", null, null));
            }
        }
    }

}
