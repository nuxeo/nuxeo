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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;

import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * @since 10.2
 */
public class TestCountProcessor implements StreamProcessorTopology {

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(() -> new CountComputation("myComputation"),
                               Arrays.asList("i1:count", "o1:output"))
                       .build();
    }

    protected static class CountComputation extends AbstractComputation {

        protected int count = 0;

        public CountComputation(String name) {
            super(name, 1, 1);
        }

        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            count += BulkRecords.docIdsFrom(record).size();
            context.produceRecord("o1", record.getKey(), BigInteger.valueOf(count).toByteArray());
            context.askForCheckpoint();
        }
    }
}
