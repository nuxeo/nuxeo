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

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import static org.nuxeo.dam.Constants.PICTURE_SCHEMA;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;

/**
 * Listener used to update the filename field of the blobs that are contained by
 * the documents that has 'picture' schema. This update should be made on all
 * the fields that contain the blob which filename was updated.
 *
 * @author btatar
 */
public class ImageFilenameUpdater implements EventListener {

    public static final String FILENAME = "filename";

    public static final String TITLE = "title";

    private static final Log log = LogFactory.getLog(ImageFilenameUpdater.class);

    public void handleEvent(Event event) throws ClientException {

        DocumentEventContext docCtx = null;
        if (event.getContext() instanceof DocumentEventContext) {
            docCtx = (DocumentEventContext) event.getContext();
        } else {
            return;
        }
        String eventId = event.getName();
        if (!eventId.equals(BEFORE_DOC_UPDATE)) {
            return;
        }
        DocumentModel doc = docCtx.getSourceDocument();
        if (!doc.hasSchema(PICTURE_SCHEMA)) {
            return;
        }

        Map<String, Object> pictureMap = doc.getDataModel(PICTURE_SCHEMA).getMap();

        List<Map<String, Object>> viewsList = (List<Map<String, Object>>) pictureMap.get("views");
        if (viewsList == null || viewsList.isEmpty()) {
            return;
        }
        String filenameUpdated = (String) viewsList.get(0).get(FILENAME);
        boolean differenceFound = false;
        for (int i = 1; i < viewsList.size(); i++) {
            if (!filenameUpdated.equals(viewsList.get(i).get(FILENAME))) {
                differenceFound = true;
                break;
            }
        }
        if (!differenceFound) {
            return;
        }
        String title = (String) viewsList.get(0).get(TITLE);
        for (Map<String, Object> view : viewsList) {
            view.put(FILENAME, filenameUpdated);
            Blob fileBlob = (Blob) view.get("content");
            fileBlob.setFilename(title + "_" + filenameUpdated);
        }

        doc.getDataModel(PICTURE_SCHEMA).setMap(pictureMap);
    }
}
