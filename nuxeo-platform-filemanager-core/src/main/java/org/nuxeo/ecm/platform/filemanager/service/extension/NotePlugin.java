/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.types.TypeManager;

/**
 * Import the string content of a blob as text for the content of the "note"
 * field of a new Note document. If an existing document with the same title is
 * found the existing Note document is updated instead.
 * 
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 */
public class NotePlugin extends AbstractPlugin {

    private static final long serialVersionUID = 1073550562485540108L;

    private static final String ICON_FIELD = "icon";

    private static final String COMMON_SCHEMA = "common";

    private static final String NOTE_FIELD = "note";

    private static final String NOTE_SCHEMA = NOTE_FIELD;

    private static final String TITLE_FIELD = "title";

    private static final String DUBLINCORE_SCHEMA = "dublincore";

    private static final String NOTE_TYPE = "Note";

    private static final Log log = LogFactory.getLog(NotePlugin.class);

    public DocumentModel create(CoreSession documentManager, Blob content,
            String path, boolean overwrite, String fullname,
            TypeManager typeService) throws ClientException, IOException {

        String filename = FileManagerUtils.fetchFileName(fullname);

        String title = FileManagerUtils.fetchTitle(filename);

        // Looking if an existing Document with the same filename exists.
        DocumentModel docModel = FileManagerUtils.getExistingDocByTitle(
                documentManager, path, title);

        if (overwrite && docModel != null) {

            // Do a checkin / checkout of the current version first
            DocumentRef docRef = docModel.getRef();
            VersionModel newVersion = new VersionModelImpl();
            newVersion.setLabel(documentManager.generateVersionLabelFor(docRef));
            documentManager.checkIn(docRef, newVersion);
            documentManager.checkOut(docRef);

            // Update the content of the note with the content of the imported
            // blob
            docModel.setProperty(NOTE_SCHEMA, NOTE_FIELD, content.getString());

            // Save changes back to the repository
            docModel = documentManager.saveDocument(docModel);
        } else {
            // Create a new empty DocumentModel of type Note in memory
            String docId = IdUtils.generateId(title);
            docModel = documentManager.createDocumentModel(path, docId,
                    NOTE_TYPE);

            // Update known attributes (title, note)
            docModel.setProperty(DUBLINCORE_SCHEMA, TITLE_FIELD, title);
            docModel.setProperty(NOTE_SCHEMA, NOTE_FIELD, content.getString());

            // Create the new document in the repository
            docModel = documentManager.createDocument(docModel);
        }
        documentManager.save();

        log.debug("Created the Note: " + docModel.getName() + " with icon: "
                + docModel.getProperty(COMMON_SCHEMA, ICON_FIELD));
        return docModel;
    }

}
