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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.message.DataBucket;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;

import com.google.code.externalsorting.ExternalSort;

/**
 * @since 10.3
 */
public class SortBlob extends AbstractTransientBlobComputation {

    public static final String NAME = "sortBlob";

    public SortBlob() {
        super(NAME);
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        Codec<DataBucket> codec = BulkCodecs.getDataBucketCodec();
        DataBucket in = codec.decode(record.getData());

        String commandId = in.getCommandId();
        Blob tmpBlob = getBlob(in.getDataAsString());
        tmpBlob = sort(tmpBlob, commandId);

        // Create a new file to add header and footer
        Path path = createTemp(commandId);
        try (InputStream is = tmpBlob.getStream(); FileOutputStream os = new FileOutputStream(path.toFile(), true)) {
            os.write(in.getHeader());
            IOUtils.copy(is, os);
            os.write(in.getFooter());
            os.flush();
            tmpBlob.getFile().delete();
        } catch (IOException e) {
            getLog().error(e, e);
        }

        storeBlob(new FileBlob(path.toFile()), commandId);

        DataBucket out = new DataBucket(commandId, in.getCount(), getTransientStoreKey(commandId));
        context.produceRecord(OUTPUT_1, Record.of(commandId, codec.encode(out)));
        context.askForCheckpoint();
    }

    protected Blob sort(Blob blob, String commandId) {
        try {
            Path temp = createTemp("tmp" + commandId);
            ExternalSort.sort(blob.getFile(), temp.toFile());
            return new FileBlob(temp.toFile());
        } catch (IOException e) {
            getLog().error(e, e);
            return blob;
        }
    }

}
