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
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

public class ImagePlugin extends AbstractFileImporter {

    private static final long serialVersionUID = 5850210255138418118L;

    private static final Log log = LogFactory.getLog(ImagePlugin.class);

    @Override
    public DocumentModel create(CoreSession documentManager, Blob content,
            String path, boolean overwrite, String fullname,
            TypeManager typeService) throws ClientException, IOException {
        path = getNearestContainerPath(documentManager, path);

        String docType = getDocType();
        if (docType == null) {
            docType = ImagingDocumentConstants.PICTURE_TYPE_NAME;
        }
        doSecurityCheck(documentManager, path, docType, typeService);

        String filename = FileManagerUtils.fetchFileName(fullname);
        content.setFilename(filename);

        // Looking if an existing Document with the same filename exists.
        DocumentModel docModel = FileManagerUtils.getExistingDocByFileName(
                documentManager, path, filename);

        if (overwrite && docModel != null) {

            // Do a snapshot of the current version first
            DocumentRef docRef = docModel.getRef();
            if (documentManager.isCheckedOut(docRef)) {
                documentManager.checkIn(docRef, null, null);
            }

            BlobHolder bh = docModel.getAdapter(BlobHolder.class);
            bh.setBlob(content.persist());
        } else {
            PathSegmentService pss;
            try {
                pss = Framework.getService(PathSegmentService.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }

            docModel = documentManager.createDocumentModel(docType);
            String title = FileManagerUtils.fetchTitle(filename);
            docModel.setPropertyValue("dc:title", title);
            docModel.setPropertyValue("file:content",
                    (Serializable) content.persist());
            docModel.setPathInfo(path, pss.generatePathSegment(docModel));
            docModel = documentManager.createDocument(docModel);
        }
        documentManager.save();

        log.debug("Created the Picture: " + docModel.getName()
                + " with icon : " + docModel.getProperty("common", "icon"));
        return docModel;
    }

}
