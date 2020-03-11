/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.transientstore.computation;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.work.BaseOverflowRecordFilter;
import org.nuxeo.runtime.api.Framework;

/**
 * Filter that use a Transient Store to pass big record value. The TTL needs to be configured at the TransientStore
 * level.
 *
 * @since 11.1
 */
public class TransientStoreOverflowRecordFilter extends BaseOverflowRecordFilter {
    private static final Logger log = LogManager.getLogger(TransientStoreOverflowRecordFilter.class);

    @Override
    public void init(Map<String, String> options) {
        super.init(options);
        // check for ts availability
        getTransientStore();
    }

    protected TransientStore getTransientStore() {
        return Framework.getService(TransientStoreService.class).getStore(getStoreName());
    }

    @Override
    protected void storeValue(String recordKey, byte[] data) {
        String key = getPrefixedKey(recordKey);
        Blob blob = new ByteArrayBlob(data);
        TransientStore store = getTransientStore();
        store.putBlobs(key, Collections.singletonList(blob));
        store.setCompleted(key, true);

    }

    @Override
    protected byte[] fetchValue(String recordKey) {
        String key = getPrefixedKey(recordKey);
        List<Blob> blobs = getTransientStore().getBlobs(key);
        Blob blob = blobs == null || blobs.isEmpty() ? null : blobs.get(0);
        if (blob == null) {
            log.error("Blob value not found for record: {}", recordKey);
            return null;
        }
        try {
            return blob.getByteArray();
        } catch (IOException e) {
            log.error("Cannot get bytes of blob value for record: {}", recordKey, e);
            return null;
        }
    }

}
