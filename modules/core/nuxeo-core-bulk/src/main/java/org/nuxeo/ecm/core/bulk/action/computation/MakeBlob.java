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

import static org.nuxeo.ecm.core.bulk.action.computation.SortBlob.SORT_PARAMETER;
import static org.nuxeo.ecm.core.bulk.action.computation.ZipBlob.ZIP_PARAMETER;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.DataBucket;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.internals.ComputationContextImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 10.3
 */
public class MakeBlob extends AbstractTransientBlobComputation {

    private static final Logger log = LogManager.getLogger(MakeBlob.class);

    public static final String NAME = "bulk/makeBlob";

    protected static final long CHECK_DELAY_MS = 1000;

    protected static final String SORT_STREAM = OUTPUT_1;

    protected static final String ZIP_STREAM = OUTPUT_2;

    protected static final String EXPOSE_BLOB_STREAM = OUTPUT_3;

    protected static final int NB_OUTPUT_STREAMS = 3;

    protected final Map<String, Long> counters = new HashMap<>();

    protected final Map<String, Long> totals = new HashMap<>();

    protected final Map<String, DataBucket> lastBuckets = new HashMap<>();

    protected final boolean produceImmediate;

    public MakeBlob() {
        this(false);
    }

    public MakeBlob(boolean produceImmediate) {
        super(NAME, NB_OUTPUT_STREAMS);
        this.produceImmediate = produceImmediate;
    }

    @Override
    public void init(ComputationContext context) {
        super.init(context);
        context.setTimer("check", System.currentTimeMillis() + CHECK_DELAY_MS);
    }

    @Override
    public void processTimer(ComputationContext context, String key, long timestamp) {
        // This timer is useful only to solve race condition that happens when the total is known
        // after the records have already been processed
        List<String> commands = counters.keySet()
                                        .stream()
                                        .filter(commandId -> !totals.containsKey(commandId)
                                                && counters.get(commandId) >= getTotal(commandId))
                                        .collect(Collectors.toList());
        commands.forEach(commandId -> finishBlob(context, commandId));
        context.setTimer("check", System.currentTimeMillis() + CHECK_DELAY_MS);
    }

    @Override
    public void processRecord(ComputationContext context, String documentIdsStreamName, Record record) {
        Codec<DataBucket> codec = BulkCodecs.getDataBucketCodec();
        DataBucket in = codec.decode(record.getData());
        String commandId = in.getCommandId();
        long nbDocuments = in.getCount();

        appendToFile(commandId, in.getData());

        if (counters.containsKey(commandId)) {
            counters.put(commandId, nbDocuments + counters.get(commandId));
        } else {
            counters.put(commandId, nbDocuments);
        }
        lastBuckets.put(commandId, in);
        if (counters.get(commandId) < getTotal(commandId)) {
            return;
        }
        finishBlob(context, commandId);
    }

    protected Long getTotal(String commandId) {
        if (!totals.containsKey(commandId)) {
            long total = Framework.getService(BulkService.class).getStatus(commandId).getTotal();
            if (total == 0) {
                return Long.MAX_VALUE;
            }
            totals.put(commandId, total);
        }
        return totals.get(commandId);
    }

    protected Path appendToFile(String commandId, byte[] content) {
        Path path = createTemp(commandId);
        try (FileOutputStream stream = new FileOutputStream(path.toFile(), true)) {
            stream.write(content);
            stream.flush();
        } catch (IOException e) {
            log.error("Unable to write content", e);
        }
        return path;
    }

    protected void appendHeaderFooterToFile(Path filePath, String commandId, byte[] header, byte[] footer) {
        if (header.length == 0 && footer.length == 0) {
            return;
        }
        try {
            Path tmpPath = Files.move(filePath, createTemp("tmp" + commandId), StandardCopyOption.REPLACE_EXISTING);
            try (InputStream is = Files.newInputStream(tmpPath);
                    FileOutputStream os = new FileOutputStream(filePath.toFile(), true)) {
                if (header.length > 0) {
                    os.write(header);
                }
                IOUtils.copy(is, os);
                if (footer.length > 0) {
                    os.write(footer);
                }
                os.flush();
            } finally {
                Files.delete(tmpPath);
            }
        } catch (IOException e) {
            log.error("Unable to append header and footer", e);
        }
    }

    protected String saveInTransientStore(String commandId, String storeName) {
        Path path = createTemp(commandId);
        storeBlob(new FileBlob(path.toFile()), commandId, storeName);
        try {
            Files.delete(path);
        } catch (IOException e) {
            log.error("Unable to delete file", e);
        }
        return getTransientStoreKey(commandId);
    }

    protected String getOutputStream(String commandId) {
        String outputStream = EXPOSE_BLOB_STREAM;
        BulkCommand command = Framework.getService(BulkService.class).getCommand(commandId);
        boolean sort = true;
        boolean zip = false;
        if (command != null) {
            if (command.getParam(SORT_PARAMETER) != null) {
                sort = command.getParam(SORT_PARAMETER);
            }
            if (command.getParam(ZIP_PARAMETER) != null) {
                zip = command.getParam(ZIP_PARAMETER);
            }
        }
        if (sort) {
            outputStream = SORT_STREAM;
        } else if (zip) {
            outputStream = ZIP_STREAM;
        }
        return outputStream;
    }

    protected void finishBlob(ComputationContext context, String commandId) {
        String outputStream = getOutputStream(commandId);
        DataBucket in = lastBuckets.get(commandId);
        if (!SORT_STREAM.equals(outputStream)) {
            appendHeaderFooterToFile(createTemp(commandId), commandId, in.getHeader(), in.getFooter());
        }

        String storeName = Framework.getService(BulkService.class).getStatus(commandId).getAction();
        String value = saveInTransientStore(commandId, storeName);
        DataBucket out = new DataBucket(commandId, totals.get(commandId), value, in.getHeaderAsString(),
                in.getFooterAsString());
        Codec<DataBucket> codec = BulkCodecs.getDataBucketCodec();

        if (produceImmediate) {
            ((ComputationContextImpl) context).produceRecordImmediate(outputStream,
                    Record.of(commandId, codec.encode(out)));
        } else {
            context.produceRecord(outputStream, Record.of(commandId, codec.encode(out)));
        }
        totals.remove(commandId);
        counters.remove(commandId);
        lastBuckets.remove(commandId);
        // we checkpoint only if there is not another command in progress
        if (counters.isEmpty()) {
            context.askForCheckpoint();
        }
    }

}
