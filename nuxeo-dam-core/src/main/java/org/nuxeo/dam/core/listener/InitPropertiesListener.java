/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.dam.core.listener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.dam.api.Constants;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class InitPropertiesListener implements EventListener {

    protected static final List<String> DUBLINCORE_PROPERTIES = Arrays.asList(
            "dc:description", "dc:coverage", "dc:subjects", "dc:expired");

    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();

        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel doc = docCtx.getSourceDocument();
            CoreSession coreSession = docCtx.getCoreSession();

            if (doc.hasSchema(Constants.DAM_COMMON_SCHEMA)
                    && !Constants.IMPORT_SET_TYPE.equals(doc.getType())) {

                DocumentModel parent = coreSession.getDocument(doc.getParentRef());
                DocumentModel importSet = docCtx.getCoreSession().getSuperSpace(
                        parent);

                Map<String, Object> damMap = importSet.getDataModel(
                        Constants.DAM_COMMON_SCHEMA).getMap();
                doc.getDataModel((Constants.DAM_COMMON_SCHEMA)).setMap(damMap);

                Map<String, Object> dublincoreMap = importSet.getDataModel(
                        Constants.DUBLINCORE_SCHEMA).getMap();

                Map<String, Object> importSetMap = new HashMap<String, Object>();
                for (Map.Entry<String, Object> entry : dublincoreMap.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (DUBLINCORE_PROPERTIES.contains(key)) {
                        importSetMap.put(key, value);
                    }
                }
                doc.getDataModel((Constants.DUBLINCORE_SCHEMA)).setMap(
                        importSetMap);
            }
        }
    }

}
