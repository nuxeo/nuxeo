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
 *     pierre
 */
package org.nuxeo.ecm.core.bulk.actions;

import static org.nuxeo.ecm.core.bulk.BulkComponent.BULK_KV_STORE_NAME;
import static org.nuxeo.ecm.core.bulk.BulkRecords.commandIdFrom;
import static org.nuxeo.ecm.core.bulk.BulkRecords.countFrom;
import static org.nuxeo.ecm.core.bulk.BulkRecords.dataFrom;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS;
import static org.nuxeo.ecm.core.bulk.StreamBulkProcessor.COUNTER_ACTION_NAME;
import static org.nuxeo.ecm.core.io.marshallers.csv.AbstractCSVWriter.TEXT_CSV_TYPE;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.BulkCounter;
import org.nuxeo.ecm.core.bulk.BulkRecords;
import org.nuxeo.ecm.core.bulk.BulkStatus;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.utils.BlobUtils;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology.Builder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;

import com.google.code.externalsorting.ExternalSort;

/**
 * @since 10.3
 */
public class CSVExportAction extends AbstractBulkAction {

    public static final String ACTION_NAME = "csvExport";

    @Override
    protected Builder addComputations(Builder builder, int size, int threshold) {
        return builder
                      .addComputation(() -> new DocumentCSVProjection(size, threshold),
                              Arrays.asList("i1:" + ACTION_NAME, "o1:" + MakeBlob.NAME))
                      .addComputation(MakeBlob::new,
                              Arrays.asList("i1:" + MakeBlob.NAME, "o1:" + SortBlob.NAME))
                      .addComputation(SortBlob::new,
                              Arrays.asList("i1:" + SortBlob.NAME, "o1:" + ZipBlob.NAME))
                      .addComputation(ZipBlob::new,
                              Arrays.asList("i1:" + ZipBlob.NAME, "o1:" + ExposeBlob.NAME))
                      .addComputation(ExposeBlob::new,
                              Arrays.asList("i1:" + ExposeBlob.NAME, "o1:" + COUNTER_ACTION_NAME));
    }

    public static class DocumentCSVProjection extends AbstractBulkComputation {

        protected OutputStream out;

        public DocumentCSVProjection(int size, int timer) {
            super(ACTION_NAME, 1, 1, size, timer);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {

            out = new ByteArrayOutputStream();

            DocumentRef[] refs = ids.stream().map(IdRef::new).collect(Collectors.toList()).toArray(new DocumentRef[0]);
            DocumentModelList list = session.getDocuments(refs);

            MarshallerRegistry registry = Framework.getService(MarshallerRegistry.class);
            Writer<DocumentModelList> writer = registry.getWriter(null, DocumentModelList.class, TEXT_CSV_TYPE);

            try {
                writer.write(list, DocumentModelList.class, null, TEXT_CSV_TYPE, out);
            } catch (IOException e) {
                getLog().error(e, e);
            }
        }

        @Override
        public void produceOutput(ComputationContext context) {
            context.produceRecord("o1", BulkRecords.of(currentCommandId, documentIds.size(), out.toString()));
        }

    }

    public static class MakeBlob extends AbstractTransientBlobComputation {

        public static final String NAME = "makeBlob";

        private Path temp;

        private long count;

        public MakeBlob() {
            super(NAME);
        }

        @Override
        public void processRecord(ComputationContext context, String documentIdsStreamName, Record record) {
            String commandId = commandIdFrom(record);
            appendToFile(commandId, dataFrom(record), countFrom(record));
            if (getLinesCount(commandId) == count) {
                saveInTransientStore(context, commandId);
            }
        }

        protected void appendToFile(String commandId, String content, long count) {
            if (temp == null) {
                try {
                    temp = createTemp(commandId);
                } catch (IOException e) {
                    getLog().error(e, e);
                    return;
                }
            }
            try (FileOutputStream stream = new FileOutputStream(temp.toFile(), true)) {
                stream.write(content.getBytes());
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
            context.produceRecord("o1", BulkRecords.of(commandId, count, getKey(commandId)));
            context.askForCheckpoint();
            temp = null;
            count = 0;
        }

    }

    public static class SortBlob extends AbstractTransientBlobComputation {

        public static final String NAME = "sortBlob";

        public SortBlob() {
            super(NAME);
        }

        @Override
        public void processRecord(ComputationContext context, String documentIdsStreamName, Record record) {
            String commandId = commandIdFrom(record);
            Blob blob = getBlob(dataFrom(record));
            blob = sort(blob, commandId);
            storeBlob(blob, commandId);
            context.produceRecord("o1", BulkRecords.of(commandId, countFrom(record), getKey(commandId)));
            context.askForCheckpoint();
        }

        protected Blob sort(Blob blob, String commandId) {
            try {
                Path temp = createTemp(commandId);
                ExternalSort.sort(blob.getFile(), temp.toFile());
                return new FileBlob(temp.toFile());
            } catch (IOException e) {
                getLog().error(e, e);
                return blob;
            }
        }

    }

    public static class ZipBlob extends AbstractTransientBlobComputation {

        public static final String NAME = "zipBlob";

        public ZipBlob() {
            super(NAME);
        }

        @Override
        public void processRecord(ComputationContext context, String documentIdsStreamName, Record record) {
            String commandId = commandIdFrom(record);
            Blob blob = getBlob(dataFrom(record));
            try {
                blob = BlobUtils.zip(blob, null);
            } catch (IOException e) {
                getLog().error(e, e);
            }
            storeBlob(blob, commandId);
            context.produceRecord("o1", BulkRecords.of(commandId, countFrom(record), getKey(commandId)));
            context.askForCheckpoint();
        }

    }

    public static class ExposeBlob extends AbstractTransientBlobComputation {

        public static final String NAME = "exposeBlob";

        public ExposeBlob() {
            super(NAME);
        }

        @Override
        public void processRecord(ComputationContext context, String documentIdsStreamName, Record record) {
            String commandId = commandIdFrom(record);
            Blob blob = getBlob(dataFrom(record));
            // store it in download transient store
            TransientStore download = Framework.getService(TransientStoreService.class)
                                               .getStore(DownloadService.TRANSIENT_STORE_STORE_NAME);
            download.putBlobs(commandId, Collections.singletonList(blob));
            download.setCompleted(commandId, true);
            // update the command status
            BulkCounter counter = new BulkCounter(commandId, countFrom(record));
            context.produceRecord("o1", commandId, BulkCodecs.getBulkCounterCodec().encode(counter));
            context.askForCheckpoint();
        }

    }

    protected abstract static class AbstractTransientBlobComputation extends AbstractComputation {

        public AbstractTransientBlobComputation(String name) {
            super(name, 1, 1);
        }

        protected String getKey(String commandId) {
            return metadata.name() + commandId;
        }

        protected Blob getBlob(String key) {
            TransientStore store = Framework.getService(TransientStoreService.class).getStore(ACTION_NAME);
            List<Blob> blobs = store.getBlobs(key);
            Blob blob = blobs == null || blobs.isEmpty() ? null : blobs.get(0);
            if (blob == null) {
                getLog().error("Could not retrienve blob for key " + key);
            }
            return blob;
        }

        protected void storeBlob(Blob blob, String commandId) {
            TransientStore store = Framework.getService(TransientStoreService.class).getStore(ACTION_NAME);
            store.putBlobs(getKey(commandId), Collections.singletonList(blob));
            store.setCompleted(getKey(commandId), true);
        }

        protected Path createTemp(String commandId) throws IOException {
            Path temp = Files.createTempFile(null, null);
            temp = Files.move(temp, Paths.get(temp.getParent().toString(), commandId + ".csv"));
            return temp;
        }

        protected long getLinesCount(String commandId) {
            KeyValueStore kvStore = Framework.getService(KeyValueService.class).getKeyValueStore(BULK_KV_STORE_NAME);
            BulkStatus status = BulkCodecs.getBulkStatusCodec().decode(kvStore.get(commandId + STATUS));
            return status.getCount();
        }

        protected Log getLog() {
            return LogFactory.getLog(getClass());
        }
    }

}
