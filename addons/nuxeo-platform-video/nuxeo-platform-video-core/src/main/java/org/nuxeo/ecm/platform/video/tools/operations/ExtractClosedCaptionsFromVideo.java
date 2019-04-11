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
 * Extracts Closed Captions from a Video.
 *
 * @since 8.4
 */
@Operation(id = ExtractClosedCaptionsFromVideo.ID, category = Constants.CAT_CONVERSION, label = "Extracts closed captions from the video.", description = "Extracts the closed captions from the whole video or from a part of it when startAt and end time is provided. The output format references how the output is generated, and xpath can be used to indicate the video blob when using documents.")
public class ExtractClosedCaptionsFromVideo {

    public static final String ID = "Video.ExtractClosedCaptions";

    @Param(name = "outFormat", required = false, values = { "ttxt", "srt", "txt" })
    protected String outFormat;

    @Param(name = "startAt", required = false)
    protected String startAt;

    @Param(name = "endAt", required = false)
    protected String endAt;

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
        for (DocumentModel doc : input) {
            blobList.add((run(doc)));
        }
        return blobList;
    }

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob input) throws OperationException {
        try {
            VideoToolsService service = Framework.getService(VideoToolsService.class);
            return service.extractClosedCaptions(input, outFormat, startAt, endAt);
        } catch (NuxeoException e) {
            throw new OperationException(e);
        }
    }

}
