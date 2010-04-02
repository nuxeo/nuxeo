/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.picture.extension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.types.TypeManager;

public class ImagePlugin extends AbstractFileImporter {

    private static final long serialVersionUID = 5850210255138418118L;

    private static final Log log = LogFactory.getLog(ImagePlugin.class);

    public DocumentModel create(CoreSession documentManager, Blob content,
            String path, boolean overwrite, String fullname,
            TypeManager typeService) throws ClientException, IOException {
        path = getNearestContainerPath(documentManager, path);
        doSecurityCheck(documentManager, path,
                ImagingDocumentConstants.PICTURE_TYPE_NAME, typeService);

        String filename = FileManagerUtils.fetchFileName(fullname);
        content.setFilename(filename);

        // Looking if an existing Document with the same filename exists.
        DocumentModel docModel = FileManagerUtils.getExistingDocByFileName(
                documentManager, path, filename);

        if (overwrite && docModel != null) {

            // Do a checkin / checkout of the current version first
            DocumentRef docRef = docModel.getRef();
            VersionModel newVersion = new VersionModelImpl();
            newVersion.setLabel(documentManager.generateVersionLabelFor(docRef));
            documentManager.checkIn(docRef, newVersion);
            documentManager.checkOut(docRef);

            ((Map) ((List) docModel.getDataModel("picture").getData("views")).get(0)).put(
                    "content", content.persist());
            // To do: Generate Picture views
            // docModel.setProperty("file", "content", content.persist());

        } else {
            String title = FileManagerUtils.fetchTitle(filename);
            // Creating an unique identifier
            String docId = IdUtils.generateId(title);
            docModel = documentManager.createDocumentModel(path,
                    docId, ImagingDocumentConstants.PICTURE_TYPE_NAME);
            try {
                DocumentModel parent = documentManager.getDocument(new PathRef(path));
                ArrayList<Map<String, Object>> pictureTemplates = null;
                if (parent.getType().equals(
                        ImagingDocumentConstants.PICTUREBOOK_TYPE_NAME)) {
                    // Use PictureBook Properties
                    pictureTemplates = (ArrayList<Map<String, Object>>) parent.getPropertyValue(ImagingDocumentConstants.PICTURETEMPLATES_PROPERTY_NAME);
                }
                PictureResourceAdapter picture = docModel.getAdapter(PictureResourceAdapter.class);
                picture.createPicture(content, filename, title,
                        pictureTemplates);

            } catch (Exception e) {
                log.error("Picture.views generation failed", e);
            }
            docModel = documentManager.createDocument(docModel);
        }
        documentManager.save();

        log.debug("Created the Picture: " + docModel.getName()
                + " with icon : " + docModel.getProperty("common", "icon"));
        return docModel;
    }

}
