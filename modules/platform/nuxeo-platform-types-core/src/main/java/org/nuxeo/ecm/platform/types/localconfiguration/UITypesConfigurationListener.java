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

package org.nuxeo.ecm.platform.types.localconfiguration;

import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_DEFAULT_TYPE;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Listener validating that the selected default type is part of the allowed type on the document.
 * <p>
 * If not, the default type is reset.
 *
 * @since 5.7.3
 */
public class UITypesConfigurationListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        if (!DocumentEventTypes.BEFORE_DOC_UPDATE.equals(event.getName())) {
            return;
        }

        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            DocumentModel doc = ((DocumentEventContext) eventContext).getSourceDocument();
            UITypesConfiguration uiTypesConfiguration = doc.getAdapter(UITypesConfiguration.class, true);
            if (uiTypesConfiguration != null) {
                List<String> allowedTypes = new ArrayList<>(uiTypesConfiguration.getAllowedTypes());
                if (!allowedTypes.isEmpty()) {
                    String defaultType = uiTypesConfiguration.getDefaultType();
                    if (!allowedTypes.contains(defaultType)) {
                        // the selected default type is not allowed, reset it
                        doc.setPropertyValue(UI_TYPES_CONFIGURATION_DEFAULT_TYPE, "");
                    }
                }
            }
        }
    }

}
