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

package org.nuxeo.ecm.platform.audio.extension;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * This class will create a Document of type "Audio" from the uploaded file, if the uploaded file
 * matches any of the mime types listed in the filemanager-plugins.xml file.
 * <p>
 * If an existing document with the same title is found, it will overwrite it and increment the
 * version number if the overwrite flag is set to true;
 * Otherwise, it will generate a new title and create a new Document of type Audio with that title.
 *
 */
public class AudioImporter extends AbstractFileImporter {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(AudioImporter.class);

    public static final String AUDIO_TYPE = "Audio";

    public DocumentModel create(CoreSession documentManager, Blob content,
            String path, boolean overwrite, String fullname,
            TypeManager typeService) throws ClientException, IOException {

        String filename = FileManagerUtils.fetchFileName(fullname);

        String title = FileManagerUtils.fetchTitle(filename);

        // Check to see if an existing Document with the same title exists.
        DocumentModel docModel = FileManagerUtils.getExistingDocByTitle(documentManager, path, title);

        // if overwrite flag is true and the file already exists, overwrite it
        if (overwrite && (docModel != null)) {

            // update known attributes, format is: schema, attribute, value
            docModel.setProperty("file", "content", content);
            docModel.setProperty("dublincore", "title", title);

            // now save the uploaded file as another new version
            docModel = overwriteAndIncrementversion(documentManager, docModel);

        } else {
            PathSegmentService pss;
            try {
                pss = Framework.getService(PathSegmentService.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }

            String docType = getDocType();
            if (docType == null) {
                docType = AUDIO_TYPE;
            }

            docModel = documentManager.createDocumentModel(docType);
            // update known attributes, format is: schema, attribute, value
            docModel.setProperty("dublincore", "title", title);
            docModel.setProperty("file", "content", content);
            docModel.setProperty("file", "filename", filename);

            // updating icon
            Type type = typeService.getType(docType);
            if (type != null) {
                String iconPath = type.getIcon();
                docModel.setProperty("common", "icon", iconPath);
            }
            docModel.setPathInfo(path, pss.generatePathSegment(docModel));
            docModel = documentManager.createDocument(docModel);
        }
        return docModel;
    }

}
