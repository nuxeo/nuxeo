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

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.threed.ThreeDConstants;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;
import java.io.IOException;

public class ThreeDImporter extends AbstractFileImporter {

    public DocumentModel create(CoreSession session, Blob content, String path, boolean overwrite,
                                String fullname, TypeManager typeService) throws IOException {
        DocumentModel container = session.getDocument(new PathRef(path));
        String docType = getDocType(container);
        if (docType == null) {
            docType = getDefaultDocType();
        }
        String title = FileManagerUtils.fetchTitle(content.getFilename());
        DocumentModel doc = session.createDocumentModel(docType);
        doc.setPropertyValue("dc:title", title);
        PathSegmentService pss = Framework.getLocalService(PathSegmentService.class);
        doc.setPathInfo(path, pss.generatePathSegment(doc));
        updateDocument(doc, content);
        return doc;
    }

    @Override
    public String getDefaultDocType() {
        return ThreeDConstants.DOCTYPE;
    }

    @Override
    public boolean isOverwriteByTitle() {
        return true;
    }

}
