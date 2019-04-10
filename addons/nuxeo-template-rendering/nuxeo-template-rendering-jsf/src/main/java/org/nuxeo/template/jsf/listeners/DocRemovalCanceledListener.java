/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Gabriel Barata <gbarata@nuxeo.com>
 */
package org.nuxeo.template.jsf.listeners;

import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class DocRemovalCanceledListener implements EventListener {

    @Override
    public void handleEvent(Event event) {

        EventContext ctx = event.getContext();

        if (DocumentEventTypes.DOCUMENT_REMOVAL_CANCELED.equals(event.getName()) &&
            ctx instanceof DocumentEventContext) {
            FacesMessages.instance().clearGlobalMessages();
            FacesMessages.instance().addFromResourceBundleOrDefault(StatusMessage.Severity.WARN,
                "label.template.canNotDeletedATemplateInUse",
                "Can not delete a template that is still in use.");
        }
    }
}
