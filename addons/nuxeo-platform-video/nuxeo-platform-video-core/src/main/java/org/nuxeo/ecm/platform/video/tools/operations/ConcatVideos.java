/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thibaud Arguillere
 *     Ricardo Dias
 */
package org.nuxeo.ecm.platform.video.tools.operations;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.video.tools.VideoToolsService;
import org.nuxeo.runtime.api.Framework;

/**
 * Operation for merging 2-n videos in one.
 *
 * @since 8.4
 */
@Operation(id = ConcatVideos.ID, category = Constants.CAT_BLOB, label = "Joins two or more videos sequentially.", description = "Merge 2-n videos in one.")
public class ConcatVideos {

    public static final String ID = "Video.Concat";

    @Param(name = "xpath", required = false)
    protected String xpath;

    @OperationMethod
    public Blob run(BlobList blobs) throws NuxeoException, IOException, CommandNotAvailable {
        VideoToolsService service = Framework.getService(VideoToolsService.class);
        return service.concat(blobs);
    }

    @OperationMethod
    public Blob run(DocumentModelList docs) throws NuxeoException, IOException, CommandNotAvailable {
        BlobList blobs = new BlobList();
        for (DocumentModel doc : docs) {
            if (StringUtils.isEmpty(xpath)) {
                blobs.add(doc.getAdapter(BlobHolder.class).getBlob());
            } else {
                blobs.add((Blob) doc.getPropertyValue(xpath));
            }
        }
        return run(blobs);
    }
}
