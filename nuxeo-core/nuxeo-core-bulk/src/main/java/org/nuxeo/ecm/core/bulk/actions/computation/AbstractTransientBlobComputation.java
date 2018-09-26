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

import static org.nuxeo.ecm.core.bulk.BulkComponent.BULK_KV_STORE_NAME;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.actions.CSVExportAction;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;

/**
 * @since 10.3
 */
public abstract class AbstractTransientBlobComputation extends AbstractComputation {

    public AbstractTransientBlobComputation(String name) {
        super(name, 1, 1);
    }

    protected String getTransientStoreKey(String commandId) {
        return metadata.name() + commandId;
    }

    protected Blob getBlob(String key) {
        TransientStore store = Framework.getService(TransientStoreService.class).getStore(CSVExportAction.ACTION_NAME);
        List<Blob> blobs = store.getBlobs(key);
        Blob blob = blobs == null || blobs.isEmpty() ? null : blobs.get(0);
        if (blob == null) {
            getLog().error("Could not retrieve blob for key " + key);
        }
        return blob;
    }

    protected void storeBlob(Blob blob, String commandId) {
        TransientStore store = Framework.getService(TransientStoreService.class).getStore(CSVExportAction.ACTION_NAME);
        store.putBlobs(getTransientStoreKey(commandId), Collections.singletonList(blob));
        store.setCompleted(getTransientStoreKey(commandId), true);
    }

    protected Path createTemp(String commandId) throws IOException {
        Path temp = Files.createTempFile(null, null);
        temp = Files.move(temp, Paths.get(temp.getParent().toString(), commandId + ".csv"));
        return temp;
    }

    protected long getLinesCount(String commandId) {
        KeyValueStore kvStore = Framework.getService(KeyValueService.class).getKeyValueStore(BULK_KV_STORE_NAME);
        BulkStatus status = BulkCodecs.getStatusCodec().decode(kvStore.get(commandId + STATUS));
        return status.getCount();
    }

    protected Log getLog() {
        return LogFactory.getLog(getClass());
    }
}
