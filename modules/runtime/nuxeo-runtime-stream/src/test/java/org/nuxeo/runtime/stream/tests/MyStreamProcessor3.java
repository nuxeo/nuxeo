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

import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;

/**
 * Example of a dummy stream processor.
 *
 * @since 11.1
 */
public class MyStreamProcessor3 extends MyStreamProcessor {

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(() -> new ForwardComputation("myComputation"),
                               Arrays.asList("i1:input3", "i2:registerInput", "o1:output3", "o2:externalOutput"))
                       .build();
    }

    protected static class ForwardComputation extends AbstractComputation {
        public ForwardComputation(String name) {
            super(name, 2, 2);
        }

        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            context.produceRecord(OUTPUT_1, record);
            context.askForCheckpoint();
        }
    }
}
