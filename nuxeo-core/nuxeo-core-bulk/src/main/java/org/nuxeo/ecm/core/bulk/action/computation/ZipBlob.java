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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.utils.BlobUtils;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;

/**
 * @since 10.3
 */
public class ZipBlob extends AbstractTransientBlobComputation {

    public static final String NAME = "zipBlob";

    public ZipBlob() {
        super(NAME);
    }

    @Override
    public void processRecord(ComputationContext context, String documentIdsStreamName, Record record) {
        String commandId = CSVProjection.getCommandIdFromKey(record.getKey());
        Blob blob = getBlob(new String(record.getData(), UTF_8));
        try {
            blob = BlobUtils.zip(blob, null);
        } catch (IOException e) {
            getLog().error(e, e);
        }
        storeBlob(blob, commandId);
        context.produceRecord("o1", Record.of(record.getKey(), getTransientStoreKey(commandId).getBytes(UTF_8)));
        context.askForCheckpoint();
    }

}
