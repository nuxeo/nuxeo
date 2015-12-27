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
 *     Thomas Roger
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.picture.extension;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants;

public class ImagePlugin extends AbstractFileImporter {

    private static final long serialVersionUID = 1L;

    @Override
    public String getDefaultDocType() {
        return ImagingDocumentConstants.PICTURE_TYPE_NAME;
    }

    @Override
    public boolean isOverwriteByTitle() {
        return false; // by filename
    }

    @Override
    public void updateDocument(DocumentModel doc, Blob content) {
        doc.setPropertyValue("file:content", (Serializable) content);
    }

}
