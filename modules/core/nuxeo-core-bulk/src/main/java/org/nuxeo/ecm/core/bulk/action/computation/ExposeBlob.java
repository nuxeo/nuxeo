/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.core.bulk.action.computation;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.message.DataBucket;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 10.3
 */
public class ExposeBlob extends AbstractTransientBlobComputation {

    public static final String NAME = "bulk/exposeBlob";

    public ExposeBlob() {
        super(NAME);
    }

    @Override
    public void processRecord(ComputationContext context, String documentIdsStreamName, Record record) {
        Codec<DataBucket> codec = BulkCodecs.getDataBucketCodec();
        DataBucket in = codec.decode(record.getData());
        String commandId = in.getCommandId();
        long documents = in.getCount();

        String storeName = Framework.getService(BulkService.class).getStatus(commandId).getAction();
        Blob blob = getBlob(in.getDataAsString(), storeName);
        // store it in download transient store
        TransientStore store = Framework.getService(TransientStoreService.class)
                                        .getStore(DownloadService.TRANSIENT_STORE_STORE_NAME);
        store.putBlobs(commandId, Collections.singletonList(blob));
        store.setCompleted(commandId, true);

        // update the command status
        BulkStatus delta = BulkStatus.deltaOf(commandId);
        delta.setProcessed(documents);
        String url = Framework.getService(DownloadService.class).getDownloadUrl(commandId);
        Map<String, Serializable> result = Collections.singletonMap("url", url);
        delta.setResult(result);
        AbstractBulkComputation.updateStatus(context, delta);
        context.askForCheckpoint();
    }

}
