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

import java.util.Date;
import java.util.Random;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;

public class NotePlugin extends AbstractPlugin {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(NotePlugin.class);

    public DocumentModel create(CoreSession documentManager, Blob content,
            String path, boolean overwrite, String fullname,
            TypeManager typeService) throws ClientException, IOException {

        String filename = FileManagerUtils.fetchFileName(fullname);

        DocumentModel docModel;
        String title = FileManagerUtils.fetchTitle(filename);

        // Looking if an existing Document with the same filename exists.
        DocumentModel existing = FileManagerUtils.getExistingDocByTitle(
                documentManager, path, title);

        if (overwrite && existing != null) {
            docModel = existing;

            // Do a checkin / checkout of the current version first
            DocumentRef docRef = docModel.getRef();
            VersionModel newVersion = new VersionModelImpl();
            newVersion
                    .setLabel(documentManager.generateVersionLabelFor(docRef));
            documentManager.checkIn(docRef, newVersion);
            documentManager.checkOut(docRef);

            docModel.setProperty("note", "note", content.getString());
        } else {
            // Creating an unique identifier
            Random random = new Random(new Date().getTime());
            String docId = String.valueOf(random.nextLong());

            DocumentModelImpl document = new DocumentModelImpl(path, docId,
                    "Note");
            docModel = documentManager.createDocument(document);

            // Updating known attributes (title, note)
            docModel.setProperty("dublincore", "title", title);
            docModel.setProperty("note", "note", content.getString());

            // updating icon
            Type noteType = typeService.getType("Note");
            String iconPath = noteType.getIcon();
            docModel.setProperty("common", "icon", iconPath);
        }
        documentManager.saveDocument(docModel);
        documentManager.save();

        log.debug("Created the Note: " + docModel.getName() + " with icon : "
                + docModel.getProperty("common", "icon"));
        return docModel;
    }

}
