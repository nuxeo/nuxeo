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
package org.nuxeo.ecm.core.bulk.actions.computation;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Collections;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 10.3
 */
public class ExposeBlob extends AbstractTransientBlobComputation {

    public static final String NAME = "exposeBlob";

    public ExposeBlob() {
        super(NAME);
    }

    @Override
    public void processRecord(ComputationContext context, String documentIdsStreamName, Record record) {
        String commandId = CSVProjection.getCommandIdFromKey(record.getKey());
        long documents = CSVProjection.getDocumentCountFromKey(record.getKey());
        Blob blob = getBlob(new String(record.getData(), UTF_8));
        // store it in download transient store
        TransientStore download = Framework.getService(TransientStoreService.class)
                                           .getStore(DownloadService.TRANSIENT_STORE_STORE_NAME);
        download.putBlobs(commandId, Collections.singletonList(blob));
        download.setCompleted(commandId, true);
        // update the command status
        AbstractBulkComputation.updateStatusProcessed(context, commandId, documents);
        context.askForCheckpoint();
    }

}
