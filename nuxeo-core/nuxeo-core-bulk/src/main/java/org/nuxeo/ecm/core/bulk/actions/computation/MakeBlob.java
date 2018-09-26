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

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;

/**
 * @since 10.3
 */
public class MakeBlob extends AbstractTransientBlobComputation {

    public static final String NAME = "makeBlob";

    protected Path temp;

    protected long count;

    public MakeBlob() {
        super(NAME);
    }

    @Override
    public void processRecord(ComputationContext context, String documentIdsStreamName, Record record) {
        String commandId = CSVProjection.getCommandIdFromKey(record.getKey());
        int documents = CSVProjection.getDocumentCountFromKey(record.getKey());
        appendToFile(commandId, record.getData(), documents);
        // TODO: use a cache per commandid so we don't need a KV roundtrip on each record + check when count=0
        if (getLinesCount(commandId) == count) {
            saveInTransientStore(context, commandId);
        }
    }

    protected void appendToFile(String commandId, byte[] content, long count) {
        if (temp == null) {
            try {
                temp = createTemp(commandId);
            } catch (IOException e) {
                getLog().error(e, e);
                return;
            }
        }
        try (FileOutputStream stream = new FileOutputStream(temp.toFile(), true)) {
            stream.write(content);
            stream.flush();
            this.count += count;
        } catch (IOException e) {
            getLog().error(e, e);
        }
    }

    protected void saveInTransientStore(ComputationContext context, String commandId) {
        storeBlob(new FileBlob(temp.toFile()), commandId);
        try {
            Files.delete(temp);
        } catch (IOException e) {
            getLog().error(e, e);
        }
        context.produceRecord("o1", Record.of(CSVProjection.buildRecordKey(commandId, count),
                getTransientStoreKey(commandId).getBytes(UTF_8)));
        context.askForCheckpoint();
        temp = null;
        count = 0;
    }

}
