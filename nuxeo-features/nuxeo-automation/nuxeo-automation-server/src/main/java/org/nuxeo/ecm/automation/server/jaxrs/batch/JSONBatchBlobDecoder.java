/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.automation.server.jaxrs.batch;

import org.nuxeo.ecm.automation.core.util.JSONBlobDecoder;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Uses a JSON definition to retrive a Blob uploaded in a batch
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class JSONBatchBlobDecoder implements JSONBlobDecoder {

    @Override
    public Blob getBlobFromJSON(ObjectNode jsonObject) {

        Blob blob = null;

        if (!jsonObject.has("upload-batch")) {
            return null;
        }

        final String batchId = jsonObject.get("upload-batch").textValue();
        String fileId = null;
        if (jsonObject.has("upload-fileId")) {
            fileId = jsonObject.get("upload-fileId").textValue();
        }
        if (fileId != null) {
            BatchManager bm = Framework.getService(BatchManager.class);
            Batch batch = bm.getBatch(batchId);
            if (batch == null) {
                return null;
            }
            blob = batch.getBlob(fileId);

            if (RequestContext.getActiveContext() != null) {
                final boolean drop = !Boolean.parseBoolean(
                        RequestContext.getActiveContext().getRequest().getHeader(BatchManagerConstants.NO_DROP_FLAG));
                if (drop) {
                    RequestContext.getActiveContext().addRequestCleanupHandler(request -> {
                        BatchManager bm1 = Framework.getService(BatchManager.class);
                        bm1.clean(batchId);
                    });
                }
            }
        }
        return blob;
    }

}
