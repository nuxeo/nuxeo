/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.test.actions;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.bulk.action.computation.ExposeBlob;
import org.nuxeo.ecm.core.bulk.action.computation.MakeBlob;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.message.DataBucket;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

public class DummyExposeBlobAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "dummyExposeBlob";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(DummyComputation::new,
                               Arrays.asList(INPUT_1 + ":" + ACTION_NAME, OUTPUT_1 + ":" + MakeBlob.NAME))
                       .addComputation(MakeBlob::new,
                               Arrays.asList(INPUT_1 + ":" + MakeBlob.NAME, OUTPUT_1 + ":" + ExposeBlob.NAME))
                       .addComputation(ExposeBlob::new,
                               Arrays.asList(INPUT_1 + ":" + ExposeBlob.NAME, OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    /**
     * A dummy computation that produces a record
     */
    public static class DummyComputation extends AbstractBulkComputation {

        public DummyComputation() {
            super(ACTION_NAME);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            // do nothing
        }

        @Override
        public void endBucket(ComputationContext context, BulkStatus delta) {
            String commandId = delta.getId();
            Codec<DataBucket> codec = BulkCodecs.getDataBucketCodec();
            DataBucket data = new DataBucket(commandId, delta.getProcessed(), "foo");
            Record record = Record.of(commandId, codec.encode(data));
            context.produceRecord(OUTPUT_1, record);
        }
    }
}
