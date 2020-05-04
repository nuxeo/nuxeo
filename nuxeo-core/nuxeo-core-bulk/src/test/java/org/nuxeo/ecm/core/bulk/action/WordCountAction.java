/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.message.DataBucket;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

public class WordCountAction implements StreamProcessorTopology {

    protected static final String ACTION_NAME = "testWordCount";

    protected static final String ACTION_STREAM = ACTION_NAME;

    protected static final String AGGREGATOR_STREAM = "countAggregator";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(WordCountComputation::new, Arrays.asList(INPUT_1 + ":" + ACTION_STREAM, //
                               OUTPUT_1 + ":" + AGGREGATOR_STREAM))
                       .addComputation(CountAggregatorComputation::new, Arrays.asList(INPUT_1 + ":" + AGGREGATOR_STREAM, //
                               OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    protected class WordCountComputation extends AbstractBulkComputation {
        public WordCountComputation() {
            super("wordCount");
        }

        protected int count;

        @Override
        public void startBucket(String bucketKey) {
            count = 0;
        }

        protected int countWordInLine(String line) {
            String trim = line.trim();
            if (trim.isEmpty()) {
                return 0;
            }
            return trim.split("\\s+").length;
        }

        @Override
        protected void compute(CoreSession coreSession, List<String> lines, Map<String, Serializable> params) {
            count += lines.stream().map(this::countWordInLine).reduce(0, Integer::sum);
        }

        @Override
        public void endBucket(ComputationContext context, BulkStatus delta) {
            String commandId = delta.getId();
            DataBucket dataBucket = new DataBucket(commandId, delta.getProcessed(), String.valueOf(count));
            Record record = Record.of(commandId, BulkCodecs.getDataBucketCodec().encode(dataBucket));
            context.produceRecord(OUTPUT_1, record);
        }
    }

    protected class CountAggregatorComputation extends AbstractComputation {
        protected final Map<String, Integer> counters = new HashMap<>();

        public CountAggregatorComputation() {
            super("countAggregator", 1, 1);
        }

        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            // extract DataBucket
            DataBucket data = BulkCodecs.getDataBucketCodec().decode(record.getData());
            // get the word count
            Integer wordCount = Integer.valueOf(data.getDataAsString());
            // aggregate
            Integer total = counters.getOrDefault(data.getCommandId(), 0) + wordCount;
            counters.put(data.getCommandId(), total);

            // update command status with the current total result
            Map<String, Serializable> result = Collections.singletonMap("wordCount", total);
            BulkStatus delta = BulkStatus.deltaOf(data.getCommandId());
            delta.setProcessed(data.getCount());
            delta.setResult(result);
            AbstractBulkComputation.updateStatus(context, delta);
            context.askForCheckpoint();
            // TODO: cleanup the counters map
        }

    }

}
