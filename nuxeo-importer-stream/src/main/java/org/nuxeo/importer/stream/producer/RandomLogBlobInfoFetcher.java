/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */

package org.nuxeo.importer.stream.producer;

import java.time.Duration;

import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.importer.stream.message.BlobInfoMessage;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;

/**
 * Returns blob information from a Log, loop on the log.
 *
 * @since 9.3
 */
public class RandomLogBlobInfoFetcher implements BlobInfoFetcher {
    protected static final int READ_DELAY_MS = 100;

    protected final LogTailer<BlobInfoMessage> tailer;

    protected boolean first;

    public RandomLogBlobInfoFetcher(LogTailer<BlobInfoMessage> blobInfoTailer) {
        this.tailer = blobInfoTailer;
        this.first = true;
    }

    @Override
    public BlobInfo get(DocumentMessage.Builder builder) {
        LogRecord<BlobInfoMessage> record;
        try {
            record = tailer.read(Duration.ofMillis(READ_DELAY_MS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (record == null) {
            if (first) {
                // there is no record in this partition, no need to loop
                return null;
            }
            // start again from beginning
            tailer.toStart();
            return get(builder);
        }
        first = false;
        return record.message();
    }

    @Override
    public void close() {
        tailer.close();
    }
}
