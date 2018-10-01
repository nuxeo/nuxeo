/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Miguel Nixo
 */
package org.nuxeo.ecm.platform.threed.importer;

import static org.nuxeo.ecm.platform.threed.ThreeDConstants.SUPPORTED_EXTENSIONS;
import static org.nuxeo.ecm.platform.threed.ThreeDConstants.THREED_TYPE;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

public class ThreeDImporter extends AbstractFileImporter {
    public static final String MIMETYPE_ZIP = "application/zip";

    public DocumentModel create(CoreSession session, Blob content, String path, boolean overwrite, String fullname,
            TypeManager typeService) throws IOException {

        boolean isThreeD = SUPPORTED_EXTENSIONS.contains(FileUtils.getFileExtension(content.getFilename()));
        boolean isZipThreeD = MIMETYPE_ZIP.equals(content.getMimeType());
        String title = null;
        if (isZipThreeD) {
            title = getModelFilename(content);
            isZipThreeD = title != null;
        }

        if (!(isThreeD || isZipThreeD)) {
            return null;
        }
        DocumentModel container = session.getDocument(new PathRef(path));
        String docType = getDocType(container);
        if (docType == null) {
            docType = getDefaultDocType();
        }
        if (isThreeD) {
            title = FileManagerUtils.fetchTitle(content.getFilename());
        }
        DocumentModel doc = session.createDocumentModel(docType);
        doc.setPropertyValue("dc:title", title);
        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        doc.setPathInfo(path, pss.generatePathSegment(doc));
        updateDocument(doc, content);
        doc = session.createDocument(doc);
        session.save();
        return doc;
    }

    protected String getModelFilename(final Blob zipContent) throws IOException {
        /* Extract ZIP contents */
        ZipEntry zipEntry;
        String threeDFilename = null;
        try (ZipInputStream zipInputStream = new ZipInputStream(zipContent.getStream())) {
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                // skip if the entry is a directory, if it's not a supported extension or if it's hidden (by convention)
                boolean isSupported = SUPPORTED_EXTENSIONS.contains(FileUtils.getFileExtension(zipEntry.getName()));
                if (zipEntry.isDirectory() || !isSupported || zipEntry.getName().startsWith(".")) {
                    continue;
                }
                threeDFilename = FileManagerUtils.fetchTitle(zipEntry.getName());
                break;
            }
        }
        return threeDFilename;
    }

    @Override
    public String getDefaultDocType() {
        return THREED_TYPE;
    }

    @Override
    public boolean isOverwriteByTitle() {
        return true;
    }

}
