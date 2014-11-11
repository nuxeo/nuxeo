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

package org.nuxeo.ecm.platform.picture.web.extension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractPlugin;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;

public class ImagePlugin extends AbstractPlugin {

    private static final long serialVersionUID = 5850210255138418118L;
    private static final Log log = LogFactory.getLog(ImagePlugin.class);

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
            newVersion.setLabel(documentManager.generateVersionLabelFor(docRef));
            documentManager.checkIn(docRef, newVersion);
            documentManager.checkOut(docRef);

            ((Map) ((List) docModel.getDataModel("picture").getData(
                    "views")).get(0)).put("content", content.persist());
            // To do: Generate Picture views
            // docModel.setProperty("file", "content", content.persist());

        } else {

            // Creating an unique identifier
            String docId = IdUtils.generateId(title);

            DocumentModelImpl document = new DocumentModelImpl(path, docId,
                    "Picture");
            docModel = documentManager.createDocument(document);
            try {
                DocumentModel parent = documentManager.getDocument(docModel.getParentRef());
                ArrayList<Map<String, Object>> pictureTemplates = null;
                if (parent.getType().equals("PictureBook")) {
                    // Use PictureBook Properties
                    pictureTemplates = (ArrayList<Map<String, Object>>) parent.getProperty(
                            "picturebook", "picturetemplates");
                }
                PictureResourceAdapter picture = docModel.getAdapter(PictureResourceAdapter.class);
                picture.createPicture(content, filename, title, pictureTemplates);

            } catch (Exception e) {
                log.error("Picture.views generation failed", e);
            }
            // updating icon

            Type imageType = typeService.getType("Picture");
            String iconPath = imageType.getIcon();
            docModel.setProperty("common", "icon", iconPath);
        }
        documentManager.saveDocument(docModel);
        documentManager.save();

        log.debug("Created the Picture: " + docModel.getName()
                + " with icon : " + docModel.getProperty("common", "icon"));
        return docModel;
    }

}
