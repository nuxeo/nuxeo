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

import static org.nuxeo.ecm.core.bulk.action.computation.ZipBlob.ZIP_PARAMETER;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.DataBucket;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;

import com.google.code.externalsorting.ExternalSort;

/**
 * @since 10.3
 */
public class SortBlob extends AbstractTransientBlobComputation {

    private static final Logger log = LogManager.getLogger(SortBlob.class);

    public static final String NAME = "bulk/sortBlob";

    public static final String SORT_PARAMETER = "sort";

    protected static final String ZIP_STREAM = OUTPUT_1;

    protected static final String EXPOSE_BLOB_STREAM = OUTPUT_2;

    protected static final int NB_OUTPUT_STREAMS = 2;

    public SortBlob() {
        super(NAME, NB_OUTPUT_STREAMS);
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        Codec<DataBucket> codec = BulkCodecs.getDataBucketCodec();
        DataBucket in = codec.decode(record.getData());

        String commandId = in.getCommandId();
        String storeName = Framework.getService(BulkService.class).getStatus(commandId).getAction();
        Blob tmpBlob = getBlob(in.getDataAsString(), storeName);
        tmpBlob = sort(tmpBlob, commandId);

        // Create a new file to add header and footer
        Path path = createTemp(commandId);
        try (InputStream is = tmpBlob.getStream(); FileOutputStream os = new FileOutputStream(path.toFile(), true)) {
            os.write(in.getHeader());
            IOUtils.copy(is, os);
            os.write(in.getFooter());
            os.flush();
        } catch (IOException e) {
            log.error("Unable to copy header/footer", e);
        }
        try {
            Files.delete(tmpBlob.getFile().toPath());
        } catch (IOException e) {
            log.error("Unable to delete tmp file", e);
        }

        storeBlob(new FileBlob(path.toFile()), commandId, storeName);

        BulkCommand command = Framework.getService(BulkService.class).getCommand(commandId);
        boolean zip = command.getParam(ZIP_PARAMETER) != null ? command.getParam(ZIP_PARAMETER) : false;
        String outputStream = zip ? ZIP_STREAM : EXPOSE_BLOB_STREAM;

        DataBucket out = new DataBucket(commandId, in.getCount(), getTransientStoreKey(commandId));
        context.produceRecord(outputStream, Record.of(commandId, codec.encode(out)));
        context.askForCheckpoint();
    }

    protected Blob sort(Blob blob, String commandId) {
        try {
            Path temp = createTemp("tmp" + commandId);
            ExternalSort.sort(blob.getFile(), temp.toFile());
            return new FileBlob(temp.toFile());
        } catch (IOException e) {
            log.error("Unable to sort blob", e);
            return blob;
        }
    }

}
