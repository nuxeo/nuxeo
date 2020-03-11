/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.restapi.listeners;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Dummy listener which throws a {@link DocumentValidationException} when a ValidatedDocument is created with firstname
 * equals to lastname.
 *
 * @since 11.1
 */
public class DummyGlobalValidationListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        if (event.getContext() instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
            DocumentModel targetDoc = docCtx.getSourceDocument();
            if (DocumentEventTypes.ABOUT_TO_CREATE.equals(event.getName())
                    || DocumentEventTypes.BEFORE_DOC_UPDATE.equals(event.getName())) {
                if ("ValidatedDocument".equals(targetDoc.getType())) {
                    ListProperty users = (ListProperty) targetDoc.getProperty("vs:users");
                    for (Property user : users.getChildren()) {
                        String firstname = (String) user.get("firstname").getValue();
                        String lastname = (String) user.get("lastname").getValue();
                        if (StringUtils.isNotBlank(firstname) && firstname.equals(lastname)) {
                            event.markBubbleException();
                            throw new DocumentValidationException("lastname.cannot.be.equals.to.firstname");
                        }
                    }
                }
            }
        }
    }

}
