/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
package org.nuxeo.ecm.platform.routing.core.persistence;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * Helper to create tree structure based on date
 * <p>
 * Emails and Mail envelopes are created within trees of folder.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TreeHelper {

    public static final String TITLE_PROPERTY_NAME = "dc:title";

    public static final String DELETED_STATE = "deleted";

    /**
     * Find or create a set of folders representing the date hierarchy
     *
     * @return the last child created (day)
     */
    public static DocumentModel getOrCreateDateTreeFolder(CoreSession session, DocumentModel root, Date date,
            String folderType) {
        String subPath = new SimpleDateFormat("yyyy/MM/dd").format(date);
        return getOrCreatePath(session, root, subPath, folderType);
    }

    public static DocumentModel getOrCreatePath(CoreSession session, DocumentModel root, String subPath,
            String folderType) {
        String[] pathSplit = subPath.split("/");
        String parentPath = root.getPathAsString();
        DocumentModel child = root;
        for (String id : pathSplit) {
            child = getOrCreate(session, parentPath, id, folderType);
            parentPath = child.getPathAsString();
        }
        return child;
    }

    public static synchronized DocumentModel getOrCreate(CoreSession session, String rootPath, String id,
            String folderType) {
        String path = String.format("%s/%s", rootPath, id);
        DocumentRef pathRef = new PathRef(path);
        boolean exists = session.exists(pathRef);
        if (exists) {
            DocumentModel existing = session.getDocument(pathRef);
            if (!DELETED_STATE.equals(existing.getCurrentLifeCycleState())) {
                return existing;
            }
        }
        // create it
        DocumentModel newDocument = session.createDocumentModel(rootPath, IdUtils.generateId(id, "-", true, 24),
                folderType);
        newDocument.setPropertyValue(TITLE_PROPERTY_NAME, id);
        newDocument = session.createDocument(newDocument);
        return newDocument;
    }

}
