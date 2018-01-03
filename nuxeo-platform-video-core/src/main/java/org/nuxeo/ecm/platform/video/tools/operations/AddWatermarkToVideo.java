/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.video.tools.VideoToolsService;
import org.nuxeo.runtime.api.Framework;

/**
 * Watermark a Video with the given Picture, at the given position (from top-left).
 *
 * @since 8.4
 */
@Operation(id = AddWatermarkToVideo.ID, category = Constants.CAT_BLOB, label = "Watermarks a Video with a Picture", description = "Watermark the video with the picture stored in file:content of watermark, at the position(x, y) from the left-top corner of the picture.")
public class AddWatermarkToVideo {

    public static final String ID = "Video.AddWatermark";

    @Param(name = "watermark")
    protected DocumentModel watermark;

    @Param(name = "x", required = false, values = { "0" })
    protected String x;

    @Param(name = "y", required = false, values = { "0" })
    protected String y;

    @Param(name = "xpath", required = false)
    protected String xpath;

    @OperationMethod
    public Blob run(DocumentModel input) throws OperationException {
        if (StringUtils.isEmpty(xpath)) {
            return run(input.getAdapter(BlobHolder.class).getBlob());
        } else {
            return run((Blob) input.getPropertyValue(xpath));
        }
    }

    @OperationMethod
    public BlobList run(DocumentModelList input) throws OperationException {
        BlobList blobList = new BlobList();
        String blobPath = StringUtils.isEmpty(xpath) ? "file:content" : xpath;
        for (DocumentModel doc : input) {
            blobList.add(run((Blob) doc.getPropertyValue(blobPath)));
        }
        return blobList;
    }

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob input) throws OperationException {
        Blob watermarkBlob = (Blob) watermark.getPropertyValue("file:content");
        try {
            VideoToolsService service = Framework.getService(VideoToolsService.class);
            return service.watermark(input, watermarkBlob, x, y);
        } catch (NuxeoException e) {
            throw new OperationException("Cannot add the watermark to the video.", e);
        }
    }
}
