/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.bulk;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.nuxeo.ecm.core.bulk.BulkComponent.BULK_KV_STORE_NAME;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.COMMAND;

import java.io.IOException;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;

/**
 * Helper class for bulk commands.
 *
 * @since 10.2
 */
public class BulkCommands {

    private BulkCommands() {
        // utility class
    }

    public static BulkCommand fromKVStore(String commandId) {
        KeyValueStore kvStore = Framework.getService(KeyValueService.class).getKeyValueStore(BULK_KV_STORE_NAME);
        return fromKVStore(kvStore, commandId);
    }

    protected static BulkCommand fromKVStore(KeyValueStore kvStore, String commandId) {
        byte[] data = kvStore.get(commandId + COMMAND);
        return fromBytes(data);
    }

    public static BulkCommand fromBytes(byte[] data) {
        String json = new String(data, UTF_8);
        try {
            return MarshallerHelper.jsonToObject(BulkCommand.class, json, RenderingContext.CtxBuilder.get());
        } catch (IOException e) {
            throw new NuxeoException("Invalid json bulkCommand=" + json, e);
        }
    }

    public static byte[] toBytes(BulkCommand command) {
        try {
            return MarshallerHelper.objectToJson(command, RenderingContext.CtxBuilder.get()).getBytes();
        } catch (IOException e) {
            throw new NuxeoException("Unable to serialize the bulk command=" + command, e);
        }
    }
}
