/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.stream.tests;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.Computation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.ComputationMetadataMapping;
import org.nuxeo.lib.stream.computation.ComputationPolicy;
import org.nuxeo.lib.stream.computation.ComputationPolicyBuilder;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.internals.ComputationContextImpl;
import org.nuxeo.lib.stream.computation.log.ComputationRunner;
import org.nuxeo.lib.stream.computation.log.LogStreamManager;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.runtime.management.api.ProbeStatus;
import org.nuxeo.runtime.stream.StreamProbe;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import net.jodah.failsafe.RetryPolicy;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.stream")
public class TestStreamProbe {

    protected static final String[] EXPECTED_MESSAGE_SEGMENTS = {
            "ComputationRunner 'testComputation' responsible for partitions [p-00, p-01] is blocked", //
            "after 3 retries\n", //
            "ComputationRunner 'testComputation' responsible for partitions [q-00, q-01, q-02] is blocked", //
            "after 2 retries\n" };

    public static class TestableComputationRunner extends ComputationRunner {
        public TestableComputationRunner(Supplier<Computation> supplier, ComputationMetadataMapping metadata,
                List<LogPartition> defaultAssignment, LogStreamManager streamManager, ComputationPolicy policy) {
            super(supplier, metadata, defaultAssignment, streamManager, policy);
        }

        @Override
        protected void processFallback(ComputationContextImpl context) {
            super.processFallback(context);
        }
    }

    protected ComputationContextImpl stubContext = new ComputationContextImpl(null) {
        @Override
        public void askForCheckpoint() {
            // do nothing
        }

        @Override
        public void askForTermination() {
            // do nothing
        }

        @Override
        public void cancelAskForCheckpoint() {
            // do nothing
        }
    };

    protected LogStreamManager stubManager = new LogStreamManager(null) {
        @Override
        public boolean supportSubscribe() {
            return false;
        }

        @Override
        public LogTailer<Record> createTailer(String computationName, Collection<LogPartition> streamPartitions) {
            return null;
        }
    };

    protected Computation comp = new AbstractComputation("testComputation", 1, 1) {
        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            // do nothing
        }
    };

    @Before
    public void before() {
        System.setProperty(StreamProbe.ACTIVATION_PROPERTY, "true");
    }

    @Test
    public void testStreamProbe() {

        Supplier<Computation> supplier = () -> comp;
        ComputationMetadataMapping metadata = new ComputationMetadataMapping(comp.metadata(), Collections.emptyMap());

        new TestableComputationRunner(supplier, metadata, partitions("p", 2), stubManager, policy(3)).processFallback(
                stubContext);

        new TestableComputationRunner(supplier, metadata, partitions("q", 3), stubManager, policy(2)).processFallback(
                stubContext);

        StreamProbe probe = new StreamProbe();
        ProbeStatus status = probe.run();
        String message = status.getAsString();

        for (String segment : EXPECTED_MESSAGE_SEGMENTS) {
            assertTrue(message.contains(segment));
        }
    }

    protected ComputationPolicy policy(int retries) {
        return new ComputationPolicyBuilder().retryPolicy(new RetryPolicy().withMaxRetries(retries)).build();
    }

    protected List<LogPartition> partitions(String name, int number) {
        List<LogPartition> partitions = new ArrayList<>(number);
        for (int i = 0; i < number; i++) {
            partitions.add(new LogPartition(name, i));
        }
        return partitions;
    }
}
