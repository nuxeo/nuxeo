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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.bulk.action;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.BulkServiceImpl;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.action.computation.MakeBlob;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.message.DataBucket;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.ComputationMetadataMapping;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.internals.ComputationContextImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreBulkFeature.class)
public class TestMakeBlob {
    @Test
    public void testSimple() throws IOException {
        final String command = "cmd1";
        final int count = 6;

        createStatus(command, count);

        // Create 2 input records
        Record record1 = createRecord(command, "ab", 2);
        Record record2 = createRecord(command, "cdef", 4);

        // init the computation
        MakeBlob comp = new MakeBlob();
        ComputationContextImpl context = new ComputationContextImpl(
                new ComputationMetadataMapping(comp.metadata(), Collections.emptyMap()));
        comp.init(context);

        // submit one record
        comp.processRecord(context, "i1", record1);

        // no output so far and no checkpoint
        assertEquals(0, context.getRecords("o1").size());
        assertFalse(context.requireCheckpoint());

        // submit the last record of the command, expecting output and checkpoint
        comp.processRecord(context, "i1", record2);
        assertEquals(1, context.getRecords("o1").size());
        assertTrue(context.requireCheckpoint());

        // check the output key
        Record output = context.getRecords("o1").get(0);
        assertEquals(command, output.getKey());
        Codec<DataBucket> codec = BulkCodecs.getDataBucketCodec();
        DataBucket outData = codec.decode(output.getData());
        assertEquals(command, outData.getCommandId());
        assertEquals(count, outData.getCount());

        // check the output blob
        String blobPath = codec.decode(output.getData()).getDataAsString();
        Blob blob = comp.getBlob(blobPath);
        assertBlobEquals("abcdef", blob);

        comp.destroy();
    }

    protected void assertBlobEquals(String expected, Blob blob) throws IOException {
        assertNotNull(blob);
        int size = (int) blob.getLength();
        byte[] buffer = new byte[size];
        blob.getStream().read(buffer, 0, size);
        String result = new String(buffer, UTF_8);
        assertEquals(expected, result);
    }

    @Test
    public void testMixed() throws IOException {
        final String command1 = "cmd1";
        final String command2 = "cmd2";
        final int count = 6;

        createStatus(command1, count);
        createStatus(command2, count);

        // Create records
        Record c1r1 = createRecord(command1, "ab", 2);
        Record c1r2 = createRecord(command1, "c", 1);
        Record c1r3 = createRecord(command1, "def", 3);

        Record c2r1 = createRecord(command2, "m", 1);
        Record c2r2 = createRecord(command2, "no", 2);
        Record c2r3 = createRecord(command2, "p", 1);
        Record c2r4 = createRecord(command2, "qr", 2);

        // init the computation
        MakeBlob comp = new MakeBlob();
        ComputationContextImpl context = new ComputationContextImpl(
                new ComputationMetadataMapping(comp.metadata(), Collections.emptyMap()));
        comp.init(context);

        // submit interleaved records
        comp.processRecord(context, "i1", c1r1);
        assertEquals(0, context.getRecords("o1").size());
        comp.processRecord(context, "i1", c2r1);
        assertEquals(0, context.getRecords("o1").size());
        comp.processRecord(context, "i1", c1r2);
        assertEquals(0, context.getRecords("o1").size());
        comp.processRecord(context, "i1", c2r2);
        assertEquals(0, context.getRecords("o1").size());
        // submit the last record for command 1
        comp.processRecord(context, "i1", c1r3);
        assertEquals(1, context.getRecords("o1").size());
        Record output1 = context.getRecords("o1").get(0);
        // there is no checkpoint because commands are interleaved
        assertFalse(context.requireCheckpoint());
        // continue with command2
        comp.processRecord(context, "i1", c2r3);
        assertEquals(1, context.getRecords("o1").size());
        // last record command2
        comp.processRecord(context, "i1", c2r4);
        assertEquals(2, context.getRecords("o1").size());
        Record output2 = context.getRecords("o1").get(1);
        assertTrue(context.requireCheckpoint());

        // check the output key
        Codec<DataBucket> codec = BulkCodecs.getDataBucketCodec();
        DataBucket data1 = codec.decode(output1.getData());
        DataBucket data2 = codec.decode(output2.getData());
        assertEquals(command1, data1.getCommandId());
        assertEquals(count, data1.getCount());
        assertEquals(command2, data2.getCommandId());
        assertEquals(count, data2.getCount());

        // check the output blob
        String blobPath = codec.decode(output1.getData()).getDataAsString();
        assertBlobEquals("abcdef", comp.getBlob(blobPath));
        blobPath = codec.decode(output2.getData()).getDataAsString();
        assertBlobEquals("mnopqr", comp.getBlob(blobPath));

        comp.destroy();
    }

    protected Record createRecord(String commandId, String content, long count) {
        Codec<DataBucket> codec = BulkCodecs.getDataBucketCodec();
        DataBucket data = new DataBucket(commandId, count, content);
        return Record.of(commandId, codec.encode(data));
    }

    protected void createStatus(String commandId, int count) {
        BulkStatus status = new BulkStatus(commandId);
        status.setState(BulkStatus.State.RUNNING);
        status.setTotal(count);
        BulkServiceImpl service = (BulkServiceImpl) Framework.getService(BulkService.class);
        service.setStatus(status);
    }
}
