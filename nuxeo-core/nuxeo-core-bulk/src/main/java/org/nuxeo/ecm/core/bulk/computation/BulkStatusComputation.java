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
 *     Funsho David
 */
package org.nuxeo.ecm.core.bulk.computation;


import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;

import static org.nuxeo.ecm.core.bulk.BulkComponent.BULK_KV_STORE_NAME;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS;

/**
 * Reads status and save them to a key value store.
 *
 * @since 10.2
 */
public class BulkStatusComputation extends AbstractComputation {

    public BulkStatusComputation(String name) {
        super(name, 1, 0);
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        KeyValueStore kvStore = Framework.getService(KeyValueService.class).getKeyValueStore(BULK_KV_STORE_NAME);
        BulkStatus status = BulkCodecs.getStatusCodec().decode(record.getData());
        kvStore.put(status.getId() + STATUS, record.getData());
        context.askForCheckpoint();
    }
}
