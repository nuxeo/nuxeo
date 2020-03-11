/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.rendition.operation;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;

/**
 * Returns a document rendition given its name.
 *
 * @since 7.3
 */
@Operation(id = GetRendition.ID, category = Constants.CAT_BLOB, label = "Gets a document rendition", description = "Gets a document rendition given its name. Returns the rendition blob.")
public class GetRendition {

    public static final String ID = "Document.GetRendition";

    @Context
    protected RenditionService renditionService;

    @Param(name = "renditionName")
    protected String renditionName;

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(DocumentModel doc) {
        Rendition rendition = renditionService.getRendition(doc, renditionName);
        Blob blob = rendition.getBlob();

        // cannot return null since it may break the next operation
        if (blob == null) { // create an empty blob
            blob = Blobs.createBlob("");
            blob.setFilename(doc.getName() + ".null");
        }
        return blob;
    }
}
