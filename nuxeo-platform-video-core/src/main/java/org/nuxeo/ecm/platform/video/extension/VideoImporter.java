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
 *     Peter Di Lorenzo
 */

package org.nuxeo.ecm.platform.video.extension;

import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.video.convert.Constants;
import org.nuxeo.runtime.api.Framework;

/**
 * This class will create a Document of type "Video" from the uploaded file, if
 * the uploaded file matches any of the mime types listed in the
 * filemanager-plugins.xml file.
 *
 * If an existing document with the same title is found, it will overwrite it
 * and increment the version number if the overwrite flag is set to true;
 * Otherwise, it will generate a new title and create a new Document of type
 * Video with that title.
 *
 */
public class VideoImporter extends AbstractFileImporter {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(VideoImporter.class);

    public static final String VIDEO_TYPE = "Video";

    protected ConversionService cs;

    public DocumentModel create(CoreSession documentManager, Blob content,
            String path, boolean overwrite, String fullname,
            TypeManager typeService) throws ClientException, IOException {

        String filename = FileManagerUtils.fetchFileName(fullname);

        String title = FileManagerUtils.fetchTitle(filename);

        // Check to see if an existing Document with the same title exists.
        DocumentModel docModel = FileManagerUtils.getExistingDocByTitle(
                documentManager, path, title);

        // if overwrite flag is true and the file already exists, overwrite it
        if (overwrite && (docModel != null)) {
            updateProperties(docModel, content, filename);
            docModel = overwriteAndIncrementversion(documentManager, docModel);
        } else {

            // Creating an unique identifier
            String docId = IdUtils.generateId(title);

            docModel = documentManager.createDocumentModel(path, docId,
                    VIDEO_TYPE);
            docModel.setProperty("dublincore", "title", title);
            updateProperties(docModel, content, filename);

            // updating icon
            Type docType = typeService.getType(VIDEO_TYPE);
            if (docType != null) {
                String iconPath = docType.getIcon();
                docModel.setProperty("common", "icon", iconPath);
            }
            docModel = documentManager.createDocument(docModel);
        }
        return docModel;
    }

    protected void updateProperties(DocumentModel docModel, Blob content,
            String filename) throws ClientException {

        try {
            BlobHolder result = getConversionService().convert(
                    Constants.STORYBOARD_CONVERTER,
                    new SimpleBlobHolder(content), null);
            docModel.setPropertyValue("video:storyboard",
                    (Serializable) result.getBlobs());
            docModel.setPropertyValue("video:duration",
                    result.getProperty("duration"));
        } catch (Exception e) {
            log.error(e, e);
        }

        docModel.setPropertyValue("file:content", (Serializable) content);
        docModel.setPropertyValue("file:filename", filename);
    }

    protected ConversionService getConversionService() throws Exception {
        if (cs == null) {
            cs = Framework.getService(ConversionService.class);
        }
        return cs;
    }
}
