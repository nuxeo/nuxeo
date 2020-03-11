/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.blob;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.utils.BlobUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Bulk Download Operation.
 *
 * @since 9.3
 */
@Operation(id = BulkDownload.ID, category = Constants.CAT_BLOB, label = "Bulk Download", description = "Prepare a Zip of a list of documents.")
public class BulkDownload {

    public static final String ID = "Blob.BulkDownload";

    private static final Logger log = LogManager.getLogger(BulkDownload.class);

    @Context
    protected CoreSession session;

    @Param(name = "filename", required = false)
    protected String fileName;

    @OperationMethod
    public Blob run(DocumentModelList docs) throws IOException {

        DownloadService downloadService = Framework.getService(DownloadService.class);
        List<Blob> blobs = docs.stream().map(doc -> {
            Blob blob = downloadService.resolveBlob(doc);
            if (blob == null) {
                log.trace("Not able to resolve blob");
                return null;
            }
            if (!downloadService.checkPermission(doc, null, blob, "download", Collections.emptyMap())) {
                log.debug("Not allowed to bulk download blob for document {}", doc::getPathAsString);
                return null;
            }
            return blob;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        if (blobs.isEmpty()) {
            log.debug("No blob to be zipped");
            return null;
        }

        String filename = StringUtils.isNotBlank(this.fileName) ? this.fileName
                : String.format("BlobListZip-%s-%s", UUID.randomUUID(), session.getPrincipal().getName());
        return BlobUtils.zip(blobs, filename);
    }

}
