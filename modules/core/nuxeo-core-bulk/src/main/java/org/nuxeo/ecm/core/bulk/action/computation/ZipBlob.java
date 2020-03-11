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

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.DataBucket;
import org.nuxeo.ecm.core.utils.BlobUtils;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 10.3
 */
public class ZipBlob extends AbstractTransientBlobComputation {

    private static final Logger log = LogManager.getLogger(ZipBlob.class);

    public static final String NAME = "zipBlob";

    public static final String ZIP_PARAMETER = "zip";

    public ZipBlob() {
        super(NAME);
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        Codec<DataBucket> codec = BulkCodecs.getDataBucketCodec();
        DataBucket in = codec.decode(record.getData());

        String storeName = Framework.getService(BulkService.class).getStatus(in.getCommandId()).getAction();
        Blob blob = getBlob(in.getDataAsString(), storeName);
        try {
            blob = BlobUtils.zip(blob, blob.getFilename() + ".zip");
        } catch (IOException e) {
            log.error("Unable to zip blob", e);
        }
        storeBlob(blob, in.getCommandId(), storeName);

        DataBucket out = new DataBucket(in.getCommandId(), in.getCount(), getTransientStoreKey(in.getCommandId()));
        context.produceRecord(OUTPUT_1, Record.of(in.getCommandId(), codec.encode(out)));
        context.askForCheckpoint();
    }

}
