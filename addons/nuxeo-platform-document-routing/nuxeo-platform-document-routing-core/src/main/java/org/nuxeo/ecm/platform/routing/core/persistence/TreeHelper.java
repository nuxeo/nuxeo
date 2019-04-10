/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: MailTreeHelper.java 57899 2008-10-07 12:02:44Z atchertchian $
 */

package org.nuxeo.ecm.platform.routing.core.persistence;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
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
    public static DocumentModel getOrCreateDateTreeFolder(
            CoreSession session, DocumentModel root, Date date,
            String folderType) throws ClientException {
        String subPath = new SimpleDateFormat("yyyy/MM/dd").format(date);
        return getOrCreatePath(session, root, subPath, folderType);
    }

    public static DocumentModel getOrCreatePath(CoreSession session,
            DocumentModel root, String subPath, String folderType)
            throws ClientException {
        String[] pathSplit = subPath.split("/");
        String parentPath = root.getPathAsString();
        DocumentModel child = root;
        for (String id : pathSplit) {
            child = getOrCreate(session, parentPath, id, folderType);
            parentPath = child.getPathAsString();
        }
        return child;
    }

    public static synchronized DocumentModel getOrCreate(
            CoreSession session, String rootPath, String id, String folderType)
            throws ClientException {
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
        DocumentModel newDocument = session.createDocumentModel(rootPath,
                IdUtils.generateId(id), folderType);
        newDocument.setPropertyValue(TITLE_PROPERTY_NAME, id);
        newDocument = session.createDocument(newDocument);
        return newDocument;
    }

}
