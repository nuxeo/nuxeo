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

package org.nuxeo.dam.webapp.fileimporter;

import java.io.IOException;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.types.TypeManager;

public class OfficeFileImporter extends AbstractFileImporter {

    private static final long serialVersionUID = 1969828612358246603L;

    public static final String TYPE_NAME = "File";

    public DocumentModel create(CoreSession documentManager, Blob input,
            String path, boolean overwrite, String fullname,
            TypeManager typeService) throws ClientException, IOException {

        String filename = FileManagerUtils.fetchFileName(fullname);
        input.setFilename(filename);

        // Looking if an existing Document with the same filename exists.
        DocumentModel docModel = FileManagerUtils.getExistingDocByFileName(
                documentManager, path, filename);

        String title = FileManagerUtils.fetchTitle(filename);

        // Creating an unique identifier
        String docId = IdUtils.generateId(title);

        docModel = documentManager.createDocumentModel(path, docId, TYPE_NAME);

        // Updating known attributes (title, filename, content)
        docModel.setProperty("dublincore", "title", title);
        docModel.setProperty("file", "filename", filename);
        docModel.setProperty("file", "content", input);

        // writing the new document to the repository
        docModel = documentManager.createDocument(docModel);

        documentManager.save();

        return docModel;
    }

}
