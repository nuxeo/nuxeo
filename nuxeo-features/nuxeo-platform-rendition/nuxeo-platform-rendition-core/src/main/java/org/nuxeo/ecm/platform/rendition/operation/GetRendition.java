/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
