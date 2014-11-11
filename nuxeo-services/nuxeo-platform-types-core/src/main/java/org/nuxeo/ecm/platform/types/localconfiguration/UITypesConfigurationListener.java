/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.types.localconfiguration;

import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_DEFAULT_TYPE;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Listener validating that the selected default type is part of the allowed
 * type on the document.
 * <p>
 * If not, the default type is reset.
 * 
 * @since 5.7.3
 */
public class UITypesConfigurationListener implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {
        if (!DocumentEventTypes.BEFORE_DOC_UPDATE.equals(event.getName())) {
            return;
        }

        EventContext eventContext = event.getContext();
        if (eventContext instanceof DocumentEventContext) {
            DocumentModel doc = ((DocumentEventContext) eventContext).getSourceDocument();
            UITypesConfiguration uiTypesConfiguration = doc.getAdapter(
                    UITypesConfiguration.class, true);
            if (uiTypesConfiguration != null) {
                List<String> allowedTypes = new ArrayList<>(
                        uiTypesConfiguration.getAllowedTypes());
                if (!allowedTypes.isEmpty()) {
                    String defaultType = uiTypesConfiguration.getDefaultType();
                    if (!allowedTypes.contains(defaultType)) {
                        // the selected default type is not allowed, reset it
                        doc.setPropertyValue(
                                UI_TYPES_CONFIGURATION_DEFAULT_TYPE, "");
                    }
                }
            }
        }
    }

}
