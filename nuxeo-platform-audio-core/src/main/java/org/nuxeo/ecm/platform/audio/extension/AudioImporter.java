/*
 * (C) Copyright 2009-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Peter Di Lorenzo
 */
package org.nuxeo.ecm.platform.audio.extension;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * This class will create a Document of type "Audio" from the uploaded file, if the uploaded file matches any of the
 * mime types listed in the filemanager-plugins.xml file.
 * <p>
 * If an existing document with the same title is found, it will overwrite it and increment the version number if the
 * overwrite flag is set to true; Otherwise, it will generate a new title and create a new Document of type Audio with
 * that title.
 */
public class AudioImporter extends AbstractFileImporter {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(AudioImporter.class);

    public static final String AUDIO_TYPE = "Audio";

    @Override
    public DocumentModel create(CoreSession documentManager, Blob content, String path, boolean overwrite,
            String fullname, TypeManager typeService) throws IOException {

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
            checkIn(docModel);
            docModel = documentManager.saveDocument(docModel);

        } else {
            PathSegmentService pss = Framework.getService(PathSegmentService.class);

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
