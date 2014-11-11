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
 *     Anahide Tchertchian
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.filemanager.api.FileManagerPermissionException;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;

/**
 * @author Anahide Tchertchian
 */
public class DefaultFileImporter extends AbstractFileImporter {

    public static final String TYPE_NAME = "File";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DefaultFileImporter.class);

    public String getTypeName() {
        return TYPE_NAME;
    }

    /**
     * Creates a file in nearest container.
     * <p>
     * If a document with same title already exists, creates a new version.
     */
    public DocumentModel create(CoreSession documentManager, Blob input,
            String path, boolean overwrite, String fullname,
            TypeManager typeService) throws ClientException, IOException {
        path = getNearestContainerPath(documentManager, path);

        // perform the security checks
        PathRef containerRef = new PathRef(path);
        if (!documentManager.hasPermission(containerRef,
                SecurityConstants.READ_PROPERTIES)
                || !documentManager.hasPermission(containerRef,
                        SecurityConstants.ADD_CHILDREN)) {
            throw new FileManagerPermissionException();
        }
        DocumentModel container = documentManager.getDocument(containerRef);

        String typeName = getTypeName();
        Type containerType = typeService.getType(container.getType());
        List<String> subTypes = new ArrayList<String>(
                containerType.getAllowedSubTypes().keySet());
        if (!subTypes.contains(typeName)) {
            throw new ClientException(
                    String.format(
                            "Cannot create document of type %s in container with type %s",
                            typeName, containerType.getId()));
        }

        String filename = FileManagerUtils.fetchFileName(fullname);
        input.setFilename(filename);

        // Looking if an existing Document with the same filename exists.
        DocumentModel docModel = FileManagerUtils.getExistingDocByFileName(
                documentManager, path, filename);

        // Determining if we need to create or update an existing one
        if (overwrite && docModel != null) {

            // save changes the user might have made to the current version
            documentManager.saveDocument(docModel);
            documentManager.save();

            docModel.setProperty("file", "content", input);
            docModel = overwriteAndIncrementversion(documentManager, docModel);

        } else {
            // new
            String title = FileManagerUtils.fetchTitle(filename);

            // Creating an unique identifier
            String docId = IdUtils.generateId(title);

            docModel = documentManager.createDocumentModel(path, docId,
                    typeName);

            // Updating known attributes (title, filename, content)
            docModel.setProperty("dublincore", "title", title);
            docModel.setProperty("file", "filename", filename);
            docModel.setProperty("file", "content", input);

            // writing the new document to the repository
            docModel = documentManager.createDocument(docModel);
        }

        documentManager.save();

        log.debug("imported the document: " + docModel.getName()
                + " with icon: " + docModel.getProperty("common", "icon")
                + " and type: " + typeName);
        return docModel;
    }

}
