/*
 * (C) Copyright 2009-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.video.importer;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.video.VideoConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * This class will create a Document of type "Video" from the uploaded file, if the uploaded file matches any of the
 * mime types listed in the filemanager-plugins.xml file.
 * <p>
 * If an existing document with the same title is found, it will overwrite it and increment the version number if the
 * overwrite flag is set to true; Otherwise, it will generate a new title and create a new Document of type Video with
 * that title.
 */
public class VideoImporter extends AbstractFileImporter {

    private static final long serialVersionUID = 1L;

    @Override
    public String getDefaultDocType() {
        return VideoConstants.VIDEO_TYPE;
    }

    @Override
    public boolean isOverwriteByTitle() {
        return true;
    }

    @Override
    public void updateDocument(DocumentModel doc, Blob content) {
        super.updateDocument(doc, content);
        // update the icon
        Type type = Framework.getService(TypeManager.class).getType(doc.getType());
        if (type != null) {
            doc.setProperty("common", "icon", type.getIcon());
        }
    }

}
