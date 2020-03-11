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

import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Simplest processor that fails.
 *
 * @since 11.1
 */
public class FailureStreamProcessor implements StreamProcessorTopology {

    private static final Log log = LogFactory.getLog(FailureStreamProcessor.class);

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(() -> new FailureComputation("failureComp"),
                               Collections.singletonList("i1:inputFailure"))
                       .build();
    }

    protected static class FailureComputation extends AbstractComputation {
        public FailureComputation(String name) {
            super(name, 1, 0);
        }

        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            log.debug(metadata.name() + " got record: " + record);
            throw new RuntimeException("Simulated failure for testing purpose");
        }
    }

}
