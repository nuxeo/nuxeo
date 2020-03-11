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
package org.nuxeo.runtime.stream.tests;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Example of a dummy stream processor.
 *
 * @since 9.3
 */
public class MyStreamProcessor implements StreamProcessorTopology {
    private static final Log log = LogFactory.getLog(MyStreamProcessor.class);

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(() -> new ComputationNoop("myComputation"), Arrays.asList("i1:input", "o1:s1"))
                       .addComputation(() -> new ComputationNoop("myComputation2"), Arrays.asList("i1:s1", "o1:output"))
                       .build();
    }

    // Simple computation that forward a record
    protected static class ComputationNoop extends AbstractComputation {
        public ComputationNoop(String name) {
            super(name, 1, 1);
        }

        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            log.debug(metadata.name() + " got record: " + record);
            context.produceRecord("o1", record);
            context.askForCheckpoint();
        }
    }
}
